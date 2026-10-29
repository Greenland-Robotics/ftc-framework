package hive;

import static util.MathUtil.approach;
import static util.MathUtil.clamp;
import static util.MathUtil.distance;

import java.util.ArrayList;
import java.util.List;

/**
 * One alliance's hive: a seesaw lying along the field's y axis, with a cup at each end. Balls
 * that land in a cup weigh down that end (a wiffle ball 1 unit, a softball 3). Once one end
 * outweighs the other by {@value #TIP_UNITS} units the seesaw pivots to it, and every pivot scores
 * {@value #PIVOT_POINTS} points for the hive's alliance, and the cup that went down tips its
 * balls out onto the floor. So an alliance keeps scoring by filling whichever end of its own hive
 * is up, and the balls come back into play.
 */
final class Hive {
    static final double LENGTH = 48;
    static final double WIDTH = 14;
    static final double CUP_OFFSET = 18;
    static final double CUP_RADIUS = 6.5;
    static final int PIVOT_POINTS = 20;
    /** How much heavier an end must be than the other to pull it down. */
    static final int TIP_UNITS = 20;

    /** The two ends: index 0 is the top end (larger y), 1 the bottom end. */
    static final int TOP = 0;
    static final int BOTTOM = 1;

    final Alliance alliance;
    final double x;
    final double y;
    private final List<List<Ball>> cups = new ArrayList<>();
    final int[] weight = new int[2];
    int down = -1;      // TOP or BOTTOM; -1 while it is still level
    int pivots;
    double tilt;        // -1 (bottom end down) .. 1 (top end down), animated

    Hive(Alliance alliance, double x, double y) {
        this.alliance = alliance;
        this.x = x;
        this.y = y;
        cups.add(new ArrayList<>());
        cups.add(new ArrayList<>());
    }

    double cupX(int end) {
        return x;
    }

    double cupY(int end) {
        return end == TOP ? y + CUP_OFFSET : y - CUP_OFFSET;
    }

    /** Units still needed in {@code end} to pull it down (0 if it is down already). */
    int unitsToTip(int end) {
        if (down == end) return 0;
        return Math.max(0, TIP_UNITS - (weight[end] - weight[1 - end]));
    }

    /**
     * The end to shoot at to make it pivot: the one that is up. While level, the one closer to
     * tipping (the nearer one if they are even), so balls aren't split between the ends: shared
     * out, the field can run dry with neither end 20 ahead, and then nothing ever pivots again.
     */
    int endToShoot(double fromX, double fromY) {
        if (down >= 0) return 1 - down;
        if (weight[TOP] != weight[BOTTOM]) return weight[TOP] > weight[BOTTOM] ? TOP : BOTTOM;
        return distance(fromX, fromY, cupX(TOP), cupY(TOP)) <= distance(fromX, fromY, cupX(BOTTOM), cupY(BOTTOM))
                ? TOP : BOTTOM;
    }

    /** The cup a ball at (bx, by) dropped into, or -1. */
    int cupAt(double bx, double by) {
        for (int end = 0; end < 2; end++) {
            if (distance(bx, by, cupX(end), cupY(end)) <= CUP_RADIUS) return end;
        }
        return -1;
    }

    void add(Ball ball, int end) {
        cups.get(end).add(ball);
        weight[end] += ball.weight();
    }

    int ballsIn(int end) {
        return cups.get(end).size();
    }

    /**
     * Pivots if one end now outweighs the other by enough. Returns the balls that fell out of the
     * cup that went down (empty if it didn't pivot); each pivot scores.
     */
    List<Ball> settle(double dt) {
        int difference = weight[TOP] - weight[BOTTOM];
        int heavier = difference >= TIP_UNITS ? TOP : difference <= -TIP_UNITS ? BOTTOM : down;
        List<Ball> spilled = new ArrayList<>();
        if (heavier != down) {
            down = heavier;
            pivots++;
            spilled.addAll(cups.get(down));
            cups.get(down).clear();
            weight[down] = 0;
        }
        double target = down < 0 ? 0 : down == TOP ? 1 : -1;
        tilt = approach(tilt, target, dt * 4);
        return spilled;
    }

    /** How far to move a circle at (cx, cy) so it no longer overlaps this hive, or null if it doesn't. */
    double[] pushOut(double cx, double cy, double radius) {
        double halfX = WIDTH / 2;
        double halfY = LENGTH / 2;
        double closestX = clamp(cx, x - halfX, x + halfX);
        double closestY = clamp(cy, y - halfY, y + halfY);
        double dx = cx - closestX;
        double dy = cy - closestY;
        double distance = Math.hypot(dx, dy);
        if (distance >= radius) return null;
        if (distance == 0) {
            // The center is inside: leave by the nearest side
            double toSideX = halfX - Math.abs(cx - x);
            double toSideY = halfY - Math.abs(cy - y);
            return toSideX < toSideY
                    ? new double[] {Math.copySign(toSideX + radius, cx - x), 0}
                    : new double[] {0, Math.copySign(toSideY + radius, cy - y)};
        }
        return new double[] {dx / distance * (radius - distance), dy / distance * (radius - distance)};
    }
}
