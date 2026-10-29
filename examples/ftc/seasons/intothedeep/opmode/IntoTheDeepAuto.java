package robot.opmode;

import static behavior.Behaviors.parallel;
import static behavior.Behaviors.sequence;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import behavior.Node;
import robot.control.AutoBase;
import robot.subsystems.Scorer;
import sdk.auto.AutoMenu;

/**
 * Basket side: score the preloaded sample, grab samples from the spike marks and score them.
 * Chamber side: hang the preloaded specimen, then park in the observation zone.
 */
@Autonomous(name = "INTO THE DEEP Auto")
public class IntoTheDeepAuto extends AutoBase {
    enum Side { BASKET, CHAMBER }

    private AutoMenu.Choice<Side> side;

    @Override
    protected void initialize() {
        side = menu().choice("Side", Side.values());
    }

    @Override
    protected void initLoop() {
        robot.alliance = alliance();   // So the intake knows which color is ours
    }

    @Override
    protected Node routine() {
        robot.alliance = alliance();
        return side.get() == Side.BASKET ? basket() : chamber();
    }

    private Node basket() {
        robot.drive.setStartingPose(pose(40, 136, 180));
        return sequence(
                robot.drive.driveTo(pose(18, 126, 135)),
                robot.scorer.scoreBasket(),
                robot.drive.driveTo(pose(30, 120, 180)),
                robot.intake.grab().retry(2).alwaysSucceed(),   // No luck? Still go park
                robot.drive.driveTo(pose(18, 126, 135)),
                robot.scorer.scoreBasket(),
                robot.drive.driveTo(pose(60, 96, 270))           // Touch the low bar to park
        );
    }

    private Node chamber() {
        robot.drive.setStartingPose(pose(88, 136, 270));
        return sequence(
                // Raise the lift on the way, so it's ready when the robot arrives
                parallel(robot.drive.driveTo(pose(72, 112, 270)), robot.scorer.lift.goTo(Scorer.HIGH_CHAMBER)),
                robot.scorer.hangSpecimen(),
                parallel(robot.drive.driveTo(pose(120, 130, 270)), robot.scorer.lift.goTo(Scorer.DOWN))
        );
    }
}
