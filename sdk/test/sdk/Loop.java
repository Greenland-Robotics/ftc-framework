package sdk;

import behavior.Node;
import behavior.Status;
import subsystem.Subsystem;

/** Runs an OpMode-style loop in a test: the world moves, the part updates, then the node ticks. */
public final class Loop {
    public static final long MS = 20;

    private final FakeClock clock;
    private final Subsystem part;
    private final FakeMotor motor;

    public Loop(FakeClock clock, Subsystem part, FakeMotor motor) {
        this.clock = clock;
        this.part = part;
        this.motor = motor;
    }

    /** One loop with nothing running. */
    public void idle() {
        if (motor != null) motor.step(MS);
        clock.advance(MS);
        part.periodic();
    }

    /** One loop, ticking {@code node}. */
    public Status tick(Node node) {
        idle();
        return node.tick();
    }

    /** Ticks {@code node} until it finishes or {@code maxLoops} pass; returns how it ended (RUNNING if it never did). */
    public Status runUntilDone(Node node, int maxLoops) {
        Status status = Status.RUNNING;
        for (int i = 0; i < maxLoops && status == Status.RUNNING; i++) {
            status = tick(node);
        }
        return status;
    }
}
