package hive;

/** Where a robot is on the field: inches from the bottom-left corner, heading in radians (0 = facing right). */
public final class Pose {
    public final double x;
    public final double y;
    public final double heading;

    public Pose(double x, double y, double heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    public double distanceTo(double otherX, double otherY) {
        return Math.hypot(otherX - x, otherY - y);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "(%.0f, %.0f) %.0f°", x, y, Math.toDegrees(heading));
    }
}
