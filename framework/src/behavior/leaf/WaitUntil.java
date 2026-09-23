package behavior.leaf;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import behavior.Node;
import behavior.Status;

/** Stays RUNNING until {@code check} is true, then succeeds. Combine with a timeout to give up. */
public final class WaitUntil implements Node {
    private final BooleanSupplier check;

    public WaitUntil(BooleanSupplier check) {
        this.check = Objects.requireNonNull(check, "check");
    }

    @Override
    public Status tick() {
        return check.getAsBoolean() ? Status.SUCCESS : Status.RUNNING;
    }
}
