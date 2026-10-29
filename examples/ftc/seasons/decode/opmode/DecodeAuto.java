package robot.opmode;

import static behavior.Behaviors.race;
import static behavior.Behaviors.sequence;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import behavior.Node;
import robot.control.AutoBase;
import sdk.auto.AutoMenu;
import sdk.vision.Randomization;

/**
 * Launches the three preloads, collects the row of artifacts matching the motif, launches those,
 * and parks. Poses are written for blue and flipped for red by {@code pose(...)}.
 */
@Autonomous(name = "DECODE Auto")
public class DecodeAuto extends AutoBase {
    enum Start { GOAL, FAR }

    private AutoMenu.Choice<Start> start;

    @Override
    protected void initialize() {
        start = menu().choice("Start", Start.values());
    }

    @Override
    protected void initLoop() {
        telemetry.addData("Motif", robot.camera.result());
    }

    @Override
    protected Node routine() {
        Randomization motif = robot.camera.result().orElse(Randomization.CENTER);
        Pose startPose = start.get() == Start.GOAL ? pose(20, 122, 144) : pose(56, 8, 90);
        Pose launch = pose(60, 84, 135);
        // The three spike-mark rows, nearest the goal first; the motif picks which one to take
        double rowY = motif == Randomization.LEFT ? 84 : motif == Randomization.CENTER ? 60 : 36;

        robot.drive.setStartingPose(startPose);
        return sequence(
                robot.drive.driveTo(launch),
                robot.launcher.shootThree(),
                robot.drive.driveTo(pose(48, rowY, 180)),
                race(                                               // drive through the row with the intake on
                        robot.drive.driveTo(pose(20, rowY, 180)),   // arriving ends the race, stopping the intake
                        robot.intake.run()
                ),
                robot.drive.driveTo(launch),
                robot.launcher.shootThree(),
                robot.drive.driveTo(pose(36, 60, 90))               // park off the launch line
        );
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Pose", robot.drive.pose());
    }
}
