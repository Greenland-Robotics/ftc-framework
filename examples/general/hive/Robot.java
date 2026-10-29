package hive;

import behavior.runtime.Clock;

/** One robot's subsystems, like robot/Robot.java on the real robot. */
public final class Robot {
    public final Alliance alliance;
    public final Drive drive;
    public final Intake intake;
    public final Shooter shooter;

    final Body body;

    Robot(Body body, Clock clock, Drive.Router router) {
        this.body = body;
        this.alliance = body.alliance;
        this.drive = new Drive(body, router);
        this.intake = new Intake(body, clock);
        this.shooter = new Shooter(body, clock);
    }

    /** Every loop before the tree ticks, like robot.periodic() on the robot: the parts read and control. */
    public void periodic() {
        drive.periodic();
        intake.periodic();
        shooter.periodic();
    }
}
