package sdk.ftc;

import org.firstinspires.ftc.vision.VisionProcessor;

import sdk.vision.Randomization;

/** Something a {@link Camera} runs on every frame that answers LEFT, CENTER or RIGHT: {@link TagReader}, {@link PropFinder}. */
public interface Detector {
    /** The FTC vision processors to add to the camera. */
    VisionProcessor[] processors();

    /** What the latest frame shows, or UNKNOWN. */
    Randomization read();
}
