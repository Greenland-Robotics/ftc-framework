package behavior.decorator;

import behavior.Node;
import behavior.Status;

/** Runs the child again when it fails, up to {@code attempts} tries in total. */
public final class Retry extends Decorator {
    private final int attempts;
    private int failures;

    public Retry(Node child, int attempts) {
        super(child);
        if (attempts < 1) {
            throw new IllegalArgumentException("attempts must be at least 1");
        }
        this.attempts = attempts;
    }

    @Override
    public Status tick() {
        Status status = child.tick();
        if (status == Status.FAILURE && ++failures < attempts) {
            return Status.RUNNING; // the child starts over on the next tick
        }
        if (status.isDone()) {
            failures = 0;
        }
        return status;
    }

    @Override
    public void halt() {
        failures = 0;
        child.halt();
    }
}
