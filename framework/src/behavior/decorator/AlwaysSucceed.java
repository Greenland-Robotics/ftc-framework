package behavior.decorator;

import behavior.Node;
import behavior.Status;

/** Reports SUCCESS when the child finishes, even if it failed. */
public final class AlwaysSucceed extends Decorator {
    public AlwaysSucceed(Node child) {
        super(child);
    }

    @Override
    public Status tick() {
        return child.tick().isDone() ? Status.SUCCESS : Status.RUNNING;
    }
}
