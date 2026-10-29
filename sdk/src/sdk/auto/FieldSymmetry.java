package sdk.auto;

/**
 * How the red half of the field matches the blue half, so an Auto written once for blue works
 * for red. Pick the one for this season once (e.g. in a {@code Field} constants class); poses are
 * in Pedro's coordinates: inches on a 144 × 144 field from a corner, heading in radians.
 *
 * <table>
 *   <tr><th>Season</th><th>Symmetry</th></tr>
 *   <tr><td>DECODE (2025-26)</td><td>{@link #MIRROR}</td></tr>
 *   <tr><td>INTO THE DEEP (2024-25)</td><td>{@link #ROTATE}</td></tr>
 *   <tr><td>CENTERSTAGE (2023-24)</td><td>{@link #MIRROR}</td></tr>
 * </table>
 * Always check against the season's field drawing: it is a property of the field, not a rule.
 */
public enum FieldSymmetry {
    /** Mirror image across the line down the middle (x = 72): x flips, y stays. */
    MIRROR,
    /** Turned half a circle about the center: both x and y flip, heading turns 180°. */
    ROTATE;

    public static final double FIELD_SIZE = 144;

    /** The other alliance's x for {@code x}. */
    public double x(double x) {
        return FIELD_SIZE - x;
    }

    /** The other alliance's y for {@code y}. */
    public double y(double y) {
        return this == MIRROR ? y : FIELD_SIZE - y;
    }

    /** The other alliance's heading for {@code heading}, in radians. */
    public double heading(double heading) {
        double flipped = this == MIRROR ? Math.PI - heading : heading + Math.PI;
        return Math.atan2(Math.sin(flipped), Math.cos(flipped));   // Keep it between -π and π
    }
}
