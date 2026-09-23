package robot.control;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import behavior.runtime.BehaviorRunner;
import robot.Robot;

/**
 * Runs the main loop shared by every OpMode. Extend {@link AutoBase} or {@link TeleOpBase}
 * rather than this class.
 *
 * <p>Every loop: subsystems update ({@code robot.periodic()}), then running behaviors tick, then
 * the OpMode's own {@link #onLoop()}.
 */
public abstract class OpModeBase extends LinearOpMode {
    protected Robot robot;
    protected final BehaviorRunner behaviors = new BehaviorRunner();

    @Override
    public final void runOpMode() {
        robot = new Robot(hardwareMap);
        onInit();

        waitForStart();
        onStart();

        while (opModeIsActive()) {
            robot.periodic();
            behaviors.tick();
            onLoop();
            telemetry.update();
        }
        behaviors.cancelAll();
    }

    /** After the robot is built, before start. */
    protected abstract void onInit();

    /** Once, when start is pressed. */
    protected abstract void onStart();

    /** Every loop, after behaviors tick. */
    protected abstract void onLoop();
}
