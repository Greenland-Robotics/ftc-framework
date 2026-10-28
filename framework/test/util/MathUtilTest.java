package util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static util.MathUtil.angleError;
import static util.MathUtil.approach;
import static util.MathUtil.clamp;
import static util.MathUtil.clampPower;
import static util.MathUtil.distance;
import static util.MathUtil.toFieldFrame;
import static util.MathUtil.toRobotFrame;
import static util.MathUtil.wrapAngle;

import org.junit.jupiter.api.Test;

class MathUtilTest {
    private static final double EPSILON = 1e-9;

    @Test
    void clampKeepsValuesInRange() {
        assertEquals(2, clamp(5, -2, 2));
        assertEquals(-2, clamp(-5, -2, 2));
        assertEquals(0.5, clamp(0.5, -2, 2));
        assertEquals(1, clampPower(3));
        assertEquals(-1, clampPower(-3));
    }

    @Test
    void approachStepsTowardTheTargetWithoutOvershooting() {
        assertEquals(0.25, approach(0, 1, 0.25), EPSILON);
        assertEquals(1, approach(0.9, 1, 0.25), EPSILON);
        assertEquals(-0.25, approach(0, -1, 0.25), EPSILON);
    }

    @Test
    void wrapAngleStaysBetweenMinusPiAndPi() {
        assertEquals(-Math.PI / 2, wrapAngle(3 * Math.PI / 2), EPSILON);
        assertEquals(Math.PI / 2, wrapAngle(-3 * Math.PI / 2), EPSILON);
        assertEquals(0.5, wrapAngle(0.5 + 4 * Math.PI), EPSILON);
    }

    @Test
    void angleErrorTurnsTheShortWayRound() {
        // From 170° to -170° is 20° to the left, not 340° to the right
        assertEquals(Math.toRadians(20), angleError(Math.toRadians(-170), Math.toRadians(170)), EPSILON);
    }

    @Test
    void distanceIsStraightLine() {
        assertEquals(5, distance(1, 1, 4, 5), EPSILON);
    }

    @Test
    void framesConvertBothWays() {
        double heading = Math.PI / 2;   // Facing +y
        // Something straight ahead of the robot is +y on the field
        assertArrayEquals(new double[] {0, 3}, toFieldFrame(3, 0, heading), EPSILON);
        // Something at +x on the field is to the robot's right
        assertArrayEquals(new double[] {0, -2}, toRobotFrame(2, 0, heading), EPSILON);
        double[] there = toFieldFrame(1.5, -0.7, 0.9);
        assertArrayEquals(new double[] {1.5, -0.7}, toRobotFrame(there[0], there[1], 0.9), EPSILON);
    }
}
