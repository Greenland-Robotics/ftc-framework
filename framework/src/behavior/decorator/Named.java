package behavior.decorator;

import java.util.Objects;

import behavior.Node;
import behavior.Status;

/**
 * Gives a node a name, like "Intake button held?", for telemetry and the behavior tree
 * visualizer. It changes nothing about how the node runs.
 */
public final class Named extends Decorator {
    private final String name;

    public Named(Node child, String name) {
        super(child);
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() {
        return name;
    }

    @Override
    public Status tick() {
        return child.tick();
    }

    @Override
    public String toString() {
        return name;
    }
}
