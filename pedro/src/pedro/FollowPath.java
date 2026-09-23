package pedro;

import java.util.Objects;
import java.util.function.Supplier;

import com.pedropathing.follower.Follower;
import com.pedropathing.paths.PathChain;

import behavior.Action;
import behavior.Status;

/**
 * Drives along a path and succeeds when the robot arrives. If halted part-way (a timeout, a
 * released button), the robot stops following. Create these through {@link Drive}.
 *
 * <p>The path comes from a Supplier that is asked each time the action starts, so paths that
 * depend on where the robot is (like {@link Drive#driveTo}) are built from its current pose.
 */
public final class FollowPath extends Action {
    private final Follower follower;
    private final Supplier<PathChain> path;

    public FollowPath(Follower follower, Supplier<PathChain> path) {
        this.follower = Objects.requireNonNull(follower, "follower");
        this.path = Objects.requireNonNull(path, "path");
    }

    @Override
    protected void start() {
        follower.followPath(path.get());
    }

    @Override
    protected Status update() {
        // Drive.periodic() updates the follower every loop; this only checks progress
        return follower.isBusy() ? Status.RUNNING : Status.SUCCESS;
    }

    @Override
    protected void end(boolean interrupted) {
        if (interrupted) {
            follower.breakFollowing();
        }
    }
}
