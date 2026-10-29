package sdk.auto;

/**
 * Remembers where the robot was when the last OpMode ended, so the TeleOp that follows an Auto
 * knows where the robot is (and which way field-centric "forward" is) without resetting. OpModeBase
 * saves it at the end of every OpMode and TeleOpBase starts from it. Forgotten when the robot
 * app restarts.
 */
public final class PoseMemory {
    private static boolean saved;
    private static double x;
    private static double y;
    private static double heading;

    private PoseMemory() {}

    public static void save(double x, double y, double heading) {
        PoseMemory.x = x;
        PoseMemory.y = y;
        PoseMemory.heading = heading;
        saved = true;
    }

    public static boolean has() {
        return saved;
    }

    public static double x() {
        return x;
    }

    public static double y() {
        return y;
    }

    public static double heading() {
        return heading;
    }

    public static void clear() {
        saved = false;
    }
}
