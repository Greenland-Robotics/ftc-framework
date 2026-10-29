package robot.subsystems;

import static behavior.Behaviors.sequence;

import behavior.Node;
import sdk.ftc.Hardware;
import sdk.parts.Lift;
import sdk.parts.ServoPart;
import subsystem.Subsystem;

/** The vertical lift with a claw on top: baskets, chambers, and the end game hang. */
public class Scorer implements Subsystem {
    public static final double DOWN = 0;
    public static final double HIGH_CHAMBER = 1500;
    public static final double HIGH_BASKET = 3000;
    public static final double HANG_REACH = 2400;
    public static final double HANG_PULL = 600;

    public static final double OPEN = 0.65;
    public static final double CLOSED = 0.3;

    public final Lift lift;
    public final ServoPart claw;

    public Scorer(Hardware hw) {
        lift = new Lift(hw.motor("lift").reversed()).limits(DOWN, 3100).gravity(0.1).manualSpeed(1800);
        claw = new ServoPart(hw.servo("claw")).range(CLOSED, OPEN).moveTime(200).startAt(CLOSED);
    }

    public Node grab() {
        return claw.setTo(CLOSED);
    }

    public Node release() {
        return claw.setTo(OPEN);
    }

    /** Up to the high basket, drop the sample, back down. */
    public Node scoreBasket() {
        return sequence(lift.goTo(HIGH_BASKET), release(), lift.goTo(DOWN)).named("Score in the high basket");
    }

    /** Above the high chamber, then down onto it so the clip catches, then let go. */
    public Node hangSpecimen() {
        return sequence(lift.goTo(HIGH_CHAMBER + 250), lift.goTo(HIGH_CHAMBER - 150), release())
                .named("Hang a specimen");
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
