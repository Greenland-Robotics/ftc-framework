package robot.control;

import com.pedropathing.geometry.Pose;

import behavior.input.Bindings;
import sdk.auto.PoseMemory;
import sdk.teleop.DriveControls;
import sdk.teleop.MatchClock;

/**
 * Base class for driver-controlled OpModes. Gamepad 1 ({@code driver}) drives the robot out of
 * the box (see {@link #driveControls()}); connect buttons to behaviors in
 * {@link #bindControls(Bindings)}, usually on gamepad 2 ({@code operator}):
 * <pre>
 * controls.onPress(operator.a(), robot.claw.toggle(OPEN, CLOSED));
 * </pre>
 * The robot starts where the last OpMode (usually the Auto) left it. At {@link #endgameSeconds()}
 * left, {@link #onEndgame()} buzzes both gamepads.
 */
public abstract class TeleOpBase extends OpModeBase {
    private final Bindings controls = new Bindings(behaviors);
    private final MatchClock clock = new MatchClock(120);
    private DriveControls drive;
    private boolean endgameStarted;

    /** Anything else to set up before start. The robot exists by now. */
    protected void initialize() {}

    /** Connect buttons to behaviors, e.g. {@code controls.onPress(operator.a(), robot.intake.collect())}. */
    protected abstract void bindControls(Bindings controls);

    /** Runs every loop, e.g. for telemetry. */
    protected void runLoop() {}

    /**
     * How gamepad 1 drives: by default field-centric, right bumper for slow, back to reset the
     * heading. Override to change it, e.g. {@code return new DriveControls(driver).robotCentric();}
     */
    protected DriveControls driveControls() {
        return new DriveControls(driver);
    }

    /**
     * Drives with {@link #driveControls()} every loop. While a path is running (e.g. from a
     * button), stick input is ignored. Override to drive some other way.
     */
    protected void driveWithGamepad() {
        if (drive.resetHeadingPressed()) {
            robot.drive.resetHeading();
        }
        robot.drive.drive(drive.forward(), drive.strafe(), drive.turn(), !drive.isFieldCentric());
    }

    /** Seconds left in the 2:00 TeleOp. */
    protected double timeLeft() {
        return clock.timeLeft();
    }

    /** When the end game starts, in seconds left. */
    protected double endgameSeconds() {
        return 30;
    }

    /** Called once when the end game starts. By default buzzes both gamepads; override to add to it. */
    protected void onEndgame() {
        driver.rumble(800);
        operator.rumble(800);
    }

    @Override
    protected final void onInit() {
        if (PoseMemory.has()) {
            robot.drive.setStartingPose(new Pose(PoseMemory.x(), PoseMemory.y(), PoseMemory.heading()));
        }
        drive = driveControls();
        initialize();
        bindControls(controls);
    }

    @Override
    protected final void onStart() {
        clock.start();
    }

    @Override
    protected final void onLoop() {
        controls.update();
        driveWithGamepad();
        if (!endgameStarted && timeLeft() <= endgameSeconds()) {
            endgameStarted = true;
            onEndgame();
        }
        telemetry.addData("Time left", "%.0f s", timeLeft());
        runLoop();
    }
}
