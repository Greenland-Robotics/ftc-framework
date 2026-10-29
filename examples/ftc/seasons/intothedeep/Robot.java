package robot;

import java.util.ArrayList;
import java.util.List;

import com.qualcomm.robotcore.hardware.HardwareMap;

import pedro.Constants;
import pedro.Drive;
import robot.subsystems.SampleIntake;
import robot.subsystems.Scorer;
import sdk.auto.Alliance;
import sdk.auto.FieldSymmetry;
import sdk.ftc.Hardware;
import sdk.ftc.Tuning;
import subsystem.Subsystem;

/**
 * INTO THE DEEP (2024-25): reach into the submersible with an extendo and a roller intake (and a
 * color sensor to throw back the other alliance's samples), then lift samples to the baskets and
 * specimens to the chambers with a lift and claw. In the end game the lift pulls the robot up.
 */
public class Robot {
    /** INTO THE DEEP's field is turned half a circle about the center. */
    public static final FieldSymmetry SYMMETRY = FieldSymmetry.ROTATE;

    public final Drive drive;
    public final SampleIntake intake;
    public final Scorer scorer;

    /** Which alliance this match is; the Auto sets it from its menu, so the intake keeps the right samples. */
    public Alliance alliance = Alliance.BLUE;

    private final List<Subsystem> subsystems = new ArrayList<>();

    public Robot(HardwareMap hardwareMap) {
        Hardware hw = new Hardware(hardwareMap);
        drive = register(new Drive(Constants.createFollower(hardwareMap)));
        intake = register(new SampleIntake(hw, () -> alliance));
        scorer = register(new Scorer(hw));

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
