package robot.control;

import behavior.input.Bindings;

/**
 * Base class for driver-controlled OpModes. Gamepad 1's sticks drive the robot (see
 * {@link #driveWithGamepad()}); map buttons to behaviors in {@link #bindControls(Bindings)}.
 */
public abstract class TeleOpBase extends OpModeBase {
    private final Bindings controls = new Bindings(behaviors);

    /** Anything else to set up before start. The robot exists by now. */
    protected void initialize() {}

    /** Connect buttons to behaviors, e.g. {@code controls.onPress(() -> gamepad2.a, robot.intake.collect())}. */
    protected abstract void bindControls(Bindings controls);

    /** Runs every loop, e.g. for telemetry. */
    protected void runLoop() {}

    /**
     * Drives with gamepad 1: left stick moves, right stick turns. Override to change the mapping.
     * While a path is running (e.g. from a button), stick input is ignored.
     */
    protected void driveWithGamepad() {
        robot.drive.drive(-gamepad1.left_stick_y, -gamepad1.left_stick_x, -gamepad1.right_stick_x);
    }

    @Override
    protected final void onInit() {
        initialize();
        bindControls(controls);
    }

    @Override
    protected final void onStart() {}

    @Override
    protected final void onLoop() {
        controls.update();
        driveWithGamepad();
        runLoop();
    }
}
