package behavior.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import behavior.Node;

/**
 * Ticks the behavior trees that are currently active, once per loop, and drops each one when it
 * finishes. The OpMode base classes own one of these and tick it for you.
 */
public final class BehaviorRunner {
    private final List<Node> active = new ArrayList<>();

    /** Starts {@code behavior} on the next tick. Does nothing if it is already running. */
    public void run(Node behavior) {
        Objects.requireNonNull(behavior, "behavior");
        if (!active.contains(behavior)) {
            active.add(behavior);
        }
    }

    /** Ticks every active behavior once and removes the ones that finished. */
    public void tick() {
        // Index loop: a behavior may start another one while it ticks
        for (int i = 0; i < active.size(); ) {
            if (active.get(i).tick().isDone()) {
                active.remove(i);
            } else {
                i++;
            }
        }
    }

    /** Halts {@code behavior} if it is running. */
    public void cancel(Node behavior) {
        if (active.remove(behavior)) {
            behavior.halt();
        }
    }

    /** Halts every running behavior, e.g. when the OpMode stops. */
    public void cancelAll() {
        List<Node> stopping = new ArrayList<>(active);
        active.clear();
        for (Node behavior : stopping) {
            behavior.halt();
        }
    }

    public boolean isRunning(Node behavior) {
        return active.contains(behavior);
    }

    public boolean isIdle() {
        return active.isEmpty();
    }
}
