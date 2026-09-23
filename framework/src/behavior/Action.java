package behavior;

/**
 * Base class for leaves that do work over several loops, like running a motor until a sensor
 * trips. Override the hooks you need; this class handles the lifecycle so an Action can be
 * reused and restarted any number of times:
 * <ol>
 *   <li>{@link #start()} runs on the first tick,</li>
 *   <li>{@link #update()} runs every tick (including the first) and reports progress,</li>
 *   <li>{@link #end(boolean)} runs once when the Action finishes or is halted.</li>
 * </ol>
 */
public abstract class Action implements Node {
    private boolean running;

    @Override
    public final Status tick() {
        if (!running) {
            running = true;
            start();
        }
        Status status = update();
        if (status.isDone()) {
            running = false;
            end(false);
        }
        return status;
    }

    @Override
    public final void halt() {
        if (running) {
            running = false;
            end(true);
        }
    }

    public final boolean isRunning() {
        return running;
    }

    /** Called on the first tick of each run, before {@link #update()}. */
    protected void start() {}

    /** Called every tick: do a slice of work and report RUNNING, SUCCESS or FAILURE. */
    protected abstract Status update();

    /**
     * Called once when this Action stops: put hardware in a safe state here.
     *
     * @param interrupted true if a parent halted it before it finished
     */
    protected void end(boolean interrupted) {}
}
