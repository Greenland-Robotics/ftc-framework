package hive;

import static behavior.Behaviors.sequence;
import static util.MathUtil.clamp;

import behavior.Node;
import behavior.runtime.Clock;
import sdk.hardware.Motor;
import sdk.hardware.Servo;
import sdk.parts.Flywheel;
import sdk.parts.ServoPart;
import subsystem.Subsystem;

/**
 * A flywheel that launches balls straight ahead, and a feeder that pushes the next held ball into
 * it. At full speed a ball lands {@value Field#SHOT_RANGE} inches in front of the robot.
 *
 * Both are sdk parts: a {@link Flywheel} and a {@link ServoPart}. The pretend motor and servo
 * below are the only simulator-specific code; on the robot they are {@code hw.motor("shooter")}
 * and {@code hw.servo("feeder")}.
 */
public final class Shooter implements Subsystem {
    /** A goBILDA 6000 rpm motor: 28 encoder ticks per turn. */
    static final double MAX_RPM = 6000;
    private static final double TICKS_PER_REV = 28;
    private static final double REST = 0;
    private static final double PUSH = 1;

    private final Flywheel flywheel;
    private final ServoPart feeder;

    Shooter(Body body, Clock clock) {
        Motor motor = new Motor() {
            @Override
            public void setPower(double power) {
                body.flywheelPower = clamp(power, 0, 1);
            }

            @Override
            public double velocity() {
                return body.flywheelSpeed * MAX_RPM * TICKS_PER_REV / 60;
            }
        };
        // The feeder launches a ball each time it swings forward
        Servo kicker = new Servo() {
            private double position = REST;

            @Override
            public void setPosition(double next) {
                if (next > 0.5 && position <= 0.5) body.launch = true;
                position = next;
            }
        };
        flywheel = new Flywheel(motor, TICKS_PER_REV).maxRpm(MAX_RPM).tolerance(MAX_RPM * 0.03);
        feeder = new ServoPart(kicker, clock).moveTime(75).startAt(REST);
    }

    public boolean atSpeed() {
        return flywheel.atSpeed();
    }

    /** Spins the flywheel up and keeps it spinning until halted. */
    public Node spin() {
        return flywheel.spin(MAX_RPM).named("Spin the flywheel");
    }

    /** Pushes the next ball into the flywheel, and pulls the feeder back. */
    public Node feed() {
        return sequence(feeder.setTo(PUSH), feeder.setTo(REST)).named("Feed a ball");
    }

    @Override
    public void periodic() {
        flywheel.periodic();
    }
}
