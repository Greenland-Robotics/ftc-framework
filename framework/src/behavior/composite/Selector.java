package behavior.composite;

import behavior.Node;
import behavior.Status;

/**
 * Tries children one after another until one succeeds (a "fallback"). Fails only if all fail.
 * Latched: remembers which child is running and resumes there.
 */
public final class Selector extends OrderedComposite {
    public Selector(Node... children) {
        super(Status.FAILURE, false, children);
    }
}
