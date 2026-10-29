package sdk.ftc;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.qualcomm.robotcore.util.SortOrder;

import org.firstinspires.ftc.vision.VisionProcessor;
import org.firstinspires.ftc.vision.opencv.ColorBlobLocatorProcessor;
import org.firstinspires.ftc.vision.opencv.ColorRange;
import org.firstinspires.ftc.vision.opencv.ImageRegion;

import sdk.auto.Alliance;
import sdk.vision.Randomization;

/**
 * Finds the biggest patch of a color and says which third of the picture it is in: CENTERSTAGE's
 * team prop, FREIGHT FRENZY's duck on the barcode, a game piece in front of the robot. Point the
 * camera so the three spots fall in the left, middle and right of the picture.
 * <pre>
 * new PropFinder(ColorRange.RED, ColorRange.BLUE, () -> alliance)   // looks for your alliance's color
 * new PropFinder(ColorRange.YELLOW)                                 // the same color either way
 * </pre>
 */
public final class PropFinder implements Detector {
    private final ColorBlobLocatorProcessor red;
    private final ColorBlobLocatorProcessor blue;
    private final Supplier<Alliance> alliance;
    private int minArea = 500;

    /** Looks for {@code color} whichever alliance you're on. */
    public PropFinder(ColorRange color) {
        this(color, color, () -> Alliance.RED);
    }

    /** Looks for {@code forRed} on red and {@code forBlue} on blue; {@code alliance} is read every frame. */
    public PropFinder(ColorRange forRed, ColorRange forBlue, Supplier<Alliance> alliance) {
        this.alliance = Objects.requireNonNull(alliance, "alliance");
        red = locator(forRed);
        blue = forBlue == forRed ? red : locator(forBlue);
    }

    /** Ignore patches smaller than this many pixels (default 500): noise, or a piece far away. */
    public PropFinder minArea(int pixels) {
        this.minArea = pixels;
        return this;
    }

    @Override
    public VisionProcessor[] processors() {
        return red == blue ? new VisionProcessor[] {red} : new VisionProcessor[] {red, blue};
    }

    @Override
    public Randomization read() {
        ColorBlobLocatorProcessor locator = alliance.get() == Alliance.BLUE ? blue : red;
        List<ColorBlobLocatorProcessor.Blob> blobs = locator.getBlobs();
        for (ColorBlobLocatorProcessor.Blob blob : blobs) {   // Sorted biggest first
            if (blob.getContourArea() < minArea) break;
            double x = blob.getBoxFit().center.x;
            if (x < Camera.WIDTH / 3.0) return Randomization.LEFT;
            if (x < Camera.WIDTH * 2 / 3.0) return Randomization.CENTER;
            return Randomization.RIGHT;
        }
        return Randomization.UNKNOWN;
    }

    private static ColorBlobLocatorProcessor locator(ColorRange color) {
        ColorBlobLocatorProcessor locator = new ColorBlobLocatorProcessor.Builder()
                .setTargetColorRange(Objects.requireNonNull(color, "color"))
                .setContourMode(ColorBlobLocatorProcessor.ContourMode.EXTERNAL_ONLY)
                .setRoi(ImageRegion.entireFrame())
                .setBlurSize(5)
                .build();
        locator.setSort(new ColorBlobLocatorProcessor.BlobSort(
                ColorBlobLocatorProcessor.BlobCriteria.BY_CONTOUR_AREA, SortOrder.DESCENDING));
        return locator;
    }
}
