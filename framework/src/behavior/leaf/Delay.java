package behavior.leaf;

import java.util.Objects;

import behavior.Action;
import behavior.Status;
import behavior.runtime.Clock;

/** Stays RUNNING for a fixed time, then succeeds. Unlike sleep(), the rest of the robot keeps running. */
public final class Delay extends Action {
    private final long millis;
    private final Clock clock;
    private long startedAt;

    public Delay(long millis, Clock clock) {
        if (millis < 0) {
            throw new IllegalArgumentException("millis must not be negative");
        }
        this.millis = millis;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    protected void start() {
        startedAt = clock.nowMillis();
    }

    @Override
    protected Status update() {
        return clock.nowMillis() - startedAt >= millis ? Status.SUCCESS : Status.RUNNING;
    }
}
