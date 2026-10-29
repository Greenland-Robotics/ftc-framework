package hive;

import java.util.function.Predicate;

/** The driver, played with the keyboard in the visualizer page. */
final class KeyboardDriver implements Driver {
    private final Predicate<String> held;

    /** {@code held} tells whether a key (a KeyboardEvent.code, like "ArrowUp") is down. */
    KeyboardDriver(Predicate<String> held) {
        this.held = held;
    }

    @Override
    public double forward() {
        return axis("ArrowUp", "ArrowDown") * scale();
    }

    @Override
    public double strafe() {
        return axis("KeyQ", "KeyE") * scale();
    }

    @Override
    public double turn() {
        return axis("ArrowLeft", "ArrowRight") * 0.6 * scale();
    }

    @Override
    public boolean intake() {
        return held.test("Space");
    }

    @Override
    public boolean shoot() {
        return held.test("KeyF");
    }

    @Override
    public boolean aim() {
        return held.test("KeyA");
    }

    /** Shift for slow, precise driving, like a gamepad's half-pushed stick. */
    private double scale() {
        return held.test("ShiftLeft") || held.test("ShiftRight") ? 0.35 : 1;
    }

    private double axis(String positive, String negative) {
        return (held.test(positive) ? 1 : 0) - (held.test(negative) ? 1 : 0);
    }
}
