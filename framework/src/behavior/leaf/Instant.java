package behavior.leaf;

import java.util.Objects;

import behavior.Node;
import behavior.Status;

/** Runs a piece of code once and succeeds, e.g. {@code instant(() -> claw.setPosition(OPEN))}. */
public final class Instant implements Node {
    private final Runnable action;

    public Instant(Runnable action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    @Override
    public Status tick() {
        action.run();
        return Status.SUCCESS;
    }
}
