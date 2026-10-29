package sdk.parts;

import java.util.ArrayList;
import java.util.List;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import behavior.runtime.Clock;
import sdk.tuning.StepScore;
import sdk.tuning.TuneReport;

/**
 * Tunes a {@link Lift} or {@link Arm} by itself, in three experiments, all within its limits:
 * <ol>
 *   <li><b>Gravity and friction.</b> Raises the power slowly until it starts moving up, lifts it
 *       clear of the bottom, then lowers the power until it starts moving down. Halfway between is what holds it up (gravity);
 *       half the gap is what it takes to get it unstuck (friction).</li>
 *   <li><b>How it responds.</b> Pushes it up and down around the middle of its range, a fixed
 *       amount each way (a "relay test"), and measures how far and how fast it swings. That
 *       gives a first kP and kD (the Åström–Hägglund method).</li>
 *   <li><b>Check and refine.</b> Moves between 30% and 70% of the range with those gains and
 *       scores each move: too much overshoot softens kP and adds kD, too slow raises kP,
 *       stopping short adds friction push. Up to six rounds, keeping the best.</li>
 * </ol>
 * If it fails or is stopped, the gains it started with are put back.
 */
final class PositionTuner extends Action {
    private enum Phase { RAMP_UP, LIFT_OFF, RAMP_DOWN, RELAY, PLACE, STEP_UP, STEP_DOWN, DONE }

    private static final double RAMP_PER_SECOND = 0.12;
    private static final long RELAY_TIMEOUT_MS = 20_000;
    private static final long STEP_TIMEOUT_MS = 4_000;
    private static final int ROUNDS = 6;

    private final PositionMechanism<?> part;
    private final Clock clock;
    private final TuneReport report;
    private final Speedometer speed = new Speedometer();

    private Phase phase;
    private long last;
    private long phaseStart;
    private double range;
    private double low;
    private double high;
    private double mid;
    private double[] original;

    // Experiment 1
    private double power;
    private int movingFor;
    private double pUp;
    private double shapeUp;

    // Experiment 2
    private double relay;
    private boolean above;
    private int crossings;
    private long lastUpCross;
    private double cycleMax;
    private double cycleMin;
    private final List<Double> periods = new ArrayList<>();
    private final List<Double> swings = new ArrayList<>();

    // Experiment 3
    private int round;
    private StepScore score;
    private StepScore upScore;
    private double bestCost = Double.MAX_VALUE;
    private double[] best;

    PositionTuner(PositionMechanism<?> part, Clock clock, TuneReport report) {
        this.part = part;
        this.clock = clock;
        this.report = report;
    }

    @Override
    protected void start() {
        report.clear();
        original = new double[] {part.kP(), part.kD(), part.gravityGain(), part.frictionGain()};
        last = phaseStart = clock.nowMillis();
        speed.clear();
        phase = Phase.RAMP_UP;
        power = 0;
        movingFor = 0;
        round = 0;
        best = null;
        bestCost = Double.MAX_VALUE;
        periods.clear();
        swings.clear();

        if (Double.isInfinite(part.min()) || Double.isInfinite(part.max())) {
            fail("set .limits(min, max) first: the tuner only moves inside them");
            return;
        }
        range = part.max() - part.min();
        low = part.min() + 0.3 * range;
        high = part.min() + 0.7 * range;
        mid = part.min() + 0.5 * range;
        report.log("Tuning between %s and %s. Keep hands clear.", TuneReport.number(part.min()), TuneReport.number(part.max()));
        report.log("1/3 Finding gravity and friction...");
        part.drive(0);
    }

    @Override
    protected Status update() {
        if (phase == Phase.DONE) return report.failed() ? Status.FAILURE : Status.SUCCESS;
        long now = clock.nowMillis();
        double seconds = (now - last) / 1000.0;
        last = now;
        double position = part.position();
        double velocity = speed.update(position, now);
        double moving = 0.015 * range;   // "Clearly moving": 1.5% of the range per second

        switch (phase) {
            case RAMP_UP:
                if (velocity > moving) movingFor++; else movingFor = 0;
                if (movingFor >= 3 || position > part.max() - 0.05 * range) {
                    pUp = power;
                    shapeUp = part.gravityShape(position);
                    movingFor = 0;
                    phase = Phase.LIFT_OFF;
                    phaseStart = now;
                } else if (velocity < -moving && power > 0.4) {
                    return fail("it moves the wrong way for positive power: add .reversed() to its motor");
                } else if (power >= 1) {
                    return fail("it didn't move even at full power: check the wiring, the motor name and its limits");
                }
                power = Math.min(1, power + RAMP_PER_SECOND * seconds);
                part.drive(power);
                break;

            case LIFT_OFF:
                // Up to a quarter of the way, so there is room to measure it coming down
                if (position >= part.min() + 0.25 * range || now - phaseStart > 3000) {
                    power = pUp;
                    speed.clear();
                    phase = Phase.RAMP_DOWN;
                } else {
                    part.drive(Math.min(1, pUp + 0.1));
                }
                break;

            case RAMP_DOWN:
                if (velocity < -moving) movingFor++; else movingFor = 0;
                if (movingFor >= 3 || position < part.min() + 0.05 * range) {
                    double shapeDown = part.gravityShape(position);
                    // Moving up:   pUp   = gravity × shapeUp   + friction
                    // Moving down: pDown = gravity × shapeDown - friction
                    double shapes = shapeUp + shapeDown;
                    double gravity = Math.abs(shapes) < 0.3 ? part.gravityGain() : (pUp + power) / shapes;
                    double friction = Math.max(0, pUp - gravity * shapeUp);
                    part.setGains(part.kP(), part.kD(), gravity, friction);
                    report.log("   gravity %s, friction %s (moves up at %s power, down at %s)",
                            TuneReport.number(gravity), TuneReport.number(friction),
                            TuneReport.number(pUp), TuneReport.number(power));
                    report.log("2/3 Swinging it around the middle...");
                    startRelay(0.25, now);
                } else if (power <= -1) {
                    return fail("it didn't come down: check the motor direction and its limits");
                }
                power = Math.max(-1, power - RAMP_PER_SECOND * seconds);
                part.drive(power);
                break;

            case RELAY:
                return relay(position, now);

            case PLACE:
            case STEP_UP:
            case STEP_DOWN:
                return verify(position, now);

            default:
                break;
        }
        return Status.RUNNING;
    }

    @Override
    protected void end(boolean interrupted) {
        if (interrupted && !report.isFinished()) {
            restore();
            report.fail("stopped before it finished; the gains are back to what they were");
        }
        part.hold(Double.NaN);
    }

    // ---- Experiment 2: relay ----

    private void startRelay(double amount, long now) {
        relay = amount;
        crossings = 0;
        lastUpCross = -1;
        above = part.position() > mid;
        cycleMax = cycleMin = part.position();
        periods.clear();
        swings.clear();
        phase = Phase.RELAY;
        phaseStart = now;
    }

    private Status relay(double position, long now) {
        if (now - phaseStart > RELAY_TIMEOUT_MS) {
            return fail("it never swung steadily around the middle; tune it by hand (raise kP until it wobbles)");
        }
        boolean outside = position < part.min() + 0.1 * range || position > part.max() - 0.1 * range;
        if (crossings > 0 && outside) {
            if (relay * 0.6 < 0.04) return fail("it swings too far even when pushed gently; tune it by hand");
            report.log("   swinging too far, pushing more gently");
            startRelay(relay * 0.6, now);
            return Status.RUNNING;
        }

        cycleMax = Math.max(cycleMax, position);
        cycleMin = Math.min(cycleMin, position);
        boolean nowAbove = position > mid;
        if (nowAbove != above) {
            above = nowAbove;
            crossings++;
            if (nowAbove) {
                if (lastUpCross >= 0 && crossings > 3) {   // Skip the first swing, while it settles into a rhythm
                    periods.add((now - lastUpCross) / 1000.0);
                    swings.add((cycleMax - cycleMin) / 2);
                }
                lastUpCross = now;
                cycleMax = cycleMin = position;
            }
        }

        if (periods.size() >= 4) {
            double period = average(periods);
            double swing = average(swings);
            double push = Math.max(relay - part.frictionGain(), relay * 0.5);
            double ultimate = 4 * push / (Math.PI * Math.max(swing, 1e-9));
            double kP = 0.25 * ultimate;
            double kD = kP * period / 8;
            part.setGains(kP, kD, part.gravityGain(), part.frictionGain());
            report.log("   swings ±%s every %.2f s: kP %s, kD %s",
                    TuneReport.number(swing), period, TuneReport.number(kP), TuneReport.number(kD));
            report.log("3/3 Test moves...");
            part.hold(low);
            phase = Phase.PLACE;
            phaseStart = now;
            return Status.RUNNING;
        }

        double toward = position < mid ? relay : -relay;
        part.drive(part.gravityGain() * part.gravityShape(position) + toward);
        return Status.RUNNING;
    }

    // ---- Experiment 3: test moves ----

    private Status verify(double position, long now) {
        if (phase == Phase.PLACE) {
            if (Math.abs(position - low) <= part.tolerance() * 2 || now - phaseStart > STEP_TIMEOUT_MS) {
                startStep(Phase.STEP_UP, low, high, now);
            }
            return Status.RUNNING;
        }

        score.sample(position, now);
        boolean timedOut = now - phaseStart > STEP_TIMEOUT_MS;
        boolean done = score.isSettled() && now - phaseStart > score.settleMillis() + 400;
        if (!done && !timedOut) return Status.RUNNING;

        if (phase == Phase.STEP_UP) {
            upScore = score;
            startStep(Phase.STEP_DOWN, high, low, now);
            return Status.RUNNING;
        }
        return judge(upScore, score, now);
    }

    private void startStep(Phase step, double from, double to, long now) {
        phase = step;
        phaseStart = now;
        score = new StepScore(from, to, part.tolerance(), now);
        part.hold(to);
    }

    private Status judge(StepScore up, StepScore down, long now) {
        round++;
        double overshoot = Math.max(up.overshoot(), down.overshoot());
        boolean settled = up.isSettled() && down.isSettled();
        long settle = settled ? Math.max(up.settleMillis(), down.settleMillis()) : STEP_TIMEOUT_MS;
        double error = Math.max(Math.abs(up.error()), Math.abs(down.error()));
        report.log("   round %d: up %s; down %s", round, up.describe(), down.describe());

        double cost = settle + overshoot * 10_000 + (settled ? 0 : 5_000);
        if (cost < bestCost) {
            bestCost = cost;
            best = new double[] {part.kP(), part.kD(), part.gravityGain(), part.frictionGain()};
        }

        double kP = part.kP();
        double kD = part.kD();
        double kS = part.frictionGain();
        String change;
        if (overshoot > 0.08) {
            kP *= 0.75;
            kD *= 1.4;
            change = "overshoots: softer kP, more kD";
        } else if (!settled && error <= part.tolerance() * 6) {
            kS = kS * 1.3 + 0.01;
            change = "stops just short: more friction push";
        } else if (!settled) {
            kD *= 0.7;
            kP *= 1.2;
            change = "doesn't settle: more kP, less kD";
        } else if (settle > 1200 && overshoot < 0.03) {
            kP *= 1.3;
            change = "slow: more kP";
        } else {
            return finish(now);
        }
        if (round >= ROUNDS) return finish(now);

        report.log("   %s", change);
        part.setGains(kP, kD, part.gravityGain(), kS);
        part.hold(low);        // Back to the low point first, unscored
        phase = Phase.PLACE;
        phaseStart = now;
        return Status.RUNNING;
    }

    private Status finish(long now) {
        if (best != null) part.setGains(best[0], best[1], best[2], best[3]);
        part.hold(low);
        report.log("Done. Paste this where it's created in Robot.java:");
        report.log("   %s", part.codeLine());
        report.finish();
        phase = Phase.DONE;
        return Status.SUCCESS;
    }

    private Status fail(String why) {
        restore();
        part.hold(Double.NaN);
        report.fail(why);
        phase = Phase.DONE;
        return Status.FAILURE;
    }

    private void restore() {
        part.setGains(original[0], original[1], original[2], original[3]);
    }

    /** Where it is, for the behavior tree visualizer and debugging. */
    @Override
    public String toString() {
        if (phase == null) return "Tunes gravity, friction, kP and kD by itself";
        switch (phase) {
            case RAMP_UP: return String.format(java.util.Locale.ROOT, "1/3 Raising the power until it moves up: %.2f", power);
            case LIFT_OFF: return "1/3 Lifting it clear of the bottom";
            case RAMP_DOWN: return String.format(java.util.Locale.ROOT, "1/3 Lowering the power until it moves down: %.2f", power);
            case RELAY: return String.format(java.util.Locale.ROOT, "2/3 Swinging around %s, ±%.2f power: %d swings measured",
                    TuneReport.number(mid), relay, periods.size());
            case DONE: return report.failed() ? "Stopped: " + report.failure() : "Done: " + part.codeLine();
            default: return String.format(java.util.Locale.ROOT, "3/3 Test moves, round %d: %s", round + 1, part.codeLine());
        }
    }

    private static double average(List<Double> values) {
        double sum = 0;
        for (double value : values) sum += value;
        return sum / values.size();
    }

    // ---- By hand: one scored test ----

    static Node stepTest(PositionMechanism<?> part, Clock clock, TuneReport report) {
        return new StepTest(part, clock, report);
    }

    /** Goes to 30% of the range, then scores a move to 70% and one back. */
    private static final class StepTest extends Action {
        private final PositionMechanism<?> part;
        private final Clock clock;
        private final TuneReport report;
        private int leg;
        private long legStart;
        private StepScore score;
        private double low;
        private double high;

        StepTest(PositionMechanism<?> part, Clock clock, TuneReport report) {
            this.part = part;
            this.clock = clock;
            this.report = report;
        }

        @Override
        protected void start() {
            leg = 0;
            legStart = clock.nowMillis();
            double range = part.max() - part.min();
            low = part.min() + 0.3 * range;
            high = part.min() + 0.7 * range;
            if (Double.isInfinite(range)) {
                low = part.position();
                high = part.position() + 500;
            }
            part.hold(low);
        }

        @Override
        protected Status update() {
            long now = clock.nowMillis();
            double position = part.position();
            if (leg == 0) {
                if (Math.abs(position - low) <= part.tolerance() * 2 || now - legStart > STEP_TIMEOUT_MS) next(low, high, now);
                return Status.RUNNING;
            }
            score.sample(position, now);
            boolean done = score.isSettled() && now - legStart > score.settleMillis() + 400;
            if (!done && now - legStart <= STEP_TIMEOUT_MS) return Status.RUNNING;
            report.log("%s: %s", leg == 1 ? "Up" : "Down", score.describe());
            if (leg == 2) return Status.SUCCESS;
            next(high, low, now);
            return Status.RUNNING;
        }

        private void next(double from, double to, long now) {
            leg++;
            legStart = now;
            score = new StepScore(from, to, part.tolerance(), now);
            part.hold(to);
        }
    }

    /** Speed from the last ~120 ms of positions, which is steadier than one loop's change. */
    static final class Speedometer {
        private final long[] times = new long[16];
        private final double[] positions = new double[16];
        private int count;
        private int next;

        void clear() {
            count = 0;
            next = 0;
        }

        double update(double position, long now) {
            times[next] = now;
            positions[next] = position;
            next = (next + 1) % times.length;
            count = Math.min(count + 1, times.length);
            int newest = (next + times.length - 1) % times.length;
            for (int back = count - 1; back > 0; back--) {
                int i = (newest - back + times.length) % times.length;
                if (now - times[i] <= 120) {
                    long span = now - times[i];
                    return span > 0 ? (position - positions[i]) * 1000.0 / span : 0;
                }
            }
            return 0;
        }
    }
}
