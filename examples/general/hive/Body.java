package hive;

import static util.MathUtil.toFieldFrame;
import static util.MathUtil.toRobotFrame;

import java.util.ArrayList;
import java.util.List;

/**
 * The simulated hardware of one robot: where it is, what its motors are told and what its
 * sensors would see. Subsystems set the motor fields; {@link Field#step()} moves everything.
 * On the real robot, these are the motors and sensors in the hardware map.
 */
final class Body {
    static final double SIZE = 18;
    static final int CAPACITY = 3;

    final Alliance alliance;
    final double speedScale;   // < 1 makes a robot slower than the motors say (the AI opponent)

    double x;
    double y;
    double heading;            // radians, 0 = facing right, counter-clockwise is positive
    double vx;
    double vy;
    double omega;

    // Motor commands, -1..1. Strafe positive = to the robot's left, turn positive = counter-clockwise.
    double forward;
    double strafe;
    double turn;
    double intakePower;
    double flywheelPower;      // 0..1
    double flywheelSpeed;      // What the flywheel is really doing, 0..1: it takes time to spin up
    boolean launch;            // The feeder pushed a ball into the flywheel this loop

    final List<Ball> held = new ArrayList<>();

    Body(Alliance alliance, double x, double y, double heading, double speedScale) {
        this.alliance = alliance;
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.speedScale = speedScale;
    }

    Pose pose() {
        return new Pose(x, y, heading);
    }

    /** Field position of a point given in the robot's frame (forward, left). */
    double[] toField(double forwardIn, double leftIn) {
        double[] offset = toFieldFrame(forwardIn, leftIn, heading);
        return new double[] {x + offset[0], y + offset[1]};
    }

    /** Where a field point is as seen from the robot: {forward, left}, in inches. */
    double[] toRobot(double fieldX, double fieldY) {
        return toRobotFrame(fieldX - x, fieldY - y, heading);
    }
}
