package robot.opmode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import behavior.input.Bindings;
import robot.control.TeleOpBase;
import robot.subsystems.Scorer;

/**
 * Operator: A grabs a sample (retrying past the other alliance's), Y scores it in the high
 * basket, X hangs a specimen, B toggles the claw. Hold the left bumper to move the lift by hand
 * with the right stick. End game (dpad): up reaches for the bar, down pulls the robot up.
 */
@TeleOp(name = "INTO THE DEEP TeleOp")
public class IntoTheDeepTeleOp extends TeleOpBase {
    @Override
    protected void bindControls(Bindings controls) {
        controls.onPress(operator.a(), robot.intake.grab().retry(3));
        controls.onPress(operator.y(), robot.scorer.scoreBasket());
        controls.onPress(operator.x(), robot.scorer.hangSpecimen());
        controls.onPress(operator.b(), robot.scorer.claw.toggle(Scorer.OPEN, Scorer.CLOSED));
        controls.whileHeld(operator.leftBumper(), robot.scorer.lift.manual(operator.rightStickY()));

        controls.onPress(operator.dpadUp(), robot.scorer.lift.goTo(Scorer.HANG_REACH));
        controls.onPress(operator.dpadDown(), robot.scorer.lift.goTo(Scorer.HANG_PULL));
    }

    @Override
    protected void onEndgame() {
        super.onEndgame();
        telemetry.speak("Hang time");
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Lift", "%.0f → %.0f", robot.scorer.lift.position(), robot.scorer.lift.target());
        telemetry.addData("Ours in the intake", robot.intake.holdsOurs());
    }
}
