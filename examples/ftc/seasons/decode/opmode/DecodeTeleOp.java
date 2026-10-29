package robot.opmode;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import behavior.input.Bindings;
import robot.control.TeleOpBase;

/**
 * Driver: drives (built in), holds A to drive into the launch spot facing the goal.
 * Operator: right bumper runs the intake, A shoots one, Y shoots three, B spits out.
 */
@TeleOp(name = "DECODE TeleOp")
public class DecodeTeleOp extends TeleOpBase {
    private static final Pose LAUNCH_SPOT = new Pose(60, 84, Math.toRadians(135));   // Blue; measure yours
    private static final Pose BASE = new Pose(105, 33, Math.toRadians(90));

    @Override
    protected void bindControls(Bindings controls) {
        controls.whileHeld(operator.rightBumper(), robot.intake.run());
        controls.whileHeld(operator.b(), robot.intake.reverse());
        controls.onPress(operator.a(), robot.launcher.shootOne());
        controls.onPress(operator.y(), robot.launcher.shootThree());

        controls.whileHeld(driver.a(), robot.drive.driveTo(LAUNCH_SPOT));
        // Last 15 seconds: one press of Y drives into the base to park
        controls.onPress(() -> timeLeft() < 15 && driver.y().getAsBoolean(), robot.drive.driveTo(BASE));
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Flywheel", "%.0f / %.0f rpm", robot.launcher.flywheel.rpm(), robot.launcher.flywheel.targetRpm());
        telemetry.addData("Artifact in intake", robot.intake.hasPiece());
    }
}
