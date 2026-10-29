package pedro;

import com.pedropathing.geometry.Pose;

import sdk.auto.Alliance;
import sdk.auto.FieldSymmetry;

/**
 * Write each Auto pose once, for the blue alliance, and get the red one from the field's
 * symmetry. AutoBase's {@code pose(x, y, degrees)} does this for the alliance picked in the menu.
 */
public final class Poses {
    private Poses() {}

    /** {@code bluePose} as it is on {@code alliance}'s side of the field. */
    public static Pose forAlliance(Alliance alliance, FieldSymmetry symmetry, Pose bluePose) {
        if (alliance == Alliance.BLUE) return bluePose;
        return new Pose(symmetry.x(bluePose.getX()), symmetry.y(bluePose.getY()), symmetry.heading(bluePose.getHeading()));
    }
}
