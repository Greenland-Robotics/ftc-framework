package robot.opmode;

import static behavior.Behaviors.sequence;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import behavior.Node;
import robot.control.AutoBase;
import robot.subsystems.PixelScorer;
import sdk.vision.Randomization;

/**
 * From the backstage start: the camera finds the team prop, the purple pixel goes on that spike
 * mark, the yellow one on the backdrop in the matching column, then park in the corner.
 */
@Autonomous(name = "CENTERSTAGE Auto")
public class CenterstageAuto extends AutoBase {
    @Override
    protected void initLoop() {
        robot.alliance = alliance();   // The camera looks for this alliance's prop color
        telemetry.addData("Prop", robot.camera.result());
    }

    @Override
    protected Node routine() {
        Randomization prop = robot.camera.result().orElse(Randomization.CENTER);
        // Blue poses; pose(...) flips them for red
        Pose spike = prop == Randomization.LEFT ? pose(30, 36, 0)
                : prop == Randomization.CENTER ? pose(24, 46, 270)
                : pose(30, 60, 180);
        double column = prop == Randomization.LEFT ? 29 : prop == Randomization.CENTER ? 35 : 41;

        robot.drive.setStartingPose(pose(12, 63, 0));
        return sequence(
                robot.drive.driveTo(spike),
                robot.scorer.dropLeft(),                          // Purple pixel on the spike mark
                robot.drive.driveTo(pose(50, column, 180)),
                robot.scorer.lift.goTo(PixelScorer.LOW),
                robot.scorer.dropRight(),                         // Yellow pixel on the backdrop
                robot.scorer.lift.goTo(PixelScorer.DOWN),
                robot.drive.driveTo(pose(58, 10, 180))
        );
    }
}
