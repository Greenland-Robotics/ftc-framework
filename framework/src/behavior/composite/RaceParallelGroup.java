package behavior.composite;

import behavior.Node;

/**
 * RaceParallelGroup is a wrapper for Parallel
 */
public final class RaceParallelGroup extends Parallel {

    public RaceParallelGroup(Node... children) {
        super(ParallelPolicy.ANY_SUCCEEDS, children);
    }
}
