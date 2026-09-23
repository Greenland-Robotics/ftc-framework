package behavior;

/** The result of ticking a {@link Node}. */
public enum Status {
    /** Not done yet: tick me again next loop. */
    RUNNING,
    /** Finished and did what it was supposed to. */
    SUCCESS,
    /** Finished without doing what it was supposed to. Selectors and retries react to this. */
    FAILURE;

    public boolean isDone() {
        return this != RUNNING;
    }
}
