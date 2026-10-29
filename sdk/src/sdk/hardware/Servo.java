package sdk.hardware;

/** A position servo: 0..1 across its range. {@code ftc.Hardware} makes one from the hardware map. */
public interface Servo {
    void setPosition(double position);
}
