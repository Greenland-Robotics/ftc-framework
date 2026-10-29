package robot;

import java.util.ArrayList;
import java.util.List;

import com.qualcomm.robotcore.hardware.HardwareMap;

import pedro.Constants;
import pedro.Drive;
import robot.subsystems.Launcher;
import sdk.auto.FieldSymmetry;
import sdk.ftc.Camera;
import sdk.ftc.Hardware;
import sdk.ftc.Tuning;
import sdk.ftc.TagReader;
import sdk.parts.Roller;
import sdk.vision.Randomization;
import subsystem.Subsystem;

/**
 * DECODE (2025-26): pick up artifacts (balls) with a roller intake, launch them into the goal
 * with a flywheel, and read the obelisk's AprilTag for the motif.
 */
public class Robot {
    /** DECODE's field is a mirror image across the middle line. */
    public static final FieldSymmetry SYMMETRY = FieldSymmetry.MIRROR;

    public final Drive drive;
    public final Roller intake;
    public final Launcher launcher;
    public final Camera camera;

    private final List<Subsystem> subsystems = new ArrayList<>();

    public Robot(HardwareMap hardwareMap) {
        Hardware hw = new Hardware(hardwareMap);
        drive = register(new Drive(Constants.createFollower(hardwareMap)));
        intake = register(new Roller(hw.motor("intake")).power(1).sensor(hw.distanceBelow("intakeSensor", 4)));
        launcher = register(new Launcher(hw));
        // The obelisk shows tag 21 (GPP), 22 (PGP) or 23 (PPG)
        camera = register(new Camera(hw, "Webcam 1", new TagReader()
                .map(21, Randomization.LEFT)
                .map(22, Randomization.CENTER)
                .map(23, Randomization.RIGHT)));

        // Gains saved by the Tune OpMode replace the numbers above
        Tuning.applySaved(this);
    }

    public void initPeriodic() {
        for (Subsystem subsystem : subsystems) {
            subsystem.initPeriodic();
        }
    }

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
