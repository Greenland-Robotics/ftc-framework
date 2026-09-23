package behavior.composite;

import behavior.Node;
import behavior.Status;

/**
 * A {@link Sequence} that re-ticks its children from the first one every tick. Put conditions
 * first: if one stops holding, the running child is halted and the sequence fails.
 * Children before the running one are re-run, so they should be conditions, not actions.
 */
public final class ReactiveSequence extends OrderedComposite {
    public ReactiveSequence(Node... children) {
        super(Status.SUCCESS, true, children);
    }
}
