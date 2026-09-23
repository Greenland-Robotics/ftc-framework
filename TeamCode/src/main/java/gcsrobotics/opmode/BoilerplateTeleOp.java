package gcsrobotics.opmode;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import gcsrobotics.control.TeleOpBase;
import gcsrobotics.vertices.ButtonAction;

@TeleOp(name="OpMode name here")
@Disabled
public class BoilerplateTeleOp extends TeleOpBase {
    private boolean driveMode = true;
    // Name your actions based on what they do, eg. `shotSequence`
    private ButtonAction action1, action2 /* ... */;

    @Override
    protected void initialize() {
        // action1 = new ButtonAction etc.
        // action2 = new ButtonAction etc.
        // ....
    }

    @Override
    protected void runLoop() {
        // action1.update(your gamepad button here)
        // action2.update(your gamepad button here)
    }
}
