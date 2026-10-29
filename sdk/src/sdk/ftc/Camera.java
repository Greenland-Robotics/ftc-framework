package sdk.ftc;

import java.util.Objects;

import android.util.Size;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;

import sdk.vision.Randomization;
import sdk.vision.Vote;
import subsystem.Subsystem;

/**
 * A webcam that reads the season's randomization during init, so the Auto knows which way to
 * go the moment it starts.
 * <pre>
 * // Robot.java
 * camera = register(new Camera(hw, "Webcam 1", new TagReader()
 *         .map(21, Randomization.LEFT).map(22, Randomization.CENTER).map(23, Randomization.RIGHT)));
 *
 * // The Auto's routine(), which runs at start
 * Randomization motif = robot.camera.result().orElse(Randomization.CENTER);
 * </pre>
 * It keeps voting over the last frames while the robot waits in init (see {@link Vote}), so one
 * bad frame doesn't change the answer. The FTC SDK closes the camera when the OpMode ends.
 */
public final class Camera implements Subsystem {
    /** The resolution frames are taken at; {@link PropFinder} needs the width. */
    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;

    private final VisionPortal portal;
    private final Detector detector;
    private final Vote vote = new Vote(15, 8);

    public Camera(Hardware hw, String webcamName, Detector detector) {
        this.detector = Objects.requireNonNull(detector, "detector");
        portal = new VisionPortal.Builder()
                .setCamera(hw.device(WebcamName.class, webcamName))
                .setCameraResolution(new Size(WIDTH, HEIGHT))
                .addProcessors(detector.processors())
                .build();
    }

    /** The steady answer from the last frames, or UNKNOWN if they don't agree (yet). */
    public Randomization result() {
        return vote.result();
    }

    /** The FTC vision portal, e.g. to stop streaming after the Auto has its answer. */
    public VisionPortal portal() {
        return portal;
    }

    @Override
    public void initPeriodic() {
        vote.add(detector.read());
    }

    @Override
    public void periodic() {
        vote.add(detector.read());
    }
}
