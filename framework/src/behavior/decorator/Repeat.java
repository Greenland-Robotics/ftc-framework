package behavior.decorator;

import behavior.Node;
import behavior.Status;

/** Runs the child again each time it succeeds, {@code times} successes in total. Stops on failure. */
public final class Repeat extends Decorator {
    /** Pass as {@code times} to repeat until the child fails or the Repeat is halted. */
    public static final int FOREVER = -1;

    private final int times;
    private int successes;

    public Repeat(Node child, int times) {
        super(child);
        if (times < 1 && times != FOREVER) {
            throw new IllegalArgumentException("times must be at least 1, or Repeat.FOREVER");
        }
        this.times = times;
    }

    @Override
    public Status tick() {
        Status status = child.tick();
        if (status == Status.SUCCESS && (times == FOREVER || ++successes < times)) {
            return Status.RUNNING; // the child starts over on the next tick
        }
        if (status.isDone()) {
            successes = 0;
        }
        return status;
    }

    @Override
    public void halt() {
        successes = 0;
        child.halt();
    }
}
