package hive;

import java.util.function.Supplier;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import subsystem.Subsystem;

/** The mecanum drivetrain: drives with the driver's sticks, or by itself to a pose. */
public final class Drive implements Subsystem {
    private static final double POSITION_TOLERANCE = 1.5;            // inches
    private static final double HEADING_TOLERANCE = Math.toRadians(3);

    private final Body body;

    Drive(Body body) {
        this.body = body;
    }

    public Pose pose() {
        return body.pose();
    }

    /** Drives with the driver's sticks until halted. */
    public Node driveWith(Driver driver) {
        return new DriveWithSticks(driver);
    }

    /**
     * Drives to a pose and succeeds when it is there. The target is read every loop, so it can
     * move (like a rolling ball); while it is null the robot waits in place.
     */
    public Node driveTo(Supplier<Pose> target) {
        return new DriveTo(target);
    }

    private void set(double forward, double strafe, double turn) {
        body.forward = clamp(forward);
        body.strafe = clamp(strafe);
        body.turn = clamp(turn);
    }

    private void stop() {
        set(0, 0, 0);
    }

    static double clamp(double value) {
        return Math.max(-1, Math.min(1, value));
    }

    /** -π..π */
    static double wrap(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    private final class DriveWithSticks extends Action {
        private final Driver driver;

        DriveWithSticks(Driver driver) {
            this.driver = driver;
        }

        @Override
        protected Status update() {
            set(driver.forward(), driver.strafe(), driver.turn());
            return Status.RUNNING;   // Until a parent halts it, e.g. when aim takes over
        }

        @Override
        protected void end(boolean interrupted) {
            stop();
        }
    }

    private final class DriveTo extends Action {
        private final Supplier<Pose> target;

        DriveTo(Supplier<Pose> target) {
            this.target = target;
        }

        @Override
        protected Status update() {
            Pose goal = target.get();
            if (goal == null) {
                stop();
                return Status.RUNNING;
            }
            double dx = goal.x - body.x;
            double dy = goal.y - body.y;
            double distance = Math.hypot(dx, dy);
            double headingError = wrap(goal.heading - body.heading);
            if (distance < POSITION_TOLERANCE && Math.abs(headingError) < HEADING_TOLERANCE) {
                stop();
                return Status.SUCCESS;
            }

            // Proportional control toward the goal, turned from field directions into robot ones
            double speed = distance < POSITION_TOLERANCE ? 0 : Math.max(0.12, Math.min(1, distance * 0.06));
            double fieldX = distance == 0 ? 0 : dx / distance * speed;
            double fieldY = distance == 0 ? 0 : dy / distance * speed;
            double c = Math.cos(body.heading);
            double s = Math.sin(body.heading);
            set(fieldX * c + fieldY * s, -fieldX * s + fieldY * c, headingError * 1.6);
            return Status.RUNNING;
        }

        @Override
        protected void end(boolean interrupted) {
            stop();
        }
    }
}
