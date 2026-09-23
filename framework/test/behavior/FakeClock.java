package behavior;

import behavior.runtime.Clock;

/** A clock that only moves when a test says so. */
final class FakeClock implements Clock {
    private long now;

    @Override
    public long nowMillis() {
        return now;
    }

    void advance(long millis) {
        now += millis;
    }
}
