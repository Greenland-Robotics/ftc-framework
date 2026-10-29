package sdk.ftc;

import java.util.Objects;
import java.util.SortedSet;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.hardware.TouchSensor;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import sdk.hardware.Motor;
import sdk.hardware.PieceColor;
import sdk.hardware.Servo;

/**
 * Gets hardware from the robot's configuration, by the names set on the Driver Station
 * (Configure Robot), ready to hand to sdk parts. Used only in Robot.java:
 * <pre>
 * Hardware hw = new Hardware(hardwareMap);
 * lift  = register(new Lift(hw.motor("lift").reversed()).limits(0, 3000));
 * claw  = register(new ServoPart(hw.servo("claw")));
 * intake = register(new Roller(hw.motor("intake")).sensor(hw.distanceBelow("intakeSensor", 5)));
 * </pre>
 * A misspelled name fails at init with the names that do exist, instead of a crash mid-match.
 * Creating it also turns on bulk reads, so reading many encoders costs one trip to the hub.
 */
public final class Hardware {
    private final HardwareMap map;

    public Hardware(HardwareMap map) {
        this.map = Objects.requireNonNull(map, "hardwareMap");
        for (LynxModule hub : map.getAll(LynxModule.class)) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }
    }

    /** A motor with an encoder (or without: position and velocity then read 0). Brakes when stopped. */
    public FtcMotor motor(String name) {
        return new FtcMotor(find(DcMotorEx.class, name, "motor"));
    }

    /** A continuous rotation servo, used like a motor without an encoder (e.g. for a {@code Roller}). */
    public Motor crServo(String name) {
        CRServo servo = find(CRServo.class, name, "continuous rotation servo");
        return servo::setPower;
    }

    public FtcServo servo(String name) {
        return new FtcServo(find(com.qualcomm.robotcore.hardware.Servo.class, name, "servo"));
    }

    /** True while a touch sensor or limit switch is pressed. */
    public BooleanSupplier touch(String name) {
        TouchSensor sensor = find(TouchSensor.class, name, "touch sensor");
        return sensor::isPressed;
    }

    /**
     * True while a digital input reads low, which is how beam breaks and most limit switches
     * report "something's here". Use {@code touch} for a REV touch sensor.
     */
    public BooleanSupplier digitalLow(String name) {
        DigitalChannel channel = find(DigitalChannel.class, name, "digital device");
        channel.setMode(DigitalChannel.Mode.INPUT);
        return () -> !channel.getState();
    }

    /** True while a distance sensor (or a color sensor's distance) sees something closer than {@code cm}. */
    public BooleanSupplier distanceBelow(String name, double cm) {
        DistanceSensor sensor = find(DistanceSensor.class, name, "distance sensor");
        return () -> sensor.getDistance(DistanceUnit.CM) < cm;
    }

    /** What color a color sensor sees, as a {@link PieceColor}. */
    public Supplier<PieceColor> color(String name) {
        NormalizedColorSensor sensor = find(NormalizedColorSensor.class, name, "color sensor");
        return () -> {
            NormalizedRGBA rgba = sensor.getNormalizedColors();
            return PieceColor.fromRgb(rgba.red, rgba.green, rgba.blue);
        };
    }

    /**
     * Any device by its FTC class, for hardware the sdk has no helper for (write your own
     * subsystem around it). Still fails with a readable message if the name is wrong.
     */
    public <T> T device(Class<T> type, String name) {
        return find(type, name, type.getSimpleName());
    }

    /** The FTC hardware map itself, for when nothing else will do (e.g. Pedro's follower). */
    public HardwareMap map() {
        return map;
    }

    private <T> T find(Class<T> type, String name, String what) {
        T device = map.tryGet(type, name);
        if (device != null) return device;

        String names = "none";
        if (HardwareDevice.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            SortedSet<String> configured = map.getAllNames((Class<? extends HardwareDevice>) type);
            if (!configured.isEmpty()) names = String.join(", ", configured);
        }
        throw new IllegalArgumentException("No " + what + " named \"" + name + "\" in the robot configuration. "
                + "Configured " + what + "s: " + names + ". Check the name in Configure Robot on the Driver Station.");
    }
}
