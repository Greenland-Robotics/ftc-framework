package behavior.decorator;

import behavior.Node;
import behavior.Status;

/** Swaps SUCCESS and FAILURE, e.g. {@code condition(intake::hasSample).invert()}. */
public final class Invert extends Decorator {
    public Invert(Node child) {
        super(child);
    }

    @Override
    public Status tick() {
        Status status = child.tick();
        switch (status) {
            case SUCCESS: return Status.FAILURE;
            case FAILURE: return Status.SUCCESS;
            default: return status;
        }
    }
}
