package robot.subsystems;

import static behavior.Behaviors.parallel;
import static behavior.Behaviors.sequence;

import behavior.Node;
import sdk.ftc.Hardware;
import sdk.parts.Lift;
import sdk.parts.ServoPart;
import subsystem.Subsystem;

/** A lift with two claws, one per pixel, so the Auto can drop them one at a time. */
public class PixelScorer implements Subsystem {
    public static final double DOWN = 0;
    public static final double LOW = 800;      // First set line on the backdrop
    public static final double HIGH = 1800;

    private static final double OPEN = 0.6;
    private static final double CLOSED = 0.25;

    public final Lift lift;
    public final ServoPart left;
    public final ServoPart right;

    public PixelScorer(Hardware hw) {
        lift = new Lift(hw.motor("lift")).limits(DOWN, 2000).gravity(0.06);
        left = new ServoPart(hw.servo("leftClaw")).moveTime(200).startAt(CLOSED);
        right = new ServoPart(hw.servo("rightClaw").reversed()).moveTime(200).startAt(CLOSED);
    }

    public Node grabBoth() {
        return parallel(left.setTo(CLOSED), right.setTo(CLOSED));
    }

    public Node dropLeft() {
        return left.setTo(OPEN);
    }

    public Node dropRight() {
        return right.setTo(OPEN);
    }

    /** Up to {@code height}, drop both pixels, close the claws and come back down. */
    public Node score(double height) {
        return sequence(
                lift.goTo(height),
                parallel(dropLeft(), dropRight()),
                grabBoth(),
                lift.goTo(DOWN)
        ).named("Score both pixels");
    }

    @Override
    public void initPeriodic() {
        lift.initPeriodic();
    }

    @Override
    public void periodic() {
        lift.periodic();
    }
}
