package hive;

import static behavior.Behaviors.instant;
import static behavior.Behaviors.sequence;

import behavior.Node;
import behavior.runtime.Clock;
import sdk.hardware.Motor;
import sdk.parts.Roller;
import subsystem.Subsystem;

/**
 * Rollers at the front that pull in balls, up to {@value Body#CAPACITY} at a time.
 *
 * The spinning is the sdk's {@link Roller}; this class only adds what is special about this
 * game: counting balls. That is the usual way to build a season subsystem: wrap sdk parts and
 * give them the game's words (see sdk/README.md, "Adding your own").
 */
public final class Intake implements Subsystem {
    private final Body body;
    private final Roller rollers;

    Intake(Body body, Clock clock) {
        this.body = body;
        Motor motor = power -> body.intakePower = power;   // On the robot: hw.motor("intake")
        this.rollers = new Roller(motor, clock);
    }

    public int ballCount() {
        return body.held.size();
    }

    public boolean hasBall() {
        return !body.held.isEmpty();
    }

    public boolean isFull() {
        return body.held.size() >= Body.CAPACITY;
    }

    /** Spins the rollers in until halted (e.g. when the driver lets go of the button). */
    public Node run() {
        return rollers.run();
    }

    /** Spins the rollers in until one more ball is inside, then stops. */
    public Node collect() {
        int[] before = new int[1];
        return sequence(
                instant(() -> before[0] = ballCount()).named("Count the balls"),
                rollers.runUntil(() -> ballCount() > before[0] || isFull()).named("Spin until one more is in")
        );
    }

    @Override
    public void periodic() {
        rollers.periodic();
    }
}
