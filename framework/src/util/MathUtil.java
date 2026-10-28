package util;

/**
 * Small math helpers that robot code needs again and again: limiting values, angles, and
 * turning directions between the field and the robot. Plain Java, so any subsystem, Action or
 * simulation can use them: {@code import static util.MathUtil.*;}
 *
 * Angles are in radians, counter-clockwise positive. "Field frame" is x/y on the field; "robot
 * frame" is (forward, left) as seen from the robot.
 */
public final class MathUtil {
    private MathUtil() {}

    // ---- Limits ----

    /** {@code value}, kept between {@code min} and {@code max}. */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** {@code value} kept to a motor's range, -1..1. */
    public static double clampPower(double value) {
        return clamp(value, -1, 1);
    }

    /** Moves {@code current} toward {@code target} by at most {@code maxStep}, e.g. to ramp a value each loop. */
    public static double approach(double current, double target, double maxStep) {
        return current + clamp(target - current, -maxStep, maxStep);
    }

    // ---- Angles ----

    /** The same angle, expressed between -π and π. */
    public static double wrapAngle(double radians) {
        return Math.atan2(Math.sin(radians), Math.cos(radians));
    }

    /** How far to turn from {@code current} to face {@code target}, the short way round: -π..π. */
    public static double angleError(double target, double current) {
        return wrapAngle(target - current);
    }

    // ---- Distances and frames ----

    /** Straight-line distance between two points. */
    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    /** A field-frame offset (dx, dy) as seen by a robot facing {@code heading}: {forward, left}. */
    public static double[] toRobotFrame(double dx, double dy, double heading) {
        double c = Math.cos(heading);
        double s = Math.sin(heading);
        return new double[] {dx * c + dy * s, -dx * s + dy * c};
    }

    /** A robot-frame offset (forward, left) for a robot facing {@code heading}, as a field-frame {dx, dy}. */
    public static double[] toFieldFrame(double forward, double left, double heading) {
        double c = Math.cos(heading);
        double s = Math.sin(heading);
        return new double[] {forward * c - left * s, forward * s + left * c};
    }
}
