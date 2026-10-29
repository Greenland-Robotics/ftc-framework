package sdk.teleop;

import java.util.Objects;

import behavior.runtime.Clock;

/**
 * How much of a period is left. TeleOpBase starts one at start for the 2:00 TeleOp; use
 * {@code timeLeft()} there for anything timed, e.g. park when 10 seconds are left:
 * <pre>
 * controls.onPress(() -> timeLeft() < 10, robot.drive.driveTo(PARK));
 * </pre>
 */
public final class MatchClock {
    private final Clock clock;
    private final double lengthSeconds;
    private long startedAt = -1;

    public MatchClock(double lengthSeconds) {
        this(lengthSeconds, Clock.system());
    }

    public MatchClock(double lengthSeconds, Clock clock) {
        this.lengthSeconds = lengthSeconds;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void start() {
        startedAt = clock.nowMillis();
    }

    public boolean isStarted() {
        return startedAt >= 0;
    }

    /** Seconds since {@link #start()}, or 0 before it. */
    public double elapsed() {
        return isStarted() ? (clock.nowMillis() - startedAt) / 1000.0 : 0;
    }

    /** Seconds left in the period, never below 0. */
    public double timeLeft() {
        return Math.max(0, lengthSeconds - elapsed());
    }
}
