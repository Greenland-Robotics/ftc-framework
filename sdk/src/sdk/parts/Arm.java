package sdk.parts;

import behavior.runtime.Clock;
import sdk.hardware.Motor;

/**
 * A motor that swings something around a pivot, measured in degrees: an arm, a pivot, a wobble
 * goal grabber, a turret. 0° is horizontal (pointing out), 90° straight up. Gravity pulls hardest
 * when it is horizontal, so it pushes back by {@code gravity × cos(angle)}.
 * <pre>
 * // Robot.java: 1425 ticks per output turn, resting at -40° when the robot powers on
 * arm = register(new Arm(hw.motor("arm"), 1425 / 360.0).zeroAt(-40).limits(-40, 110).gravity(0.12));
 *
 * robot.arm.goTo(45)
 * </pre>
 */
public class Arm extends PositionMechanism<Arm> {
    private final double ticksPerDegree;
    private double zeroAngle;

    public Arm(Motor motor, double ticksPerDegree) {
        this(motor, ticksPerDegree, Clock.system());
    }

    public Arm(Motor motor, double ticksPerDegree, Clock clock) {
        super(motor, clock, 0.03, 2, 90);
        if (ticksPerDegree == 0) throw new IllegalArgumentException("ticksPerDegree can't be 0");
        this.ticksPerDegree = ticksPerDegree;
    }

    /** The angle the arm is at when the encoder reads 0: where it rests when the robot powers on. */
    public Arm zeroAt(double degrees) {
        this.zeroAngle = degrees;
        return this;
    }

    /** The arm's angle in degrees: the same as {@link #position()}. */
    public double angle() {
        return position();
    }

    @Override
    protected double readPosition() {
        return motor.position() / ticksPerDegree + zeroAngle;
    }

    @Override
    protected double holdingPower(double angle) {
        return gravityPower() * Math.cos(Math.toRadians(angle));
    }

    @Override
    double gravityShape(double angle) {
        return Math.cos(Math.toRadians(angle));
    }

    @Override
    protected double homePosition() {
        return zeroAngle;
    }
}
