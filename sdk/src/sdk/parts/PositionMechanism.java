package sdk.parts;

import static behavior.Behaviors.instant;
import static util.MathUtil.clamp;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

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
 * A motor with an encoder that moves to positions and holds them: what {@link Lift} and
 * {@link Arm} share. Use one of those; this class holds their logic.
 *
 * <p>The part always has a <b>target</b>, and {@link #periodic()} drives the motor toward it
 * every loop. Behaviors only change the target, so:
 * <ul>
 *   <li>halting a behavior (a released button, a timeout) leaves the part holding where it is,
 *       instead of dropping;</li>
 *   <li>two behaviors can't fight over the motor: the last target set wins;</li>
 *   <li>every target is clamped to {@link #limits}, so nothing can drive it past its ends.</li>
 * </ul>
 * The power it uses is {@code kP × error + kD × (how fast the error changes) + gravity + friction}.
 * Don't guess these: the Tune OpMode finds them ({@link #autoTune}).
 *
 * @param <T> the part's own class, so the chained setters return it
 */
public abstract class PositionMechanism<T extends PositionMechanism<T>> implements Subsystem, Tunable {
    private enum Mode { HOLD, HOMING, OFF, TUNING }

    protected final Motor motor;
    private final Clock clock;

    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;
    private double kP;
    private double kD;
    private double kG;
    private double kS;
    private double tolerance;
    private double maxPower = 1;
    private double manualSpeed;
    private double homePower = -0.3;
    private BooleanSupplier limitSwitch;

    private Mode mode = Mode.HOLD;
    private double target = Double.NaN;   // NaN until the first reading: then it holds where it is
    private double position;
    private double lastError;
    private long lastMillis = -1;
    private double tuningPower;

    protected PositionMechanism(Motor motor, Clock clock, double kP, double tolerance, double manualSpeed) {
        this.motor = Objects.requireNonNull(motor, "motor");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.kP = kP;
        this.tolerance = tolerance;
        this.manualSpeed = manualSpeed;
    }

    // ---- Setup: chain these where the part is created, in Robot.java ----

    /** The lowest and highest positions it may ever be sent to. Set these before anything else. */
    public T limits(double min, double max) {
        if (min > max) throw new IllegalArgumentException("limits: min " + min + " is above max " + max);
        this.min = min;
        this.max = max;
        return self();
    }

    /** How hard it pushes toward the target: power per unit of error ({@code kP}) and damping ({@code kD}). */
    public T gains(double kP, double kD) {
        this.kP = kP;
        this.kD = kD;
        return self();
    }

    /** Power that holds it still against gravity (for an Arm: when it is level). Find it with the Tune OpMode. */
    public T gravity(double kG) {
        this.kG = kG;
        return self();
    }

    /**
     * Extra power to get past friction, pushed toward the target while it is off by more than the
     * tolerance. Without it, a sticky slide can stop just short of its target.
     */
    public T friction(double kS) {
        this.kS = Math.abs(kS);
        return self();
    }

    /** How close counts as "there" for {@link #goTo} and {@link #atTarget()}. */
    public T tolerance(double tolerance) {
        this.tolerance = tolerance;
        return self();
    }

    /** Caps the motor power (0..1), e.g. while testing a new mechanism. */
    public T maxPower(double maxPower) {
        this.maxPower = clamp(maxPower, 0, 1);
        return self();
    }

    /** How fast {@link #manual} moves it at full stick, in position units per second. */
    public T manualSpeed(double unitsPerSecond) {
        this.manualSpeed = unitsPerSecond;
        return self();
    }

    /** A switch pressed at the home end. Without one, {@link #home()} stops when the motor stalls. */
    public T homeSwitch(BooleanSupplier pressed) {
        this.limitSwitch = pressed;
        return self();
    }

    /** The power {@link #home()} drives with: negative goes toward the home end. */
    public T homePower(double power) {
        this.homePower = power;
        return self();
    }

    // ---- Behaviors ----

    /** Moves to {@code position} (clamped to the limits) and succeeds once it is there. It keeps holding afterwards. */
    public Node goTo(double position) {
        return new GoTo(position);
    }

    /**
     * Moves while {@code input} (-1..1, e.g. a stick) is pushed, and holds where it is when let go.
     * Runs until halted; bind it with {@code whileHeld} or run it for the whole TeleOp.
     */
    public Node manual(DoubleSupplier input) {
        return new Manual(input);
    }

    /**
     * Drives slowly to the home end until the {@link #homeSwitch} is pressed (or the motor stalls
     * against the end), then makes that position the zero. Run it at the start of an Auto or
     * TeleOp if the part may not be at home when the robot powers on.
     */
    public Node home() {
        return new Home();
    }

    /** Stops holding and lets the motor go limp, until the next {@link #goTo}. */
    public Node off() {
        return instant(() -> mode = Mode.OFF);
    }

    // ---- Checks ----

    public double position() {
        return position;
    }

    public double target() {
        return target;
    }

    public boolean atTarget() {
        return mode == Mode.HOLD && Math.abs(target - position) <= tolerance;
    }

    // ---- Every loop ----

    /** During init the part only reads its position: motors must not move before start. */
    @Override
    public void initPeriodic() {
        read();
    }

    @Override
    public void periodic() {
        long now = clock.nowMillis();
        double seconds = lastMillis < 0 ? 0 : (now - lastMillis) / 1000.0;
        lastMillis = now;
        read();

        switch (mode) {
            case HOMING:
                motor.setPower(homePower);
                break;
            case OFF:
                motor.setPower(0);
                break;
            case TUNING:
                motor.setPower(clamp(tuningPower, -1, 1));
                break;
            default:
                double error = target - position;
                double derivative = seconds > 0 ? (error - lastError) / seconds : 0;
                lastError = error;
                double push = Math.abs(error) > tolerance ? kS * Math.signum(error) : 0;
                double power = kP * error + kD * derivative + holdingPower(position) + push;
                motor.setPower(clamp(power, -maxPower, maxPower));
        }
    }

    // ---- Tuning (see sdk/README.md, "Tuning") ----

    @Override
    public List<Gain> gains() {
        return Arrays.asList(
                new Gain("kP", () -> kP, value -> kP = value),
                new Gain("kD", () -> kD, value -> kD = value),
                new Gain("gravity", () -> kG, value -> kG = value),
                new Gain("friction", () -> kS, value -> kS = Math.abs(value)));
    }

    /**
     * Finds gravity, friction, kP and kD by itself, moving only within {@link #limits} (which it
     * needs), then checks them with test moves and refines them. See {@link PositionTuner}.
     */
    @Override
    public Node autoTune(TuneReport report) {
        return new PositionTuner(this, clock, report);
    }

    /** Moves between 30% and 70% of its range and back, and scores both moves. */
    @Override
    public Node stepTest(TuneReport report) {
        return PositionTuner.stepTest(this, clock, report);
    }

    @Override
    public double measured() {
        return position;
    }

    @Override
    public double setpoint() {
        return mode == Mode.TUNING ? Double.NaN : target;
    }

    @Override
    public String codeLine() {
        return String.format(Locale.ROOT, ".gains(%s, %s).gravity(%s).friction(%s)",
                TuneReport.number(kP), TuneReport.number(kD), TuneReport.number(kG), TuneReport.number(kS));
    }

    // For the tuner, in this package only

    double min() {
        return min;
    }

    double max() {
        return max;
    }

    double tolerance() {
        return tolerance;
    }

    double gravityGain() {
        return kG;
    }

    double frictionGain() {
        return kS;
    }

    void setGains(double kP, double kD, double kG, double kS) {
        this.kP = kP;
        this.kD = kD;
        this.kG = kG;
        this.kS = Math.abs(kS);
    }

    double kP() {
        return kP;
    }

    double kD() {
        return kD;
    }

    /** Drives the motor at exactly {@code power} from the next loop on, ignoring the target. */
    void drive(double power) {
        mode = Mode.TUNING;
        tuningPower = power;
    }

    /** Goes back to holding a target (where it is now, if {@code value} is NaN). */
    void hold(double value) {
        setTarget(Double.isNaN(value) ? position : value);
        lastError = target - position;
    }

    /** How hard gravity pulls at {@code position}, from 0 to 1: always 1 for a Lift, {@code cos(angle)} for an Arm. */
    abstract double gravityShape(double position);

    // ---- For Lift and Arm ----

    /** The motor's encoder reading, in this part's units (ticks for a Lift, degrees for an Arm). */
    protected abstract double readPosition();

    /** Power that holds it still against gravity at {@code position}, from {@link #gravity}. */
    protected abstract double holdingPower(double position);

    /** The gravity gain, for {@link #holdingPower}. */
    protected double gravityPower() {
        return kG;
    }

    /** The position {@link #home()} makes zero, in this part's units. */
    protected abstract double homePosition();

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }

    private void read() {
        position = readPosition();
        if (Double.isNaN(target)) target = clamp(position, min, max);
    }

    private void setTarget(double value) {
        mode = Mode.HOLD;
        target = clamp(value, min, max);
    }

    private final class GoTo extends Action {
        private final double position;

        GoTo(double position) {
            this.position = position;
        }

        @Override
        protected void start() {
            setTarget(position);
        }

        @Override
        protected Status update() {
            return atTarget() ? Status.SUCCESS : Status.RUNNING;
        }
    }

    private final class Manual extends Action {
        private final DoubleSupplier input;
        private long last;

        Manual(DoubleSupplier input) {
            this.input = input;
        }

        @Override
        protected void start() {
            last = clock.nowMillis();
            setTarget(position);
        }

        @Override
        protected Status update() {
            long now = clock.nowMillis();
            double seconds = (now - last) / 1000.0;
            last = now;
            double push = clamp(input.getAsDouble(), -1, 1);
            if (Math.abs(push) > 0.05) {
                // Keep the target just ahead of where it really is, so letting go stops it right away
                double lead = manualSpeed * 0.25;
                double next = clamp(target + push * manualSpeed * seconds, position - lead, position + lead);
                setTarget(next);
            }
            return Status.RUNNING;
        }
    }

    private final class Home extends Action {
        private long startedAt;
        private long lastMoved;
        private double lastMovedFrom;

        @Override
        protected void start() {
            startedAt = lastMoved = clock.nowMillis();
            lastMovedFrom = position;
            mode = Mode.HOMING;
        }

        @Override
        protected Status update() {
            long now = clock.nowMillis();
            if (Math.abs(position - lastMovedFrom) > tolerance / 2) {
                lastMovedFrom = position;
                lastMoved = now;
            }
            boolean pressed = limitSwitch != null && limitSwitch.getAsBoolean();
            boolean stalled = limitSwitch == null && now - startedAt > 300 && now - lastMoved > 250;
            if (!pressed && !stalled) return Status.RUNNING;

            motor.resetPosition();
            position = homePosition();
            setTarget(position);
            return Status.SUCCESS;
        }

        @Override
        protected void end(boolean interrupted) {
            if (interrupted) setTarget(position);
        }
    }
}
