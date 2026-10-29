package robot.opmode;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import behavior.input.Bindings;
import robot.control.TeleOpBase;

/**
 * Gamepad 1 ({@code driver}) drives, built into TeleOpBase; gamepad 2 ({@code operator}) runs the intake.
 * Needs the example Intake subsystem registered in Robot.java as {@code intake}.
 */
@TeleOp(name = "Example TeleOp")
public class ExampleTeleOp extends TeleOpBase {
    @Override
    protected void bindControls(Bindings controls) {
        controls.onPress(operator.a(), robot.intake.collect().withTimeout(3000));
        controls.onPress(operator.b(), robot.intake.eject());
        controls.toggleOnPress(operator.x(), robot.intake.run());

        // Hold Y to drive to the middle of the field; let go to take back control
        controls.whileHeld(driver.y(), robot.drive.driveTo(new Pose(72, 72, 0)));

        // With 10 seconds left, park by itself; the sticks work again once it gets there
        controls.onPress(() -> timeLeft() < 10, robot.drive.driveTo(new Pose(60, 60, Math.toRadians(90))));
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Has sample", robot.intake.hasSample());
        telemetry.addData("Pose", robot.drive.pose());
    }
}
