package sdk.ftc;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import sdk.hardware.Motor;

/**
 * An FTC motor as an sdk {@link Motor}. Made by {@code Hardware.motor(name)}. Parts run their
 * own control, so the motor runs without the SDK's built-in modes, and the encoder is never
 * reset here: positions carry over from the Auto into the TeleOp.
 */
public final class FtcMotor implements Motor {
    private final DcMotorEx motor;
    private double lastPower = Double.NaN;

    FtcMotor(DcMotorEx motor) {
        this.motor = motor;
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    /** Swaps which way is positive, for a motor mounted the other way round. */
    public FtcMotor reversed() {
        motor.setDirection(motor.getDirection() == DcMotorSimple.Direction.FORWARD
                ? DcMotorSimple.Direction.REVERSE : DcMotorSimple.Direction.FORWARD);
        return this;
    }

    /** Coasts instead of braking when the power is 0, e.g. for a flywheel. */
    public FtcMotor coast() {
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        return this;
    }

    @Override
    public void setPower(double power) {
        // Each write is a trip to the hub; skip ones that wouldn't change anything
        if (power == lastPower || (power != 0 && Math.abs(power - lastPower) < 0.005)) return;
        lastPower = power;
        motor.setPower(power);
    }

    @Override
    public double position() {
        return motor.getCurrentPosition();
    }

    @Override
    public double velocity() {
        return motor.getVelocity();
    }

    @Override
    public void resetPosition() {
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        lastPower = Double.NaN;
    }
}
