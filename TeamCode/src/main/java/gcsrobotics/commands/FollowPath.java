package gcsrobotics.commands;

import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;

import gcsrobotics.control.OpModeBase;
import gcsrobotics.vertices.Command;

public class FollowPath implements Command {
    private final OpModeBase r = OpModeBase.INSTANCE;
    private final PathChain path;

    public FollowPath(PathChain path) {
        this.path = path;
    }
    public FollowPath(Path path) {
        this.path = r.follower.pathBuilder().addPath(path).build();
    }
    public FollowPath(Pose... poses) {
        if (poses.length >= 3) {
            this.path = r.follower.pathBuilder().addPath(new Path(new BezierCurve(poses))).build();
        } else if (poses.length == 2) {
            this.path = r.follower.pathBuilder().addPath(new Path(new BezierLine(poses[0], poses[1]))).build();
        } else {
            throw new IllegalArgumentException("Path must have at least 2 points");
        }
    }


    @Override
    public void init() {
        r.follower.followPath(path);
    }

    @Override
    public void loop() {
        r.follower.update();
    }

    @Override
    public boolean isFinished() {
        return !r.follower.isBusy();
    }
}
