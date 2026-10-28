package hive;

/** A wiffle ball or a wiffle softball, which is bigger and weighs as much as three wiffle balls. */
final class Ball {
    final boolean softball;
    double x;
    double y;
    double vx;
    double vy;

    // While it flies from a shooter to where it lands
    double fromX;
    double fromY;
    double toX;
    double toY;
    double flight;       // 0 when shot, 1 when it lands
    double flightTime;   // seconds

    Ball(boolean softball, double x, double y) {
        this.softball = softball;
        this.x = x;
        this.y = y;
    }

    double radius() {
        return softball ? 2.0 : 1.5;
    }

    /** How much it weighs down the hive. */
    int weight() {
        return softball ? 3 : 1;
    }
}
