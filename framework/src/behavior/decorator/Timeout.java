package behavior.decorator;

import java.util.Objects;

import behavior.Node;
import behavior.Status;
import behavior.runtime.Clock;

/**
 * Fails if the child doesn't finish in time, halting it. To give up but carry on anyway, add
 * {@code .alwaysSucceed()}.
 */
public final class Timeout extends Decorator {
    private final long millis;
    private final Clock clock;
    private boolean running;
    private long startedAt;

    public Timeout(Node child, long millis, Clock clock) {
        super(child);
        if (millis < 0) {
            throw new IllegalArgumentException("millis must not be negative");
        }
        this.millis = millis;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Status tick() {
        if (!running) {
            running = true;
            startedAt = clock.nowMillis();
        }
        if (clock.nowMillis() - startedAt >= millis) {
            halt();
            return Status.FAILURE;
        }
        Status status = child.tick();
        if (status.isDone()) {
            running = false;
        }
        return status;
    }

    @Override
    public void halt() {
        running = false;
        child.halt();
    }
}
