package behavior.composite;

import behavior.Node;

/**
 * RaceParallelGroup is a wrapper for Parallel
 */
public final class RaceParallelGroup extends Parallel {
    /**
     * RaceParallelGroup is a group that ends when any child Node ends.
     * @param children Nodes to race in Parallel
     */
    public RaceParallelGroup(Node... children) {
        super(ParallelPolicy.ANY_SUCCEEDS, children);
    }
}
