package sdk.auto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import behavior.input.EdgeDetector;
import sdk.teleop.Controller;

/**
 * Choices made with gamepad 1 during init, before the Auto starts: alliance, start position,
 * a start delay... AutoBase shows it on the Driver Station and locks it at start.
 * <pre>
 *   Dpad up/down: choose a line.  Dpad left/right: change it.
 *   &gt; Alliance          ◀ BLUE ▶
 *     Start             ◀ NEAR ▶
 *     Start delay (s)   ◀ 0 ▶
 * </pre>
 * Add your own lines in your Auto's {@code initialize()}, and read them in {@code routine()}:
 * <pre>
 * enum Start { NEAR, FAR }
 * private AutoMenu.Choice&lt;Start&gt; start;
 *
 * protected void initialize() { start = menu().choice("Start", Start.values()); }
 * protected Node routine()    { return start.get() == Start.NEAR ? nearAuto() : farAuto(); }
 * </pre>
 */
public final class AutoMenu {
    private final List<Line> lines = new ArrayList<>();
    private final EdgeDetector up;
    private final EdgeDetector down;
    private final EdgeDetector left;
    private final EdgeDetector right;
    private int selected;
    private boolean locked;

    public AutoMenu(Controller gamepad) {
        this(gamepad.dpadUp(), gamepad.dpadDown(), gamepad.dpadLeft(), gamepad.dpadRight());
    }

    public AutoMenu(BooleanSupplier up, BooleanSupplier down, BooleanSupplier left, BooleanSupplier right) {
        this.up = new EdgeDetector(up);
        this.down = new EdgeDetector(down);
        this.left = new EdgeDetector(left);
        this.right = new EdgeDetector(right);
    }

    /** A line that picks one of {@code options}, starting at the first. */
    @SafeVarargs
    public final <T> Choice<T> choice(String label, T... options) {
        if (options.length == 0) throw new IllegalArgumentException(label + ": needs at least one option");
        Choice<T> choice = new Choice<>(label, options);
        lines.add(choice);
        return choice;
    }

    /** A line that picks a number from {@code min} to {@code max} in steps of {@code step}, starting at {@code min}. */
    public Number number(String label, double min, double max, double step) {
        if (step <= 0 || max < min) throw new IllegalArgumentException(label + ": needs min <= max and step > 0");
        Number number = new Number(label, min, max, step);
        lines.add(number);
        return number;
    }

    /** Reads the dpad; call once per loop during init. */
    public void update() {
        boolean upPressed = up.update() == EdgeDetector.Edge.PRESSED;
        boolean downPressed = down.update() == EdgeDetector.Edge.PRESSED;
        boolean leftPressed = left.update() == EdgeDetector.Edge.PRESSED;
        boolean rightPressed = right.update() == EdgeDetector.Edge.PRESSED;
        if (locked || lines.isEmpty()) return;

        if (upPressed) selected = (selected + lines.size() - 1) % lines.size();
        if (downPressed) selected = (selected + 1) % lines.size();
        if (leftPressed) lines.get(selected).change(-1);
        if (rightPressed) lines.get(selected).change(1);
    }

    /** Stops changes; AutoBase calls this at start. */
    public void lock() {
        locked = true;
    }

    public boolean isLocked() {
        return locked;
    }

    /** What to show on the Driver Station, one line per entry. */
    public List<String> show() {
        List<String> text = new ArrayList<>();
        text.add(locked ? "Auto choices (locked)" : "Dpad ▲▼ choose, ◀▶ change");
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            String marker = !locked && i == selected ? "> " : "  ";
            text.add(marker + line.label + ": " + (locked ? line.text() : "◀ " + line.text() + " ▶"));
        }
        return text;
    }

    private abstract static class Line {
        final String label;

        Line(String label) {
            this.label = Objects.requireNonNull(label, "label");
        }

        abstract void change(int direction);

        abstract String text();
    }

    /** One line of the menu that picks from a list. */
    public static final class Choice<T> extends Line {
        private final T[] options;
        private int index;

        Choice(String label, T[] options) {
            super(label);
            this.options = options;
        }

        public T get() {
            return options[index];
        }

        /** Starts at {@code option} instead of the first one. */
        public Choice<T> startAt(T option) {
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(option)) index = i;
            }
            return this;
        }

        @Override
        void change(int direction) {
            index = (index + direction + options.length) % options.length;
        }

        @Override
        String text() {
            return String.valueOf(get());
        }
    }

    /** One line of the menu that picks a number. */
    public static final class Number extends Line {
        private final double min;
        private final double max;
        private final double step;
        private double value;

        Number(String label, double min, double max, double step) {
            super(label);
            this.min = min;
            this.max = max;
            this.step = step;
            this.value = min;
        }

        public double get() {
            return value;
        }

        @Override
        void change(int direction) {
            value = Math.max(min, Math.min(max, value + direction * step));
        }

        @Override
        String text() {
            return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.2f", value);
        }
    }
}
