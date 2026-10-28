package hive;

/**
 * What the driver is asking for. In the simulator the keyboard answers; on the robot a gamepad
 * will (e.g. {@code forward()} is {@code -gamepad1.left_stick_y}), and the TeleOp tree stays the same.
 */
public interface Driver {
    /** -1..1, positive drives forward. */
    double forward();

    /** -1..1, positive slides to the robot's left. */
    double strafe();

    /** -1..1, positive turns counter-clockwise (left). */
    double turn();

    /** Held to run the intake. */
    boolean intake();

    /** Held to shoot: one ball per press, or one after another while held. */
    boolean shoot();

    /** Held to let the robot line itself up for a shot (the same behavior the Auto uses). */
    boolean aim();
}
