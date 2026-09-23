package subsystem;

/**
 * One part of the robot (drivetrain, intake, arm...) that owns its hardware. Other code never
 * touches that hardware directly: it asks the subsystem, usually for a behavior tree Node such
 * as {@code intake.collect()}.
 */
public interface Subsystem {
    /** Called once per loop, before behaviors tick: read sensors, update controllers. */
    default void periodic() {}
}
