package robot.opmode;

import static behavior.Behaviors.parallel;
import static behavior.Behaviors.selector;
import static behavior.Behaviors.sequence;

import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import behavior.Node;
import pedro.Drive;
import robot.control.AutoBase;

/**
 * Scores a preloaded sample, tries to grab and score another one, and parks if that fails.
 * Needs the example Intake subsystem registered in Robot.java as {@code intake}.
 */
@Autonomous(name = "Example Auto")
public class ExampleAuto extends AutoBase {
    private Paths paths;

    @Override
    protected void initialize() {
        robot.drive.setStartingPose(Paths.START);
        paths = new Paths(robot.drive);
    }

    @Override
    protected Node routine() {
        return sequence(
                robot.drive.follow(paths.toGoal),
                robot.intake.eject(),                       // score the preload

                selector(                                   // the first branch that succeeds wins
                        sequence(                           // 1. grab another sample and score it
                                parallel(
                                        robot.drive.follow(paths.toStack),
                                        robot.intake.collect()
                                ).withTimeout(4000),        // no sample in 4 s: this branch fails
                                robot.drive.follow(paths.backToGoal),
                                robot.intake.eject()
                        ),
                        robot.drive.driveTo(Paths.PARK)     // 2. otherwise park, from wherever we are
                )
        );
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Pose", robot.drive.pose());
    }

    /** Paths for this Auto. Generate curves with https://visualizer.pedropathing.com */
    static class Paths {
        static final Pose START = new Pose(26, 128, Math.toRadians(-38));
        static final Pose GOAL = new Pose(66, 97, Math.toRadians(-38));
        static final Pose STACK = new Pose(19, 84, Math.toRadians(180));
        static final Pose PARK = new Pose(60, 60, Math.toRadians(90));

        final PathChain toGoal;
        final PathChain toStack;
        final PathChain backToGoal;

        Paths(Drive drive) {
            toGoal = line(drive, START, GOAL);
            toStack = drive.pathBuilder()
                    .addPath(new BezierCurve(GOAL, new Pose(46, 83), STACK))
                    .setTangentHeadingInterpolation()
                    .build();
            backToGoal = line(drive, STACK, GOAL);
        }

        private static PathChain line(Drive drive, Pose from, Pose to) {
            return drive.pathBuilder()
                    .addPath(new BezierLine(from, to))
                    .setLinearHeadingInterpolation(from.getHeading(), to.getHeading())
                    .build();
        }
    }
}
