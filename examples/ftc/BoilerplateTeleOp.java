package robot.opmode;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import behavior.input.Bindings;
import robot.control.TeleOpBase;

@TeleOp(name = "OpMode name here")
@Disabled
public class BoilerplateTeleOp extends TeleOpBase {
    @Override
    protected void bindControls(Bindings controls) {
        // controls.onPress(() -> gamepad2.a, robot.intake.collect());
        // controls.whileHeld(() -> gamepad2.right_bumper, robot.intake.run());
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Pose", robot.drive.pose());
    }
}
