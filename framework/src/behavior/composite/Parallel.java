package behavior.composite;

import java.util.Arrays;
import java.util.Objects;

import behavior.Node;
import behavior.Status;

/**
 * Ticks all children every loop. The {@link ParallelPolicy} decides when it is done; any
 * children still running at that point are halted. Finished children are not ticked again.
 */
public final class Parallel extends Composite {
    private final ParallelPolicy policy;
    private final Status[] results;

    public Parallel(ParallelPolicy policy, Node... children) {
        super(children);
        this.policy = Objects.requireNonNull(policy, "policy");
        this.results = new Status[this.children.size()];
        reset();
    }

    @Override
    public Status tick() {
        int succeeded = 0;
        int failed = 0;
        for (int i = 0; i < children.size(); i++) {
            if (results[i] == Status.RUNNING) {
                results[i] = children.get(i).tick();
            }
            if (results[i] == Status.SUCCESS) {
                succeeded++;
            } else if (results[i] == Status.FAILURE) {
                failed++;
            }
        }
        Status status = policy.decide(succeeded, failed, children.size());
        if (status.isDone()) {
            halt();
        }
        return status;
    }

    @Override
    public void halt() {
        haltChildrenFrom(0);
        reset();
    }

    private void reset() {
        Arrays.fill(results, Status.RUNNING);
    }
}
