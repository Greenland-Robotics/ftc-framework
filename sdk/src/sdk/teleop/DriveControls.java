package sdk.teleop;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import behavior.input.EdgeDetector;

/**
 * How the driver's sticks become drive commands. The default is what most teams want:
 * <ul>
 *   <li>left stick moves (up = forward), right stick turns;</li>
 *   <li>field-centric: up on the stick is always away from the driver, whichever way the robot faces;</li>
 *   <li>hold the right bumper to drive at 40% for lining up;</li>
 *   <li>press back (share) to reset the heading, with the robot facing away from the driver.</li>
 * </ul>
 * TeleOpBase makes one for gamepad 1. Change it by overriding {@code driveControls()}:
 * <pre>
 * protected DriveControls driveControls() {
 *     return new DriveControls(driver).robotCentric().slowWhileHeld(driver.leftBumper(), 0.3);
 * }
 * </pre>
 * Commands follow FTC drive conventions: forward, strafe (positive = left), turn
 * (positive = counter-clockwise, to the left), each -1..1.
 */
public class DriveControls {
    private DoubleSupplier forward;
    private DoubleSupplier left;
    private DoubleSupplier turnLeft;
    private BooleanSupplier slow;
    private double slowScale = 0.4;
    private double turnScale = 1;
    private boolean fieldCentric = true;
    private EdgeDetector resetHeading;

    public DriveControls(Controller driver) {
        Objects.requireNonNull(driver, "driver");
        DoubleSupplier leftX = driver.leftStickX();
        DoubleSupplier rightX = driver.rightStickX();
        forward = driver.leftStickY();
        left = () -> -leftX.getAsDouble();       // Stick right is positive; strafe left is positive
        turnLeft = () -> -rightX.getAsDouble();  // Same for turning
        slow = driver.rightBumper();
        resetHeading = new EdgeDetector(driver.back());
    }

    /** Up on the stick drives the robot's own forward, whichever way it faces. */
    public DriveControls robotCentric() {
        fieldCentric = false;
        return this;
    }

    /** Drives at {@code scale} (0..1) of full speed while {@code button} is held. */
    public DriveControls slowWhileHeld(BooleanSupplier button, double scale) {
        this.slow = Objects.requireNonNull(button, "button");
        this.slowScale = scale;
        return this;
    }

    /** Turns at {@code scale} of full speed, for robots that spin too fast to control. */
    public DriveControls turnScale(double scale) {
        this.turnScale = scale;
        return this;
    }

    /** Which button resets the heading. */
    public DriveControls resetHeadingOn(BooleanSupplier button) {
        this.resetHeading = new EdgeDetector(Objects.requireNonNull(button, "button"));
        return this;
    }

    // ---- Read every loop ----

    public double forward() {
        return forward.getAsDouble() * speed();
    }

    public double strafe() {
        return left.getAsDouble() * speed();
    }

    public double turn() {
        return turnLeft.getAsDouble() * speed() * turnScale;
    }

    public boolean isFieldCentric() {
        return fieldCentric;
    }

    /** True once per press of the reset-heading button; call once per loop. */
    public boolean resetHeadingPressed() {
        return resetHeading.update() == EdgeDetector.Edge.PRESSED;
    }

    private double speed() {
        return slow.getAsBoolean() ? slowScale : 1;
    }
}
