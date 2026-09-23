package behavior.composite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import behavior.Node;

/** A node with an ordered list of children. */
public abstract class Composite implements Node {
    protected final List<Node> children;

    protected Composite(Node... children) {
        List<Node> list = new ArrayList<>();
        for (Node child : children) {
            list.add(Objects.requireNonNull(child, "child"));
        }
        this.children = Collections.unmodifiableList(list);
    }

    /** Halts every child from {@code index} on. Halting a child that isn't running does nothing. */
    protected final void haltChildrenFrom(int index) {
        for (int i = index; i < children.size(); i++) {
            children.get(i).halt();
        }
    }
}
