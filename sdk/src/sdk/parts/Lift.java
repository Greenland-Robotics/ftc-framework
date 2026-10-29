package sdk.parts;

import behavior.runtime.Clock;
import sdk.hardware.Motor;

/**
 * Anything a motor moves up and down or in and out, measured in encoder ticks: a lift, a linear
 * slide, an extendo, a hang winch. Holds its position against gravity; see
 * {@link PositionMechanism} for how targets and limits keep it safe.
 * <pre>
 * // Robot.java
 * lift = register(new Lift(hw.motor("lift")).limits(0, 3100).gravity(0.08));
 *
 * // An Auto or TeleOp
 * robot.lift.goTo(HIGH_BASKET)                          // succeeds once it's there
 * controls.whileHeld(operator.leftBumper(), robot.lift.manual(operator.rightStickY()));
 * </pre>
 * Starts at 0 when the robot powers on (or after {@link #home()}): keep it down at power-on.
 */
public class Lift extends PositionMechanism<Lift> {
    public Lift(Motor motor) {
        this(motor, Clock.system());
    }

    public Lift(Motor motor, Clock clock) {
        super(motor, clock, 0.005, 20, 1500);
        limits(0, Double.POSITIVE_INFINITY);
    }

    @Override
    protected double readPosition() {
        return motor.position();
    }

    @Override
    protected double holdingPower(double position) {
        return gravityPower();
    }

    @Override
    double gravityShape(double position) {
        return 1;
    }

    @Override
    protected double homePosition() {
        return 0;
    }
}
