package sdk.hardware;

/**
 * A motor, as the parts in {@code sdk.parts} see it. On the robot, {@code ftc.Hardware} makes one
 * from the hardware map; in tests and the simulator, anything that takes a power will do:
 * {@code Motor intake = power -> body.intakePower = power;}
 */
public interface Motor {
    /** -1..1; positive is the motor's forward direction. */
    void setPower(double power);

    /** Encoder position in ticks, or 0 for a motor without an encoder (like a CR servo). */
    default double position() {
        return 0;
    }

    /** Encoder speed in ticks per second, or 0 without an encoder. */
    default double velocity() {
        return 0;
    }

    /** Makes the current encoder position read 0. */
    default void resetPosition() {}
}
