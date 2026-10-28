package hive;

import behavior.runtime.Clock;

/** One robot's subsystems, like robot/Robot.java on the real robot. */
public final class Robot {
    public final Alliance alliance;
    public final Drive drive;
    public final Intake intake;
    public final Shooter shooter;

    final Body body;

    Robot(Body body, Clock clock) {
        this.body = body;
        this.alliance = body.alliance;
        this.drive = new Drive(body);
        this.intake = new Intake(body);
        this.shooter = new Shooter(body, clock);
    }
}
