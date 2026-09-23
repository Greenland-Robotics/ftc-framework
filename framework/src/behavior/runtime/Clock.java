package behavior.runtime;

/** Tells time. Nodes that wait depend on this instead of the system clock, so tests can fake it. */
public interface Clock {
    long nowMillis();

    static Clock system() {
        return () -> System.nanoTime() / 1_000_000;
    }
}
