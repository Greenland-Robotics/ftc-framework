package robot.opmode;

import static behavior.Behaviors.when;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import behavior.input.Bindings;
import robot.Robot;
import robot.control.TeleOpBase;
import robot.subsystems.PixelScorer;

/**
 * Operator: A grabs, the triggers drop the left/right pixel, Y and X score high and low. End game:
 * B launches the drone (ignored before the end game, so a slip can't throw it away), and the
 * right stick winds the hang winch while the left bumper is held.
 */
@TeleOp(name = "CENTERSTAGE TeleOp")
public class CenterstageTeleOp extends TeleOpBase {
    @Override
    protected void bindControls(Bindings controls) {
        controls.onPress(operator.a(), robot.scorer.grabBoth());
        controls.onPress(operator.leftTrigger(), robot.scorer.dropLeft());
        controls.onPress(operator.rightTrigger(), robot.scorer.dropRight());
        controls.onPress(operator.y(), robot.scorer.score(PixelScorer.HIGH));
        controls.onPress(operator.x(), robot.scorer.score(PixelScorer.LOW));

        controls.onPress(operator.b(), when(() -> timeLeft() <= endgameSeconds(), robot.drone.setTo(Robot.DRONE_LAUNCH)));
        controls.whileHeld(operator.leftBumper(), robot.hang.manual(operator.rightStickY()));
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Lift", "%.0f", robot.scorer.lift.position());
        telemetry.addData("Hang", "%.0f", robot.hang.position());
    }
}
