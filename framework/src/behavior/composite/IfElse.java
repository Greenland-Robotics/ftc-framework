package behavior.composite;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import behavior.Node;
import behavior.Status;

/**
 * Checks {@code condition} once when it starts and runs {@code then} or {@code otherwise} to the
 * end, reporting that branch's result. To re-check every tick instead, see {@code Node.onlyWhile}.
 */
public final class IfElse implements Node {
    private final BooleanSupplier condition;
    private final Node then;
    private final Node otherwise;
    private Node chosen;

    public IfElse(BooleanSupplier condition, Node then, Node otherwise) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.then = Objects.requireNonNull(then, "then");
        this.otherwise = Objects.requireNonNull(otherwise, "otherwise");
    }

    @Override
    public Status tick() {
        if (chosen == null) {
            chosen = condition.getAsBoolean() ? then : otherwise;
        }
        Status status = chosen.tick();
        if (status.isDone()) {
            chosen = null;
        }
        return status;
    }

    @Override
    public void halt() {
        if (chosen != null) {
            chosen.halt();
            chosen = null;
        }
    }
}
