package robot.subsystems;

import static behavior.Behaviors.delay;
import static behavior.Behaviors.race;
import static behavior.Behaviors.waitUntil;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import subsystem.Subsystem;

/**
 * Example subsystem: a roller intake with a distance sensor that sees when a sample is inside.
 * It owns its hardware; everything else uses its behaviors and {@link #hasSample()}.
 * Register it in Robot.java: {@code intake = register(new Intake(hardwareMap));}
 */
public class Intake implements Subsystem {
    private static final double POWER = 0.8;
    private static final double SAMPLE_DISTANCE_CM = 5;

    private final DcMotor motor;
    private final DistanceSensor sensor;
    private double distanceCm = Double.MAX_VALUE;

    public Intake(HardwareMap hardwareMap) {
        motor = hardwareMap.get(DcMotor.class, "intake");
        sensor = hardwareMap.get(DistanceSensor.class, "intakeSensor");
    }

    @Override
    public void periodic() {
        // Read the sensor once per loop; everything below uses this value
        distanceCm = sensor.getDistance(DistanceUnit.CM);
    }

    public boolean hasSample() {
        return distanceCm < SAMPLE_DISTANCE_CM;
    }

    /** Spins the rollers in until halted (e.g. by releasing a button). */
    public Node run() {
        return new Spin(POWER);
    }

    /** Spins in until a sample is inside, then stops. */
    public Node collect() {
        return race(new Spin(POWER), waitUntil(this::hasSample));
    }

    /** Spits the sample out for half a second. */
    public Node eject() {
        return race(new Spin(-POWER), delay(500));
    }

    /** A custom Action: runs the motor while active and always stops it at the end. */
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
            return Status.RUNNING; // until a parent halts it
        }

        @Override
        protected void end(boolean interrupted) {
            motor.setPower(0);
        }
    }
}
