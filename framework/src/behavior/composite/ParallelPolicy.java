package behavior.composite;

import behavior.Status;

/** Decides when a {@link Parallel} is done, from how many children have succeeded and failed. */
public interface ParallelPolicy {
    /** Succeeds when every child succeeds; fails as soon as any child fails. */
    ParallelPolicy ALL_SUCCEED = (succeeded, failed, total) ->
            failed > 0 ? Status.FAILURE : succeeded == total ? Status.SUCCESS : Status.RUNNING;

    /** Succeeds as soon as any child succeeds (a race); fails only if every child fails. */
    ParallelPolicy ANY_SUCCEEDS = (succeeded, failed, total) ->
            succeeded > 0 ? Status.SUCCESS : failed == total ? Status.FAILURE : Status.RUNNING;

    Status decide(int succeeded, int failed, int total);
}
