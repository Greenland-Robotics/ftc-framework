package behavior.decorator;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import behavior.Node;
import behavior.Status;

/**
 * Checks {@code condition} every tick and only lets the child run while it holds. The moment it
 * doesn't, the child is halted and the Guard fails, e.g. stop a path if the robot bumps something.
 */
public final class Guard extends Decorator {
    private final BooleanSupplier condition;

    public Guard(BooleanSupplier condition, Node child) {
        super(child);
        this.condition = Objects.requireNonNull(condition, "condition");
    }

    @Override
    public Status tick() {
        if (!condition.getAsBoolean()) {
            child.halt();
            return Status.FAILURE;
        }
        return child.tick();
    }
}
