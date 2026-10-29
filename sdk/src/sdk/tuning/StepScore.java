package sdk.tuning;

/**
 * Scores one move from {@code from} to {@code to}: how far it overshot, how long it took to
 * settle (inside the tolerance and staying there), and where it ended up.
 */
public final class StepScore {
    private static final long SETTLE_HOLD_MS = 300;

    private final double from;
    private final double to;
    private final double tolerance;
    private final long startedAt;
    private double furthest;
    private long insideSince = -1;
    private long settledAt = -1;
    private double last;

    public StepScore(double from, double to, double tolerance, long nowMillis) {
        this.from = from;
        this.to = to;
        this.tolerance = tolerance;
        this.startedAt = nowMillis;
        this.furthest = from;
        this.last = from;
    }

    /** Records where it is now. */
    public void sample(double position, long nowMillis) {
        last = position;
        double direction = Math.signum(to - from);
        if ((position - furthest) * direction > 0) furthest = position;
        boolean inside = Math.abs(position - to) <= tolerance;
        if (!inside) {
            insideSince = -1;
            settledAt = -1;
        } else if (insideSince < 0) {
            insideSince = nowMillis;
        } else if (settledAt < 0 && nowMillis - insideSince >= SETTLE_HOLD_MS) {
            settledAt = insideSince;
        }
    }

    public boolean isSettled() {
        return settledAt >= 0;
    }

    /** How long it took to get inside the tolerance for good, or -1 if it hasn't. */
    public long settleMillis() {
        return isSettled() ? settledAt - startedAt : -1;
    }

    /** How far past the target it went, as a fraction of the move (0.1 = 10%). */
    public double overshoot() {
        double size = Math.abs(to - from);
        if (size == 0) return 0;
        return Math.max(0, (furthest - to) * Math.signum(to - from)) / size;
    }

    /** How far from the target it is now. */
    public double error() {
        return to - last;
    }

    public String describe() {
        return String.format(java.util.Locale.ROOT, "overshoot %.0f%%, %s",
                overshoot() * 100, isSettled() ? "settles in " + settleMillis() + " ms"
                        : "didn't settle (off by " + TuneReport.number(error()) + ")");
    }
}
