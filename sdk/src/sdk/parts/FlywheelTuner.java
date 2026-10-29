package sdk.parts;

import java.util.ArrayList;
import java.util.List;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import behavior.runtime.Clock;
import sdk.tuning.TuneReport;

/**
 * Tunes a {@link Flywheel} by itself:
 * <ol>
 *   <li><b>Feed-forward.</b> Runs it at 35% and then 75% power and measures the rpm of each. The
 *       straight line through them gives {@code maxRpm} (rpm per unit of power) and
 *       {@code friction} (the power where the line starts).</li>
 *   <li><b>kP.</b> Steps from 40% to 70% of its top speed with a small kP, then again with double,
 *       and so on, while it still spins up without overshooting by more than 5% or wobbling by
 *       more than 1%. Uses half the largest kP that passed, for a safety margin, and sets the
 *       tolerance from the wobble.</li>
 * </ol>
 * If it fails or is stopped, the gains it started with are put back and the wheel stops.
 */
final class FlywheelTuner extends Action {
    private enum Phase { LOW_POWER, HIGH_POWER, SETTLE, STEP, DONE }

    private static final long HOLD_MS = 1800;
    private static final long MEASURE_MS = 600;
    private static final int TRIALS = 9;

    private final Flywheel wheel;
    private final Clock clock;
    private final TuneReport report;
    private final List<Double> samples = new ArrayList<>();

    private Phase phase;
    private long phaseStart;
    private double[] original;
    private double rpmLow;
    private double target;
    private double settleRpm;
    private double trialKp;
    private int trial;
    private double bestKp = -1;
    private double bestWobble;
    private double peak;

    FlywheelTuner(Flywheel wheel, Clock clock, TuneReport report) {
        this.wheel = wheel;
        this.clock = clock;
        this.report = report;
    }

    @Override
    protected void start() {
        report.clear();
        original = new double[] {wheel.maxRpmGain(), wheel.frictionGain(), wheel.kP(), wheel.toleranceRpm()};
        trial = 0;
        bestKp = -1;
        report.log("Tuning the flywheel. Keep hands and game pieces clear.");
        report.log("1/2 Measuring speed at 35%% and 75%% power...");
        begin(Phase.LOW_POWER);
        wheel.drive(0.35);
    }

    @Override
    protected Status update() {
        if (phase == Phase.DONE) return report.failed() ? Status.FAILURE : Status.SUCCESS;
        long now = clock.nowMillis();
        double rpm = wheel.rpm();
        long elapsed = now - phaseStart;
        if (elapsed > HOLD_MS - MEASURE_MS) samples.add(rpm);
        if (phase == Phase.STEP) peak = Math.max(peak, rpm);
        if (elapsed < HOLD_MS) return Status.RUNNING;

        switch (phase) {
            case LOW_POWER:
                rpmLow = average(samples);
                begin(Phase.HIGH_POWER);
                wheel.drive(0.75);
                break;

            case HIGH_POWER: {
                double rpmHigh = average(samples);
                if (rpmHigh < 0 || rpmLow < 0) {
                    return fail("it spins backwards: add .reversed() to its motor (and check the encoder)");
                }
                if (rpmHigh < rpmLow * 1.1 + 50) {
                    return fail("its speed hardly changed with power: check the encoder cable and ticks per turn");
                }
                double perPower = (rpmHigh - rpmLow) / 0.4;
                double friction = Math.max(0, 0.35 - rpmLow / perPower);
                wheel.setGains(perPower, friction, 0.0001, wheel.toleranceRpm());
                report.log("   %.0f rpm at 35%%, %.0f rpm at 75%%: maxRpm %s, friction %s",
                        rpmLow, rpmHigh, TuneReport.number(perPower), TuneReport.number(friction));
                target = 0.7 * perPower * (1 - friction);
                report.log("2/2 Finding kP with spin-ups to %.0f rpm...", target);
                wheel.drive(Double.NaN);
                trialKp = 0.0001;
                startTrial();
                break;
            }

            case SETTLE:
                begin(Phase.STEP);
                peak = 0;
                wheel.target(target);
                break;

            case STEP: {
                double mean = average(samples);
                double wobble = deviation(samples, mean) / target;
                double overshoot = Math.max(0, peak - target) / target;
                boolean good = overshoot < 0.05 && wobble < 0.01 && Math.abs(mean - target) < 0.03 * target;
                report.log("   kP %s: overshoot %.1f%%, wobble %.1f%% %s", TuneReport.number(trialKp),
                        overshoot * 100, wobble * 100, good ? "ok" : "too much");
                if (good) {
                    bestKp = trialKp;
                    bestWobble = wobble;
                }
                if (!good || ++trial >= TRIALS) return finish();
                trialKp *= 2;
                startTrial();
                break;
            }

            default:
                break;
        }
        return Status.RUNNING;
    }

    @Override
    protected void end(boolean interrupted) {
        wheel.drive(Double.NaN);
        wheel.target(0);
        if (interrupted && !report.isFinished()) {
            restore();
            report.fail("stopped before it finished; the gains are back to what they were");
        }
    }

    private void startTrial() {
        wheel.setGains(wheel.maxRpmGain(), wheel.frictionGain(), trialKp, wheel.toleranceRpm());
        settleRpm = 0.4 / 0.7 * target;
        wheel.target(settleRpm);
        begin(Phase.SETTLE);
    }

    private void begin(Phase next) {
        phase = next;
        phaseStart = clock.nowMillis();
        samples.clear();
    }

    private Status finish() {
        if (bestKp < 0) return fail("even the smallest kP wobbled: check the motor mount and encoder");
        double tolerance = Math.max(30, Math.ceil(3 * bestWobble * target / 10) * 10);
        wheel.setGains(wheel.maxRpmGain(), wheel.frictionGain(), bestKp / 2, tolerance);
        report.log("   using half of kP %s, for a safety margin", TuneReport.number(bestKp));
        report.log("Done. Paste this where it's created in Robot.java:");
        report.log("   %s", wheel.codeLine());
        report.finish();
        phase = Phase.DONE;
        return Status.SUCCESS;
    }

    private Status fail(String why) {
        restore();
        report.fail(why);
        phase = Phase.DONE;
        return Status.FAILURE;
    }

    private void restore() {
        wheel.setGains(original[0], original[1], original[2], original[3]);
    }

    private static double average(List<Double> values) {
        if (values.isEmpty()) return 0;
        double sum = 0;
        for (double value : values) sum += value;
        return sum / values.size();
    }

    private static double deviation(List<Double> values, double mean) {
        if (values.isEmpty()) return 0;
        double sum = 0;
        for (double value : values) sum += (value - mean) * (value - mean);
        return Math.sqrt(sum / values.size());
    }

    // ---- By hand: one scored spin-up ----

    static Node stepTest(Flywheel wheel, Clock clock, TuneReport report) {
        return new StepTest(wheel, clock, report);
    }

    private static final class StepTest extends Action {
        private final Flywheel wheel;
        private final Clock clock;
        private final TuneReport report;
        private long startedAt;
        private long atSpeedAt;
        private boolean stepped;
        private double target;
        private double peak;

        StepTest(Flywheel wheel, Clock clock, TuneReport report) {
            this.wheel = wheel;
            this.clock = clock;
            this.report = report;
        }

        @Override
        protected void start() {
            target = 0.7 * wheel.maxRpmGain();
            wheel.target(0.4 * wheel.maxRpmGain());
            startedAt = clock.nowMillis();
            stepped = false;
            atSpeedAt = -1;
            peak = 0;
        }

        @Override
        protected Status update() {
            long now = clock.nowMillis();
            if (!stepped) {
                if (now - startedAt < 1500) return Status.RUNNING;
                stepped = true;
                startedAt = now;
                wheel.target(target);
                return Status.RUNNING;
            }
            peak = Math.max(peak, wheel.rpm());
            if (atSpeedAt < 0 && wheel.atSpeed()) atSpeedAt = now;
            if (now - startedAt < 2000) return Status.RUNNING;
            report.log("Spin-up to %.0f rpm: %s, overshoot %.1f%%, now %.0f rpm", target,
                    atSpeedAt < 0 ? "never at speed" : "at speed in " + (atSpeedAt - startedAt) + " ms",
                    Math.max(0, peak - target) / target * 100, wheel.rpm());
            return Status.SUCCESS;
        }

        @Override
        protected void end(boolean interrupted) {
            wheel.target(0);
        }
    }
}
