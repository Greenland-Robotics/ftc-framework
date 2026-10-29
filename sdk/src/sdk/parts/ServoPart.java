package sdk.parts;

import static util.MathUtil.clamp;

import java.util.Objects;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import behavior.runtime.Clock;
import sdk.hardware.Servo;
import subsystem.Subsystem;

/**
 * A servo moved between a few positions: a claw, a wrist, a gate, a kicker, a drone latch.
 * Servos can't say when they've arrived, so {@link #setTo} waits a fixed {@link #moveTime} that
 * you measure once.
 * <pre>
 * // Robot.java
 * claw = register(new ServoPart(hw.servo("claw")).range(0.2, 0.8).moveTime(250).startAt(CLOSED));
 *
 * sequence(robot.claw.setTo(OPEN), robot.lift.goTo(DOWN))   // the lift waits until the claw is open
 * controls.onPress(operator.a(), robot.claw.toggle(OPEN, CLOSED));
 * </pre>
 * Two servos driving one joint from opposite sides? {@link #pairedWith} moves the second one to
 * the mirrored position, so they never fight.
 */
public class ServoPart implements Subsystem {
    private final Servo servo;
    private final Clock clock;
    private Servo partner;
    private double min = 0;
    private double max = 1;
    private long moveMillis = 300;
    private double position = Double.NaN;

    public ServoPart(Servo servo) {
        this(servo, Clock.system());
    }

    public ServoPart(Servo servo, Clock clock) {
        this.servo = Objects.requireNonNull(servo, "servo");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The positions it may be sent to; anything outside is clamped. Keeps it off hard stops. */
    public ServoPart range(double min, double max) {
        if (min > max) throw new IllegalArgumentException("range: min " + min + " is above max " + max);
        this.min = clamp(min, 0, 1);
        this.max = clamp(max, 0, 1);
        return this;
    }

    /** How long it takes to get anywhere, in milliseconds. */
    public ServoPart moveTime(long millis) {
        this.moveMillis = Math.max(0, millis);
        return this;
    }

    /** A second servo on the same joint, mounted facing the other way: it always gets {@code 1 - position}. */
    public ServoPart pairedWith(Servo other) {
        this.partner = Objects.requireNonNull(other, "other");
        return this;
    }

    /** Moves there now, while the robot is being set up (servos may move during init). */
    public ServoPart startAt(double position) {
        set(position);
        return this;
    }

    // ---- Behaviors ----

    /** Moves to {@code position} and succeeds once {@link #moveTime} has passed. */
    public Node setTo(double position) {
        return new Move(() -> position);
    }

    /** Moves to {@code a}, or to {@code b} if it is already at {@code a}. */
    public Node toggle(double a, double b) {
        return new Move(() -> isAt(a) ? b : a);
    }

    // ---- Checks ----

    /** The last position it was sent to (after clamping), or NaN before the first. */
    public double position() {
        return position;
    }

    public boolean isAt(double position) {
        return Math.abs(this.position - clamp(position, min, max)) < 1e-6;
    }

    private void set(double target) {
        position = clamp(target, min, max);
        servo.setPosition(position);
        if (partner != null) partner.setPosition(1 - position);
    }

    private interface Target {
        double get();
    }

    private final class Move extends Action {
        private final Target target;
        private long startedAt;

        Move(Target target) {
            this.target = target;
        }

        @Override
        protected void start() {
            set(target.get());
            startedAt = clock.nowMillis();
        }

        @Override
        protected Status update() {
            return clock.nowMillis() - startedAt >= moveMillis ? Status.SUCCESS : Status.RUNNING;
        }
    }
}
