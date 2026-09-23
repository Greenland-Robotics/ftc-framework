```java
package gcsrobotics.opmode;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import gcsrobotics.commands.FollowPath;
import gcsrobotics.commands.Shoot;
import gcsrobotics.commands.StartIntake;
import gcsrobotics.control.AutoBase;
import gcsrobotics.vertices.Command;
import gcsrobotics.vertices.CommandRunner;
import gcsrobotics.vertices.InstantCommand;
import gcsrobotics.vertices.ParallelCommand;
import gcsrobotics.vertices.SeriesCommand;


@Autonomous(name="Example Auto")
public class ExampleAuto extends AutoBase {
    private Command shootPreload, intakeLine1, shootIntake;
    private Paths paths;
    protected void buildCommands() {
        shootPreload = new SeriesCommand(
                new FollowPath(paths.shootPreloadPath),
                new Shoot()
        );

        intakeLine1 = new ParallelCommand(
                new FollowPath(paths.intakePath),
                new StartIntake()
        );

        shootIntake = new ParallelCommand(
                new InstantCommand(() -> intake.setVelocity(0)),
                new SeriesCommand(
                        new FollowPath(paths.shootIntakePath),
                        new Shoot()
                )
        );

    }

    @Override
    protected void initialize() {
        follower.setStartingPose(new Pose(26, 128, Math.toRadians(-38)));
        paths = new Paths(follower);
        commandRunner = new CommandRunner(new SeriesCommand(
                shootPreload,
                intakeLine1,
                shootIntake
        ));
    }

    @Override
    protected void runLoop() {
        telemetry.addData("x pos", getX());
        telemetry.addData("y pos", getY());
        telemetry.addData("heading", getHeading());
    }

    public static class Paths {
        public PathChain shootPreloadPath;
        public PathChain intakePath;
        public PathChain shootIntakePath;

        public Paths(Follower follower) {
            shootPreloadPath = follower.pathBuilder().addPath(
                            new BezierLine(
                                    new Pose(25.823, 128.911),

                                    new Pose(66.329, 96.759)
                            )
                    ).setTangentHeadingInterpolation()

                    .build();

            intakePath = follower.pathBuilder().addPath(
                            new BezierCurve(
                                    new Pose(66.329, 96.759),
                                    new Pose(45.797, 83.259),
                                    new Pose(18.886, 83.734)
                            )
                    ).setTangentHeadingInterpolation()

                    .build();

            shootIntakePath = follower.pathBuilder().addPath(
                            new BezierLine(
                                    new Pose(18.886, 83.734),

                                    new Pose(66.443, 96.873)
                            )
                    ).setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(-36))

                    .build();
        }
    }


}
```