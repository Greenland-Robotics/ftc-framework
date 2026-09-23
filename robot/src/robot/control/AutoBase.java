package robot.control;

import behavior.Node;

/**
 * Base class for autonomous OpModes: describe the whole routine as one behavior tree in
 * {@link #routine()}; it starts when start is pressed.
 */
public abstract class AutoBase extends OpModeBase {
    private Node routine;

    /** Set the starting pose, build paths... The robot exists by now. */
    protected void initialize() {}

    /** The whole autonomous as one behavior tree. Called during init, after {@link #initialize()}. */
    protected abstract Node routine();

    /** Runs every loop alongside the routine, e.g. for telemetry. */
    protected void runLoop() {}

    @Override
    protected final void onInit() {
        initialize();
        routine = routine();
    }

    @Override
    protected final void onStart() {
        behaviors.run(routine);
    }

    @Override
    protected final void onLoop() {
        runLoop();
    }
}
