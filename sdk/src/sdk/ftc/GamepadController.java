package sdk.ftc;

import java.util.Objects;

import com.qualcomm.robotcore.hardware.Gamepad;

import sdk.teleop.Controller;

/** A real gamepad as an sdk {@link Controller}. OpModeBase makes {@code driver} and {@code operator} from these. */
public final class GamepadController extends Controller {
    private final Gamepad gamepad;

    public GamepadController(Gamepad gamepad) {
        this.gamepad = Objects.requireNonNull(gamepad, "gamepad");
    }

    @Override
    protected boolean isDown(Button button) {
        switch (button) {
            case A: return gamepad.a;
            case B: return gamepad.b;
            case X: return gamepad.x;
            case Y: return gamepad.y;
            case DPAD_UP: return gamepad.dpad_up;
            case DPAD_DOWN: return gamepad.dpad_down;
            case DPAD_LEFT: return gamepad.dpad_left;
            case DPAD_RIGHT: return gamepad.dpad_right;
            case LEFT_BUMPER: return gamepad.left_bumper;
            case RIGHT_BUMPER: return gamepad.right_bumper;
            case BACK: return gamepad.back;
            case START: return gamepad.start;
            case LEFT_STICK_BUTTON: return gamepad.left_stick_button;
            case RIGHT_STICK_BUTTON: return gamepad.right_stick_button;
            default: return false;
        }
    }

    @Override
    protected double raw(Axis axis) {
        switch (axis) {
            case LEFT_X: return gamepad.left_stick_x;
            case LEFT_Y: return -gamepad.left_stick_y;     // FTC sticks read negative when pushed up
            case RIGHT_X: return gamepad.right_stick_x;
            case RIGHT_Y: return -gamepad.right_stick_y;
            case LEFT_TRIGGER: return gamepad.left_trigger;
            case RIGHT_TRIGGER: return gamepad.right_trigger;
            default: return 0;
        }
    }

    @Override
    public void rumble(int millis) {
        gamepad.rumble(millis);
    }
}
