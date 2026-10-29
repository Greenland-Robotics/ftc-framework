package pedro;

import java.util.Objects;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathBuilder;
import com.pedropathing.paths.PathChain;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import subsystem.Subsystem;

/**
 * The drivetrain, driven by Pedro Pathing. Owns the {@link Follower}: nothing else should call
 * {@code follower.update()} or command the drive motors.
 */
public class Drive implements Subsystem {
    private final Follower follower;

    public Drive(Follower follower) {
        this.follower = Objects.requireNonNull(follower, "follower");
    }

    @Override
    public void periodic() {
        follower.update();
    }

    /** Follows a prebuilt path, e.g. one from your Auto's {@code Paths} class. */
    public Node follow(PathChain path) {
        Objects.requireNonNull(path, "path");
        return new FollowPath(follower, () -> path);
    }

    /** Follows a prebuilt path at no more than {@code maxPower} (0..1), e.g. to go gently near a wall. */
    public Node follow(PathChain path, double maxPower) {
        Objects.requireNonNull(path, "path");
        return new FollowPath(follower, () -> path, maxPower);
    }

    /** Drives in a straight line from wherever the robot is when this starts to {@code target}. */
    public Node driveTo(Pose target) {
        Objects.requireNonNull(target, "target");
        return new FollowPath(follower, () -> {
            Pose start = follower.getPose();
            return follower.pathBuilder()
                    .addPath(new BezierLine(start, target))
                    .setLinearHeadingInterpolation(start.getHeading(), target.getHeading())
                    .build();
        });
    }

    /** Turns in place to face {@code degrees} (0 = Pedro's +x), and succeeds once it does. */
    public Node turnTo(double degrees) {
        return new UntilArrived(() -> follower.turnTo(Math.toRadians(degrees)));
    }

    /**
     * Holds the robot where it is when this starts, pushing back if another robot shoves it,
     * until halted. E.g. while a mechanism scores: {@code race(robot.drive.holdPosition(), score())}.
     */
    public Node holdPosition() {
        return new Hold();
    }

    /**
     * Driver control (field-centric). Ignored while a path is being followed, so a path started
     * from a button can finish; the driver gets control back when it ends or is halted.
     */
    public void drive(double forward, double strafe, double turn) {
        drive(forward, strafe, turn, false);
    }

    /** Driver control; {@code robotCentric} makes forward the robot's own forward instead of the field's. */
    public void drive(double forward, double strafe, double turn, boolean robotCentric) {
        if (follower.isBusy()) {
            return;
        }
        if (!follower.isTeleopDrive()) {
            follower.startTeleopDrive();
        }
        follower.setTeleOpDrive(forward, strafe, turn, robotCentric);
    }

    /** Makes the way the robot faces now heading 0, so field-centric "forward" is straight ahead of it. */
    public void resetHeading() {
        follower.setPose(follower.getPose().withHeading(0));
    }

    /** Where the robot is on the field. */
    public Pose pose() {
        return follower.getPose();
    }

    /** Tells the localizer where the robot starts. Call during init. */
    public void setStartingPose(Pose pose) {
        follower.setStartingPose(pose);
    }

    /** Starts building a path, e.g. in your Auto's {@code Paths} class. */
    public PathBuilder pathBuilder() {
        return follower.pathBuilder();
    }

    /** Starts a Pedro motion and succeeds when the follower is done with it; stops it if halted. */
    private final class UntilArrived extends Action {
        private final Runnable begin;

        UntilArrived(Runnable begin) {
            this.begin = begin;
        }

        @Override
        protected void start() {
            begin.run();
        }

        @Override
        protected Status update() {
            return follower.isBusy() ? Status.RUNNING : Status.SUCCESS;
        }

        @Override
        protected void end(boolean interrupted) {
            if (interrupted) {
                follower.breakFollowing();
            }
        }
    }

    private final class Hold extends Action {
        @Override
        protected void start() {
            follower.holdPoint(follower.getPose());
        }

        @Override
        protected Status update() {
            return Status.RUNNING;
        }

        @Override
        protected void end(boolean interrupted) {
            follower.breakFollowing();
        }
    }
}
