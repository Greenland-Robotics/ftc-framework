package sdk.parts;

import static behavior.Behaviors.race;
import static behavior.Behaviors.waitUntil;
import static util.MathUtil.clamp;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import behavior.leaf.Delay;
import behavior.runtime.Clock;
import sdk.hardware.Motor;
import subsystem.Subsystem;

/**
 * A motor (or CR servo) that just spins: an intake, a transfer, a conveyor, a carousel. An
 * optional sensor tells it when a game piece is inside.
 * <pre>
 * // Robot.java
 * intake = register(new Roller(hw.motor("intake")).power(0.8).sensor(hw.distanceBelow("intakeSensor", 5)));
 *
 * robot.intake.collect()                 // spin in until the sensor sees a piece
 * robot.intake.eject(500)                // spit it out for half a second
 * robot.carousel.runFor(2500)            // FREIGHT FRENZY's duck carousel
 * </pre>
 * Every behavior stops the motor when it ends or is halted.
 */
public class Roller implements Subsystem {
    private final Motor motor;
    private final Clock clock;
    private double power = 1;
    private BooleanSupplier sensor;
    private boolean hasPiece;

    public Roller(Motor motor) {
        this(motor, Clock.system());
    }

    public Roller(Motor motor, Clock clock) {
        this.motor = Objects.requireNonNull(motor, "motor");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** How fast it spins in, 0..1 (eject and reverse use the same speed backwards). */
    public Roller power(double power) {
        this.power = clamp(Math.abs(power), 0, 1);
        return this;
    }

    /** Something that is true while a game piece is inside, e.g. {@code hw.distanceBelow("intakeSensor", 5)}. */
    public Roller sensor(BooleanSupplier hasPiece) {
        this.sensor = hasPiece;
        return this;
    }

    // ---- Behaviors ----

    /** Spins in until halted (e.g. bind it with {@code whileHeld}). */
    public Node run() {
        return new Spin(power);
    }

    /** Spins out until halted. */
    public Node reverse() {
        return new Spin(-power);
    }

    /** Spins in for {@code millis}, then stops and succeeds. */
    public Node runFor(long millis) {
        return race(run(), new Delay(millis, clock));
    }

    /** Spins in until {@code done} is true, then stops and succeeds. */
    public Node runUntil(BooleanSupplier done) {
        return race(run(), waitUntil(done));
    }

    /** Spins in until the sensor sees a piece. Add {@code .withTimeout(ms)} so it can give up. */
    public Node collect() {
        if (sensor == null) {
            throw new IllegalStateException("collect() needs a sensor: add .sensor(...) where this Roller is created");
        }
        return runUntil(this::hasPiece);
    }

    /** Spins out for {@code millis}, then stops and succeeds. */
    public Node eject(long millis) {
        return race(reverse(), new Delay(millis, clock));
    }

    // ---- Checks ----

    /** Whether the sensor sees a piece (always false without a sensor). */
    public boolean hasPiece() {
        return hasPiece;
    }

    @Override
    public void initPeriodic() {
        read();
    }

    @Override
    public void periodic() {
        read();
    }

    private void read() {
        hasPiece = sensor != null && sensor.getAsBoolean();
    }

    private final class Spin extends Action {
        private final double power;

        Spin(double power) {
            this.power = power;
        }

        @Override
        protected void start() {
            motor.setPower(power);
        }

        @Override
        protected Status update() {
            return Status.RUNNING;   // until a parent halts it
        }

        @Override
        protected void end(boolean interrupted) {
            motor.setPower(0);
        }
    }
}
