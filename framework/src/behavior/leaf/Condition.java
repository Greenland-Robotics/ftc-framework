package behavior.leaf;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import behavior.Node;
import behavior.Status;

/** Checks something right now: SUCCESS if it is true, FAILURE if not. Never RUNNING. */
public final class Condition implements Node {
    private final BooleanSupplier check;

    public Condition(BooleanSupplier check) {
        this.check = Objects.requireNonNull(check, "check");
    }

    @Override
    public Status tick() {
        return check.getAsBoolean() ? Status.SUCCESS : Status.FAILURE;
    }
}
