package sdk.teleop;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * A gamepad, with every button and stick ready to hand to a binding or a behavior:
 * <pre>
 * controls.onPress(operator.a(), robot.claw.toggle(OPEN, CLOSED));
 * controls.whileHeld(operator.rightTrigger(), robot.intake.run());
 * robot.lift.manual(operator.rightStickY())
 * </pre>
 * Sticks are the way you'd expect, which raw FTC gamepads are not: pushing <b>up is positive</b>
 * and right is positive, and a small {@link #deadband} around the middle reads as 0 so a
 * worn stick doesn't creep.
 *
 * <p>On the robot, {@code ftc.GamepadController} reads a real gamepad. Anything else (the hive
 * simulator's keyboard, a test) extends this class and answers {@link #isDown} and {@link #raw}.
 * PlayStation names work too: {@code cross()} is {@code a()}, {@code circle()} is {@code b()}...
 */
public abstract class Controller {
    public enum Button {
        A, B, X, Y,
        DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
        LEFT_BUMPER, RIGHT_BUMPER,
        BACK, START,
        LEFT_STICK_BUTTON, RIGHT_STICK_BUTTON
    }

    /** Sticks are -1..1 with up and right positive; triggers are 0..1. */
    public enum Axis { LEFT_X, LEFT_Y, RIGHT_X, RIGHT_Y, LEFT_TRIGGER, RIGHT_TRIGGER }

    private double deadband = 0.05;

    /** Whether {@code button} is down right now. */
    protected abstract boolean isDown(Button button);

    /** {@code axis} right now: sticks with up and right positive. */
    protected abstract double raw(Axis axis);

    /** Buzzes the gamepad for {@code millis}, e.g. at the end game. Does nothing if it can't. */
    public void rumble(int millis) {}

    /** Stick values smaller than this read as 0. */
    public Controller deadband(double deadband) {
        this.deadband = Math.abs(deadband);
        return this;
    }

    // ---- Buttons ----

    public BooleanSupplier a() { return button(Button.A); }
    public BooleanSupplier b() { return button(Button.B); }
    public BooleanSupplier x() { return button(Button.X); }
    public BooleanSupplier y() { return button(Button.Y); }
    public BooleanSupplier dpadUp() { return button(Button.DPAD_UP); }
    public BooleanSupplier dpadDown() { return button(Button.DPAD_DOWN); }
    public BooleanSupplier dpadLeft() { return button(Button.DPAD_LEFT); }
    public BooleanSupplier dpadRight() { return button(Button.DPAD_RIGHT); }
    public BooleanSupplier leftBumper() { return button(Button.LEFT_BUMPER); }
    public BooleanSupplier rightBumper() { return button(Button.RIGHT_BUMPER); }
    public BooleanSupplier back() { return button(Button.BACK); }
    public BooleanSupplier start() { return button(Button.START); }
    public BooleanSupplier leftStickButton() { return button(Button.LEFT_STICK_BUTTON); }
    public BooleanSupplier rightStickButton() { return button(Button.RIGHT_STICK_BUTTON); }

    // PlayStation names for the same buttons
    public BooleanSupplier cross() { return a(); }
    public BooleanSupplier circle() { return b(); }
    public BooleanSupplier square() { return x(); }
    public BooleanSupplier triangle() { return y(); }
    public BooleanSupplier share() { return back(); }
    public BooleanSupplier options() { return start(); }

    /** Triggers as buttons: "down" once pressed more than halfway. */
    public BooleanSupplier leftTrigger() { return () -> raw(Axis.LEFT_TRIGGER) > 0.5; }
    public BooleanSupplier rightTrigger() { return () -> raw(Axis.RIGHT_TRIGGER) > 0.5; }

    public BooleanSupplier button(Button button) {
        return () -> isDown(button);
    }

    // ---- Sticks and triggers ----

    public DoubleSupplier leftStickX() { return axis(Axis.LEFT_X); }
    public DoubleSupplier leftStickY() { return axis(Axis.LEFT_Y); }
    public DoubleSupplier rightStickX() { return axis(Axis.RIGHT_X); }
    public DoubleSupplier rightStickY() { return axis(Axis.RIGHT_Y); }
    /** How far the trigger is pressed, 0..1. */
    public DoubleSupplier leftTriggerAmount() { return axis(Axis.LEFT_TRIGGER); }
    public DoubleSupplier rightTriggerAmount() { return axis(Axis.RIGHT_TRIGGER); }

    public DoubleSupplier axis(Axis axis) {
        return () -> {
            double value = Math.max(-1, Math.min(1, raw(axis)));
            return Math.abs(value) < deadband ? 0 : value;
        };
    }
}
