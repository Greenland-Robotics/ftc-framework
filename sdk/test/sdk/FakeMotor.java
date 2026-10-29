package sdk;

import sdk.hardware.Motor;

/**
 * A pretend motor with just enough physics for tests: it moves at {@code power × fullSpeed}
 * ticks per second, minus a constant pull (gravity) it needs power to hold against, and it stops
 * at a hard end if given one.
 */
public final class FakeMotor implements Motor {
    public double power;
    public double position;
    public double velocity;
    private final double fullSpeed;
    private double pull;
    private double hardStop = Double.NEGATIVE_INFINITY;

    public FakeMotor(double fullSpeed) {
        this.fullSpeed = fullSpeed;
    }

    /** Power needed just to hold still, e.g. a lift's weight. */
    public FakeMotor pull(double power) {
        this.pull = power;
        return this;
    }

    /** It can't go below {@code position}, like a lift sitting on its frame. */
    public FakeMotor hardStop(double position) {
        this.hardStop = position;
        return this;
    }

    /** Moves for {@code millis}. */
    public void step(long millis) {
        velocity = (power - pull) * fullSpeed;
        position += velocity * millis / 1000.0;
        if (position < hardStop) {
            position = hardStop;
            velocity = 0;
        }
    }

    @Override
    public void setPower(double power) {
        this.power = power;
    }

    @Override
    public double position() {
        return position;
    }

    @Override
    public double velocity() {
        return velocity;
    }

    @Override
    public void resetPosition() {
        hardStop -= position;
        position = 0;
    }
}
