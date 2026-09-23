package pedro;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;

import commands.Command;

public class FollowPath implements Command {
    private final Follower follower = Pedro.follower();
    private final PathChain path;

    public FollowPath(PathChain path) {
        this.path = path;
    }
    public FollowPath(Path path) {
        this.path = follower.pathBuilder().addPath(path).build();
    }
    public FollowPath(Pose... poses) {
        if (poses.length >= 3) {
            this.path = follower.pathBuilder().addPath(new Path(new BezierCurve(poses))).build();
        } else if (poses.length == 2) {
            this.path = follower.pathBuilder().addPath(new Path(new BezierLine(poses[0], poses[1]))).build();
        } else {
            throw new IllegalArgumentException("Path must have at least 2 points");
        }
    }


    @Override
    public void init() {
        follower.followPath(path);
    }

    @Override
    public void loop() {
        follower.update();
    }

    @Override
    public boolean isFinished() {
        return !follower.isBusy();
    }
}
