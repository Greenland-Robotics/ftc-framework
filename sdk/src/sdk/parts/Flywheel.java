package sdk.parts;

import static behavior.Behaviors.instant;
import static util.MathUtil.clamp;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import behavior.runtime.Clock;
import sdk.hardware.Motor;
import sdk.tuning.Gain;
import sdk.tuning.TuneReport;
import sdk.tuning.Tunable;
import subsystem.Subsystem;

/**
 * A wheel that spins at a set speed to launch game pieces: ULTIMATE GOAL's rings, DECODE's
 * artifacts. It holds the speed itself (feed-forward plus a correction), so a shot doesn't slow
 * the next one down for long.
 * <pre>
 * // Robot.java: a goBILDA 6000 rpm motor has 28 ticks per turn
 * shooter = register(new Flywheel(hw.motor("shooter"), 28).maxRpm(6000));
 *
 * // Shoot one: spin while feeding, as soon as it is up to speed
 * race(robot.shooter.spin(4500),
 *      sequence(waitUntil(robot.shooter::atSpeed), robot.feeder.setTo(PUSH), robot.feeder.setTo(REST)))
 * </pre>
 * Power is {@code friction + target / maxRpm + kP × (target - rpm)}. The Tune OpMode measures
 * all three ({@link #autoTune}).
 */
public class Flywheel implements Subsystem, Tunable {
    private final Motor motor;
    private final double ticksPerRev;
    private final Clock clock;
    private double maxRpm = 6000;
    private double kS;
    private double kP = 0.0005;
    private double tolerance = 100;
    private double tuningPower = Double.NaN;
    private double targetRpm;
    private double rpm;
    private boolean atSpeed;

    public Flywheel(Motor motor, double ticksPerRev) {
        this(motor, ticksPerRev, Clock.system());
    }

    public Flywheel(Motor motor, double ticksPerRev, Clock clock) {
        this.motor = Objects.requireNonNull(motor, "motor");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (ticksPerRev <= 0) throw new IllegalArgumentException("ticksPerRev must be more than 0");
        this.ticksPerRev = ticksPerRev;
    }

    /** How fast it spins at full power (no load). Sets the feed-forward. */
    public Flywheel maxRpm(double maxRpm) {
        this.maxRpm = maxRpm;
        return this;
    }

    /** Power it takes just to get the wheel turning, added whenever it is spinning. */
    public Flywheel friction(double kS) {
        this.kS = Math.abs(kS);
        return this;
    }

    /** Extra power per rpm it is below (or above) the target. */
    public Flywheel gain(double kP) {
        this.kP = kP;
        return this;
    }

    /** How close to the target counts as {@link #atSpeed()}, in rpm. */
    public Flywheel tolerance(double rpm) {
        this.tolerance = rpm;
        return this;
    }

    // ---- Behaviors ----

    /** Spins at {@code rpm} until halted, then stops. */
    public Node spin(double rpm) {
        return new Spin(rpm, true);
    }

    /** Starts spinning at {@code rpm} and succeeds once it gets there; it keeps spinning until {@link #stop()}. */
    public Node spinUp(double rpm) {
        return new Spin(rpm, false);
    }

    public Node stop() {
        return instant(() -> setTarget(0));
    }

    // ---- Checks ----

    public double rpm() {
        return rpm;
    }

    public double targetRpm() {
        return targetRpm;
    }

    /**
     * Whether it is spinning at the target. Only a reading taken after the target was set counts,
     * so a wheel that was just told to stop (or change speed) isn't "at speed" from an old reading.
     */
    public boolean atSpeed() {
        return atSpeed;
    }

    @Override
    public void initPeriodic() {
        rpm = motor.velocity() * 60 / ticksPerRev;
    }

    @Override
    public void periodic() {
        rpm = motor.velocity() * 60 / ticksPerRev;
        atSpeed = targetRpm > 0 && Math.abs(rpm - targetRpm) <= tolerance;
        if (!Double.isNaN(tuningPower)) {
            motor.setPower(clamp(tuningPower, 0, 1));
            return;
        }
        if (targetRpm <= 0) {
            motor.setPower(0);
            return;
        }
        double power = kS + targetRpm / maxRpm + kP * (targetRpm - rpm);
        motor.setPower(clamp(power, 0, 1));   // never brakes backwards against the wheel
    }

    // ---- Tuning (see sdk/README.md, "Tuning") ----

    @Override
    public List<Gain> gains() {
        return Arrays.asList(
                new Gain("maxRpm", () -> maxRpm, value -> maxRpm = Math.max(1, value)),
                new Gain("friction", () -> kS, value -> kS = Math.abs(value)),
                new Gain("kP", () -> kP, value -> kP = value),
                new Gain("tolerance", () -> tolerance, value -> tolerance = Math.abs(value)));
    }

    /** Measures maxRpm and friction, then finds the largest kP that spins up without overshoot or wobble. */
    @Override
    public Node autoTune(TuneReport report) {
        return new FlywheelTuner(this, clock, report);
    }

    /** Spins from 40% to 70% of its top speed and scores the spin-up, then stops. */
    @Override
    public Node stepTest(TuneReport report) {
        return FlywheelTuner.stepTest(this, clock, report);
    }

    @Override
    public double measured() {
        return rpm;
    }

    @Override
    public double setpoint() {
        return targetRpm;
    }

    @Override
    public String codeLine() {
        return String.format(Locale.ROOT, ".maxRpm(%s).friction(%s).gain(%s).tolerance(%s)",
                TuneReport.number(maxRpm), TuneReport.number(kS), TuneReport.number(kP), TuneReport.number(tolerance));
    }

    // For the tuner, in this package only

    double maxRpmGain() {
        return maxRpm;
    }

    double frictionGain() {
        return kS;
    }

    double kP() {
        return kP;
    }

    double toleranceRpm() {
        return tolerance;
    }

    void setGains(double maxRpm, double kS, double kP, double tolerance) {
        this.maxRpm = Math.max(1, maxRpm);
        this.kS = Math.abs(kS);
        this.kP = kP;
        this.tolerance = Math.abs(tolerance);
    }

    /** Drives the motor at exactly {@code power}, ignoring the target; NaN goes back to normal. */
    void drive(double power) {
        tuningPower = power;
    }

    void target(double rpm) {
        setTarget(rpm);
    }

    private final class Spin extends Action {
        private final double rpm;
        private final boolean untilHalted;

        Spin(double rpm, boolean untilHalted) {
            this.rpm = rpm;
            this.untilHalted = untilHalted;
        }

        @Override
        protected void start() {
            setTarget(rpm);
        }

        @Override
        protected Status update() {
            if (untilHalted) return Status.RUNNING;
            return atSpeed() ? Status.SUCCESS : Status.RUNNING;
        }

        @Override
        protected void end(boolean interrupted) {
            if (untilHalted) setTarget(0);
        }
    }

    private void setTarget(double rpm) {
        double target = clamp(rpm, 0, maxRpm);
        if (target != targetRpm) atSpeed = false;
        targetRpm = target;
    }
}
