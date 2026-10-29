package robot;

import java.util.ArrayList;
import java.util.List;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.vision.opencv.ColorRange;

import pedro.Constants;
import pedro.Drive;
import robot.subsystems.PixelScorer;
import sdk.auto.Alliance;
import sdk.auto.FieldSymmetry;
import sdk.ftc.Camera;
import sdk.ftc.Hardware;
import sdk.ftc.Tuning;
import sdk.ftc.PropFinder;
import sdk.parts.Lift;
import sdk.parts.ServoPart;
import subsystem.Subsystem;

/**
 * CENTERSTAGE (2023-24): find the team prop on a spike mark, carry two pixels in a pair of
 * claws on a lift to the backdrop, launch a paper drone and hang from the rigging at the end.
 */
public class Robot {
    /** CENTERSTAGE's field is a mirror image across the middle line. */
    public static final FieldSymmetry SYMMETRY = FieldSymmetry.MIRROR;

    public static final double DRONE_HELD = 0.2;
    public static final double DRONE_LAUNCH = 0.7;

    public final Drive drive;
    public final PixelScorer scorer;
    public final ServoPart drone;
    public final Lift hang;
    public final Camera camera;

    /** Which alliance this match is; the Auto sets it from its menu, so the camera looks for the right prop color. */
    public Alliance alliance = Alliance.BLUE;

    private final List<Subsystem> subsystems = new ArrayList<>();

    public Robot(HardwareMap hardwareMap) {
        Hardware hw = new Hardware(hardwareMap);
        drive = register(new Drive(Constants.createFollower(hardwareMap)));
        scorer = register(new PixelScorer(hw));
        drone = register(new ServoPart(hw.servo("drone")).moveTime(300).startAt(DRONE_HELD));
        hang = register(new Lift(hw.motor("hang")).limits(0, 9000).manualSpeed(4000));
        camera = register(new Camera(hw, "Webcam 1",
                new PropFinder(ColorRange.RED, ColorRange.BLUE, () -> alliance)));

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
