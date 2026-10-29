package hive;

import java.util.function.Predicate;

import sdk.teleop.Controller;

/**
 * The keyboard in the visualizer page, pretending to be a gamepad. The TeleOp only sees an sdk
 * {@link Controller}, so on the robot the same {@link GamepadDriver} reads a real gamepad
 * ({@code sdk.ftc.GamepadController}) instead.
 *
 * <pre>
 *   Arrow up/down  = left stick up/down     Q/E   = left stick left/right
 *   Arrow left/right = right stick          Shift = left bumper
 *   Space = right bumper     F = right trigger     A = A
 * </pre>
 */
final class KeyboardController extends Controller {
    private final Predicate<String> held;

    /** {@code held} tells whether a key (a KeyboardEvent.code, like "ArrowUp") is down. */
    KeyboardController(Predicate<String> held) {
        this.held = held;
    }

    @Override
    protected boolean isDown(Button button) {
        switch (button) {
            case A: return held.test("KeyA");
            case LEFT_BUMPER: return held.test("ShiftLeft") || held.test("ShiftRight");
            case RIGHT_BUMPER: return held.test("Space");
            default: return false;
        }
    }

    @Override
    protected double raw(Axis axis) {
        switch (axis) {
            case LEFT_Y: return keys("ArrowUp", "ArrowDown");
            case LEFT_X: return keys("KeyE", "KeyQ");
            case RIGHT_X: return keys("ArrowRight", "ArrowLeft");
            case RIGHT_TRIGGER: return held.test("KeyF") ? 1 : 0;
            default: return 0;
        }
    }

    private double keys(String positive, String negative) {
        return (held.test(positive) ? 1 : 0) - (held.test(negative) ? 1 : 0);
    }
}
