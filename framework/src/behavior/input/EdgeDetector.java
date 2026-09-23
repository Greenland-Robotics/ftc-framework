package behavior.input;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Turns a button that reads true while held into single "pressed" and "released" events. */
public final class EdgeDetector {
    public enum Edge { NONE, PRESSED, RELEASED }

    private final BooleanSupplier input;
    private boolean wasDown;

    public EdgeDetector(BooleanSupplier input) {
        this.input = Objects.requireNonNull(input, "input");
    }

    /** Reads the input; call once per loop. */
    public Edge update() {
        boolean down = input.getAsBoolean();
        Edge edge = down == wasDown ? Edge.NONE : down ? Edge.PRESSED : Edge.RELEASED;
        wasDown = down;
        return edge;
    }
}
