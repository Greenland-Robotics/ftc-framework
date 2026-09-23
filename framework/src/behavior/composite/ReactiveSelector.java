package behavior.composite;

import behavior.Node;
import behavior.Status;

/**
 * A {@link Selector} that re-ticks its children from the first one every tick, so a
 * higher-priority child that starts succeeding (or running) takes over from a lower one,
 * which is halted.
 */
public final class ReactiveSelector extends OrderedComposite {
    public ReactiveSelector(Node... children) {
        super(Status.FAILURE, true, children);
    }
}
