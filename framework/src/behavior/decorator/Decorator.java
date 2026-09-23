package behavior.decorator;

import java.util.Objects;

import behavior.Node;

/** A node that wraps exactly one child and changes how it runs or what it reports. */
public abstract class Decorator implements Node {
    protected final Node child;

    protected Decorator(Node child) {
        this.child = Objects.requireNonNull(child, "child");
    }

    @Override
    public void halt() {
        child.halt();
    }
}
