package sdk.ftc;

import sdk.hardware.Servo;

/** An FTC servo as an sdk {@link Servo}. Made by {@code Hardware.servo(name)}. */
public final class FtcServo implements Servo {
    private final com.qualcomm.robotcore.hardware.Servo servo;

    FtcServo(com.qualcomm.robotcore.hardware.Servo servo) {
        this.servo = servo;
    }

    /** Swaps the ends, so 0 and 1 trade places. */
    public FtcServo reversed() {
        servo.setDirection(servo.getDirection() == com.qualcomm.robotcore.hardware.Servo.Direction.FORWARD
                ? com.qualcomm.robotcore.hardware.Servo.Direction.REVERSE
                : com.qualcomm.robotcore.hardware.Servo.Direction.FORWARD);
        return this;
    }

    @Override
    public void setPosition(double position) {
        servo.setPosition(position);
    }
}
