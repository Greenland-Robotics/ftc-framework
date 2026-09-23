package behavior.composite;

import behavior.Node;
import behavior.Status;

/**
 * Runs children one after another. Succeeds when all succeed; fails as soon as one fails.
 * Latched: remembers which child is running and resumes there.
 */
public final class Sequence extends OrderedComposite {
    public Sequence(Node... children) {
        super(Status.SUCCESS, false, children);
    }
}
