package sdk;

import behavior.runtime.Clock;

/** A clock that only moves when a test says so. */
public final class FakeClock implements Clock {
    private long now;

    @Override
    public long nowMillis() {
        return now;
    }

    public void advance(long millis) {
        now += millis;
    }
}
