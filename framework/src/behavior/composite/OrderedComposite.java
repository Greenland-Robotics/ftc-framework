package behavior.composite;

import behavior.Node;
import behavior.Status;

/**
 * Shared engine of sequences and selectors: tick children in order and keep going while they
 * report {@code continueOn}. Sequences continue on SUCCESS (all must succeed); selectors continue
 * on FAILURE (first success wins).
 *
 * <p>A <b>latched</b> composite resumes at the child that was RUNNING. A <b>reactive</b> one
 * starts from the first child every tick, so earlier children (usually conditions) are re-checked
 * and can interrupt a later running child.
 */
abstract class OrderedComposite extends Composite {
    private final Status continueOn;
    private final boolean reactive;
    private int current;

    OrderedComposite(Status continueOn, boolean reactive, Node... children) {
        super(children);
        this.continueOn = continueOn;
        this.reactive = reactive;
    }

    @Override
    public final Status tick() {
        for (int i = reactive ? 0 : current; i < children.size(); i++) {
            Status status = children.get(i).tick();
            if (status == continueOn) {
                continue;
            }
            // RUNNING, or the result that ends this composite: anything after i must stop
            haltChildrenFrom(i + 1);
            current = status == Status.RUNNING ? i : 0;
            return status;
        }
        current = 0;
        return continueOn;
    }

    @Override
    public final void halt() {
        haltChildrenFrom(0);
        current = 0;
    }
}
