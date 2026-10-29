package subsystem;

/**
 * One part of the robot (drivetrain, intake, arm...) that owns its hardware. Other code never
 * touches that hardware directly: it asks the subsystem, usually for a behavior tree Node such
 * as {@code intake.collect()}.
 */
public interface Subsystem {
    /** Called once per loop after start, before behaviors tick: read sensors, update controllers. */
    default void periodic() {}

    /**
     * Called once per loop during init, before start. Read sensors (a camera, an encoder) here,
     * but never move a motor: the robot must stay still until start.
     */
    default void initPeriodic() {}
}
