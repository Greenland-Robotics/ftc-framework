package sdk.hardware;

import java.util.function.DoubleUnaryOperator;

/**
 * A pretend motor with enough physics to be worth tuning against: it takes time to speed up,
 * gravity pulls on what it lifts, friction holds it still at low power, and hard stops end its
 * travel. The tuning tests and the laptop tuning demo use it; your own laptop experiments can
 * too. Call {@link #step} once per loop with the time that passed.
 * <pre>
 * SimMotor lift = new SimMotor(2800).lag(0.12).gravity(0.12).friction(0.05).travel(0, 3000);
 * </pre>
 */
public final class SimMotor implements Motor {
    private final double freeSpeed;
    private double lagSeconds = 0.1;
    private DoubleUnaryOperator gravity = position -> 0;
    private double friction;
    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;

    private double power;
    private double position;
    private double velocity;
    private double zero;

    /** {@code freeSpeed}: ticks per second at full power with nothing pulling on it. */
    public SimMotor(double freeSpeed) {
        this.freeSpeed = freeSpeed;
    }

    /** How long it takes to get most of the way to a new speed, in seconds: its inertia. */
    public SimMotor lag(double seconds) {
        this.lagSeconds = Math.max(1e-3, seconds);
        return this;
    }

    /** The power gravity cancels out, the same everywhere (a lift). */
    public SimMotor gravity(double power) {
        return gravity(position -> power);
    }

    /** The power gravity cancels out at each position (an arm: strongest when level). */
    public SimMotor gravity(DoubleUnaryOperator powerAt) {
        this.gravity = powerAt;
        return this;
    }

    /** Power lost to friction; below it, the motor doesn't move at all. */
    public SimMotor friction(double power) {
        this.friction = Math.abs(power);
        return this;
    }

    /** Hard stops at the ends of its travel, in ticks. */
    public SimMotor travel(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    /** Starts it at {@code ticks} (the encoder reads that, too). */
    public SimMotor at(double ticks) {
        this.position = ticks;
        return this;
    }

    /** Moves the world on by {@code seconds}. */
    public void step(double seconds) {
        double push = power - gravity.applyAsDouble(position);
        double drive;
        if (Math.abs(velocity) < 1e-6 && Math.abs(push) <= friction) {
            drive = 0;   // Stuck: friction holds it
        } else {
            double resisting = velocity != 0 ? Math.signum(velocity) : Math.signum(push);
            drive = push - friction * resisting;
        }
        double targetSpeed = drive * freeSpeed;
        velocity += (targetSpeed - velocity) * Math.min(1, seconds / lagSeconds);
        if (Math.abs(velocity) < 1e-3 && Math.abs(push) <= friction) velocity = 0;
        position += velocity * seconds;
        if (position <= min) {
            position = min;
            velocity = Math.max(0, velocity);
        } else if (position >= max) {
            position = max;
            velocity = Math.min(0, velocity);
        }
    }

    @Override
    public void setPower(double power) {
        this.power = Math.max(-1, Math.min(1, power));
    }

    public double power() {
        return power;
    }

    @Override
    public double position() {
        return position - zero;
    }

    @Override
    public double velocity() {
        return velocity;
    }

    @Override
    public void resetPosition() {
        zero = position;
    }
}
