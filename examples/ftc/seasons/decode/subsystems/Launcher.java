package robot.subsystems;

import static behavior.Behaviors.race;
import static behavior.Behaviors.sequence;
import static behavior.Behaviors.waitUntil;

import behavior.Node;
import sdk.ftc.Hardware;
import sdk.parts.Flywheel;
import sdk.parts.ServoPart;
import subsystem.Subsystem;

/**
 * A season subsystem made of sdk parts: a flywheel and the servo that feeds it. It adds the
 * game's words ("shoot one", "shoot three") on top; the parts do the motor control.
 */
public class Launcher implements Subsystem {
    public static final double RPM = 4200;   // Tune: how fast lands an artifact in the goal from the launch zone
    private static final double REST = 0.15;
    private static final double PUSH = 0.6;

    public final Flywheel flywheel;
    private final ServoPart feeder;

    public Launcher(Hardware hw) {
        flywheel = new Flywheel(hw.motor("shooter").coast(), 28).maxRpm(6000).tolerance(120);
        feeder = new ServoPart(hw.servo("feeder")).moveTime(150).startAt(REST);
    }

    /** Spins up, feeds one artifact once it is at speed, and pulls the feeder back. */
    public Node shootOne() {
        return race(
                flywheel.spin(RPM),
                sequence(waitUntil(flywheel::atSpeed), feeder.setTo(PUSH), feeder.setTo(REST))
        ).named("Shoot one");
    }

    /** Keeps the flywheel spinning between shots, so the second and third don't wait for spin-up. */
    public Node shootThree() {
        Node feedOne = sequence(waitUntil(flywheel::atSpeed), feeder.setTo(PUSH), feeder.setTo(REST));
        return race(flywheel.spin(RPM), feedOne.repeat(3)).named("Shoot three");
    }

    @Override
    public void periodic() {
        flywheel.periodic();
    }
}
