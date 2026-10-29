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
        // Gamepad 1 (driver) already drives. Bind gamepad 2 (operator) to behaviors:
        // controls.onPress(operator.a(), robot.intake.collect());
        // controls.whileHeld(operator.rightBumper(), robot.intake.run());
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Pose", robot.drive.pose());
    }
}
