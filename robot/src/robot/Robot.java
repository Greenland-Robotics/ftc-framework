package robot;

import java.util.ArrayList;
import java.util.List;

import com.qualcomm.robotcore.hardware.HardwareMap;

import pedro.Constants;
import pedro.Drive;
import sdk.auto.FieldSymmetry;
import sdk.ftc.Hardware;
import sdk.ftc.Tuning;
import subsystem.Subsystem;

/**
 * Builds the robot from its subsystems. This is the only place that turns hardware into
 * subsystems: OpModes use {@code robot.drive}, {@code robot.lift}... and never the hardware map.
 *
 * <p>To add a mechanism: add a field below and create it with {@code register(...)} in the
 * constructor so it gets its {@code periodic()} call. Most mechanisms are an sdk part
 * ({@code Lift}, {@code Arm}, {@code Roller}, {@code ServoPart}, {@code Flywheel}); see
 * sdk/README.md. Anything else is your own subsystem in {@code robot/subsystems/}.
 */
public class Robot {
    /** How the red side of this season's field matches the blue side, for AutoBase's {@code pose(...)}. */
    public static final FieldSymmetry SYMMETRY = FieldSymmetry.MIRROR;

    public final Drive drive;
    // public final Lift lift;
    // public final ServoPart claw;
    // public final Roller intake;

    private final List<Subsystem> subsystems = new ArrayList<>();

    public Robot(HardwareMap hardwareMap) {
        Hardware hw = new Hardware(hardwareMap);
        drive = register(new Drive(Constants.createFollower(hardwareMap)));
        // lift = register(new Lift(hw.motor("lift")).limits(0, 3000).gravity(0.08));
        // claw = register(new ServoPart(hw.servo("claw")).moveTime(250));
        // intake = register(new Roller(hw.motor("intake")).sensor(hw.distanceBelow("intakeSensor", 5)));

        // Gains saved by the Tune OpMode replace the numbers above
        Tuning.applySaved(this);
    }

    /** Called once per loop during init by the OpMode base classes: sensors only, nothing moves. */
    public void initPeriodic() {
        for (Subsystem subsystem : subsystems) {
            subsystem.initPeriodic();
        }
    }

    /** Called once per loop after start by the OpMode base classes, before behaviors tick. */
    public void periodic() {
        for (Subsystem subsystem : subsystems) {
            subsystem.periodic();
        }
    }

    private <T extends Subsystem> T register(T subsystem) {
        subsystems.add(subsystem);
        return subsystem;
    }
}
