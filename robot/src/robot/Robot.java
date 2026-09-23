package robot;

import java.util.ArrayList;
import java.util.List;

import com.qualcomm.robotcore.hardware.HardwareMap;

import pedro.Constants;
import pedro.Drive;
import subsystem.Subsystem;

/**
 * Builds the robot from its subsystems. This is the only place that turns hardware into
 * subsystems: OpModes use {@code robot.drive}, {@code robot.intake}... and never the hardware map.
 *
 * <p>To add a subsystem: write it in {@code robot/subsystems/}, add a field below, and create it
 * with {@code register(...)} in the constructor so it gets its {@code periodic()} call.
 */
public class Robot {
    public final Drive drive;
    // public final Intake intake;

    private final List<Subsystem> subsystems = new ArrayList<>();

    public Robot(HardwareMap hardwareMap) {
        drive = register(new Drive(Constants.createFollower(hardwareMap)));
        // intake = register(new Intake(hardwareMap));
    }

    /** Called once per loop by the OpMode base classes, before behaviors tick. */
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
