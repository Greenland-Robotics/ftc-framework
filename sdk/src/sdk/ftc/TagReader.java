package sdk.ftc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.firstinspires.ftc.vision.VisionProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import sdk.vision.Randomization;

/**
 * Reads AprilTags: which one is showing (DECODE's obelisk, a custom signal sleeve) and where a
 * tag is (DECODE's goal tags, CENTERSTAGE's backdrop) for lining up a shot.
 * <pre>
 * new TagReader().map(21, LEFT).map(22, CENTER).map(23, RIGHT)   // DECODE motif GPP / PGP / PPG
 * </pre>
 */
public final class TagReader implements Detector {
    private final AprilTagProcessor processor = AprilTagProcessor.easyCreateWithDefaults();
    private final Map<Integer, Randomization> answers = new HashMap<>();

    /** Seeing tag {@code id} means {@code answer}. */
    public TagReader map(int id, Randomization answer) {
        answers.put(id, answer);
        return this;
    }

    /**
     * Tag {@code id} if the camera sees it now, else null. Its {@code ftcPose} has {@code range}
     * (inches) and {@code bearing} (degrees, positive = to the left) for aiming.
     */
    public AprilTagDetection tag(int id) {
        for (AprilTagDetection detection : processor.getDetections()) {
            if (detection.id == id) return detection;
        }
        return null;
    }

    @Override
    public VisionProcessor[] processors() {
        return new VisionProcessor[] {processor};
    }

    @Override
    public Randomization read() {
        List<AprilTagDetection> detections = processor.getDetections();
        for (AprilTagDetection detection : detections) {
            Randomization answer = answers.get(detection.id);
            if (answer != null) return answer;
        }
        return Randomization.UNKNOWN;
    }
}
