package robot.opmode;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import behavior.input.Bindings;
import robot.control.TeleOpBase;

/**
 * Gamepad 1 drives (built into TeleOpBase); gamepad 2 runs the intake.
 * Needs the example Intake subsystem registered in Robot.java as {@code intake}.
 */
@TeleOp(name = "Example TeleOp")
public class ExampleTeleOp extends TeleOpBase {
    @Override
    protected void bindControls(Bindings controls) {
        controls.onPress(() -> gamepad2.a, robot.intake.collect().withTimeout(3000));
        controls.onPress(() -> gamepad2.b, robot.intake.eject());
        controls.toggleOnPress(() -> gamepad2.x, robot.intake.run());

        // Hold Y to drive to the middle of the field; let go to take back control
        controls.whileHeld(() -> gamepad1.y, robot.drive.driveTo(new Pose(72, 72, 0)));
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Has sample", robot.intake.hasSample());
        telemetry.addData("Pose", robot.drive.pose());
    }
}
