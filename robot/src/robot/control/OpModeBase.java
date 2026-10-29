package robot.control;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import behavior.runtime.BehaviorRunner;
import robot.Robot;
import sdk.auto.PoseMemory;
import sdk.ftc.GamepadController;
import sdk.ftc.Tuning;
import sdk.teleop.Controller;

/**
 * Runs the main loop shared by every OpMode. Extend {@link AutoBase} or {@link TeleOpBase}
 * rather than this class.
 *
 * <p>During init, every loop: subsystems read their sensors ({@code robot.initPeriodic()}, which
 * never moves a motor), then the OpMode's {@link #onInitLoop()}. After start, every loop:
 * subsystems update ({@code robot.periodic()}), then running behaviors tick, then the OpMode's
 * own {@link #onLoop()}. When it stops, behaviors are halted and the robot's pose is saved for
 * the next OpMode ({@link PoseMemory}).
 */
public abstract class OpModeBase extends LinearOpMode {
    protected Robot robot;
    protected final BehaviorRunner behaviors = new BehaviorRunner();

    /** Gamepad 1, usually the one who drives. */
    protected Controller driver;
    /** Gamepad 2, usually the one who runs the mechanisms. */
    protected Controller operator;

    @Override
    public final void runOpMode() {
        driver = new GamepadController(gamepad1);
        operator = new GamepadController(gamepad2);
        robot = new Robot(hardwareMap);
        onInit();

        while (opModeInInit()) {
            robot.initPeriodic();
            onInitLoop();
            if (!Tuning.applied().isEmpty()) {
                telemetry.addData("Saved tuning in use", String.join(", ", Tuning.applied()));
            }
            telemetry.update();
        }
        if (isStopRequested()) {
            return;
        }
        onStart();

        long lastLoop = System.nanoTime();
        while (opModeIsActive()) {
            robot.periodic();
            behaviors.tick();
            onLoop();
            long now = System.nanoTime();
            telemetry.addData("Loop", "%d ms", (now - lastLoop) / 1_000_000);
            lastLoop = now;
            telemetry.update();
        }
        behaviors.cancelAll();
        Pose pose = robot.drive.pose();
        PoseMemory.save(pose.getX(), pose.getY(), pose.getHeading());
    }

    /** After the robot is built, before start. */
    protected abstract void onInit();

    /** Every loop between init and start. */
    protected void onInitLoop() {}

    /** Once, when start is pressed. */
    protected abstract void onStart();

    /** Every loop, after behaviors tick. */
    protected abstract void onLoop();
}
