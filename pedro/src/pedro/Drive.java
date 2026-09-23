package pedro;

import java.util.Objects;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathBuilder;
import com.pedropathing.paths.PathChain;

import behavior.Node;
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

    /**
     * Driver control (field-centric). Ignored while a path is being followed, so a path started
     * from a button can finish; the driver gets control back when it ends or is halted.
     */
    public void drive(double forward, double strafe, double turn) {
        if (follower.isBusy()) {
            return;
        }
        if (!follower.isTeleopDrive()) {
            follower.startTeleopDrive();
        }
        follower.setTeleOpDrive(forward, strafe, turn, false);
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
}
