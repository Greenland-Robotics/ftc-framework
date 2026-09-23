package gcsrobotics.control;

import gcsrobotics.vertices.CommandRunner;

public abstract class TeleOpBase extends OpModeBase {

    protected abstract void initialize();
    protected abstract void runLoop();

    protected final CommandRunner commandRunner = new CommandRunner();
    protected boolean driveMode = true;

    @Override
    protected void initInternal() {
        initialize();
        follower.startTeleOpDrive();
    }

    @Override
    protected void loopInternal() {
        follower.update();
        commandRunner.update();


        if (driveMode) {
            follower.setTeleOpDrive(
                    -gamepad1.left_stick_y,
                    -gamepad1.left_stick_x,
                    -gamepad1.right_stick_x,
                    false
            );
        }

        runLoop();
    }
}
