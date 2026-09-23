package visualizer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import behavior.Node;
import behavior.Status;
import behavior.composite.IfElse;
import behavior.composite.Parallel;
import behavior.composite.ParallelPolicy;
import behavior.composite.ReactiveSelector;
import behavior.composite.ReactiveSequence;
import behavior.composite.Selector;
import behavior.composite.Sequence;
import behavior.decorator.AlwaysSucceed;
import behavior.decorator.Decorator;
import behavior.decorator.Guard;
import behavior.decorator.Invert;
import behavior.decorator.Repeat;
import behavior.decorator.Retry;
import behavior.decorator.Timeout;
import behavior.leaf.Delay;
import behavior.leaf.Instant;
import behavior.leaf.WaitUntil;

/**
 * Wraps every node in a tree with a {@link Traced} node that records each tick and halt, so the
 * web UI can show what the tree is doing.
 *
 * The framework's nodes keep their children in private fields, so the children are found (and
 * swapped for their traced wrappers) with reflection. This works for custom nodes too, without
 * changing the framework.
 */
final class BehaviorTree {
    /** One node in the tree and what has happened to it since the last restart. */
    static final class Info {
        final int id;
        final Node node;
        final String label;   // How the parent refers to it: "1", "then", "else", ...
        final List<Info> children = new ArrayList<>();

        String state = "idle";   // idle, running, success, failure or halted
        int ticks;
        int runs;                 // Times it started again after being done (or for the first time)
        int halts;
        int lastTick = -1;        // Runner tick in which it was last ticked
        long startNanos;
        long endNanos;
        final List<String> output = new ArrayList<>();

        Info(int id, Node node, String label) {
            this.id = id;
            this.node = node;
            this.label = label;
        }

        String type() {
            Class<?> type = node.getClass();
            if (type.isSynthetic() || type.getName().contains("$$Lambda")) return "Node (lambda)";
            String name = type.getSimpleName();
            if (node instanceof Parallel && policy((Parallel) node) == ParallelPolicy.ANY_SUCCEEDS) {
                return "Parallel (race)";
            }
            return name.isEmpty() ? type.getName() : name;
        }
    }

    /** Receives the calls made to traced nodes. */
    interface Listener {
        /** The runner tick currently being run, to mark which nodes were ticked in it. */
        int currentTick();

        void enter(Info info);

        void exit();

        /** An exception is passing up through this node. */
        void threw(Info info);
    }

    /** Stands in for a node in its parent and reports every call to the listener. */
    static final class Traced implements Node {
        final Node inner;
        final Info info;
        private final Listener listener;

        Traced(Node inner, Info info, Listener listener) {
            this.inner = inner;
            this.info = info;
            this.listener = listener;
        }

        @Override
        public Status tick() {
            if (!info.state.equals("running")) {
                info.runs++;
                info.startNanos = System.nanoTime();
            }
            info.ticks++;
            info.lastTick = listener.currentTick();
            Status status;
            listener.enter(info);
            try {
                status = inner.tick();
            } catch (RuntimeException e) {
                listener.threw(info);
                throw e;
            } finally {
                listener.exit();
            }
            info.state = status.name().toLowerCase(java.util.Locale.ROOT);
            if (status.isDone()) info.endNanos = System.nanoTime();
            return status;
        }

        @Override
        public void halt() {
            // Parents halt children that aren't running too (it does nothing), so only count real halts
            if (info.state.equals("running")) {
                info.state = "halted";
                info.halts++;
                info.endNanos = System.nanoTime();
            }
            listener.enter(info);
            try {
                inner.halt();
            } catch (RuntimeException e) {
                listener.threw(info);
                throw e;
            } finally {
                listener.exit();
            }
        }
    }

    final Traced root;

    private final Listener listener;
    private final Map<Node, Traced> traced = new IdentityHashMap<>();
    private int nextId;

    BehaviorTree(Node root, Listener listener) {
        this.listener = listener;
        this.root = trace(root, null);
    }

    private Traced trace(Node node, String label) {
        if (node instanceof Traced) {
            return (Traced) node;
        }
        Traced existing = traced.get(node);
        if (existing != null) {
            return existing;   // The same node object used twice: share one entry
        }

        Info info = new Info(nextId++, node, label);
        Traced wrapper = new Traced(node, info, listener);
        traced.put(node, wrapper);

        for (Field field : fields(node.getClass())) {
            if (!field.isSynthetic()) {   // Skip e.g. an inner class's reference to its outer object
                traceChildren(node, field, info);
            }
        }
        return wrapper;
    }

    /** Replaces the nodes held in one field (directly, in a List or in an array) with traced ones. */
    private void traceChildren(Node parent, Field field, Info info) {
        Object value;
        try {
            field.setAccessible(true);
            value = field.get(parent);
        } catch (RuntimeException | IllegalAccessException e) {
            return;   // Not reachable by reflection (e.g. a JDK class): show it without children
        }

        if (value instanceof Node) {
            Traced child = trace((Node) value, childLabel(parent, field.getName(), -1));
            if (set(field, parent, child)) info.children.add(child.info);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            List<Object> replaced = new ArrayList<>(list);
            List<Info> found = new ArrayList<>();
            for (int i = 0; i < replaced.size(); i++) {
                if (replaced.get(i) instanceof Node) {
                    Traced child = trace((Node) replaced.get(i), childLabel(parent, field.getName(), i));
                    replaced.set(i, child);
                    found.add(child.info);
                }
            }
            if (found.isEmpty()) return;
            boolean swapped;
            try {
                for (int i = 0; i < list.size(); i++) list.set(i, replaced.get(i));
                swapped = true;
            } catch (UnsupportedOperationException e) {
                // Composites keep an unmodifiable list, so put a new one in its place
                swapped = set(field, parent, Collections.unmodifiableList(replaced));
            }
            if (swapped) info.children.addAll(found);
        } else if (value != null && value.getClass().isArray()
                && Node.class.isAssignableFrom(value.getClass().getComponentType())) {
            for (int i = 0; i < Array.getLength(value); i++) {
                Object element = Array.get(value, i);
                if (element instanceof Node) {
                    Traced child = trace((Node) element, childLabel(parent, field.getName(), i));
                    Array.set(value, i, child);
                    info.children.add(child.info);
                }
            }
        }
    }

    /** Sets a (usually final) field; if that isn't allowed, the child is left untraced. */
    private static boolean set(Field field, Object target, Object value) {
        try {
            field.set(target, value);
            return true;
        } catch (RuntimeException | IllegalAccessException e) {
            return false;
        }
    }

    private static String childLabel(Node parent, String field, int index) {
        if (isOrdered(parent)) return String.valueOf(index + 1);
        if (parent instanceof Parallel || parent instanceof Decorator) return "";
        if (parent instanceof IfElse) return field.equals("then") ? "then" : "else";
        return index < 0 ? field : field + "[" + index + "]";
    }

    private static boolean isOrdered(Node node) {
        return node instanceof Sequence || node instanceof Selector
                || node instanceof ReactiveSequence || node instanceof ReactiveSelector;
    }

    /**
     * How the UI lays out a node's children: "series" (top to bottom), "parallel" (side by side),
     * "switch" (two labelled branches), "wrapper" (one child inside), "group" (unknown) or "leaf".
     */
    static String kind(Info info) {
        Node n = info.node;
        if (isOrdered(n)) return "series";
        if (n instanceof Parallel) return "parallel";
        if (n instanceof IfElse) return "switch";
        if (n instanceof Decorator) return "wrapper";
        return info.children.isEmpty() ? "leaf" : "group";
    }

    /** A one-line description of what the node does, from its settings. */
    static String describe(Info info) {
        Node n = info.node;
        if (n instanceof Sequence) return "One after another; stops at the first failure";
        if (n instanceof Selector) return "Tries each in turn until one succeeds";
        if (n instanceof ReactiveSequence) return "A sequence that re-checks from the first child every tick";
        if (n instanceof ReactiveSelector) return "A selector that re-checks from the first child every tick";
        if (n instanceof Parallel) {
            return policy((Parallel) n) == ParallelPolicy.ANY_SUCCEEDS
                    ? "All at once; the first to succeed wins and the rest are halted"
                    : "All at once; succeeds when all succeed, fails when any fails";
        }
        if (n instanceof IfElse) return "Checks its condition once, when it starts, and runs that branch";
        if (n instanceof Timeout) return "Fails and halts its child after " + read(n, "millis") + " ms";
        if (n instanceof Retry) {
            return "Tries again after a failure, up to " + read(n, "attempts") + " tries"
                    + " (" + read(n, "failures") + " failed so far)";
        }
        if (n instanceof Repeat) {
            Object times = read(n, "times");
            return (Integer.valueOf(Repeat.FOREVER).equals(times) ? "Runs again after each success, forever"
                    : "Runs its child " + times + " times") + " (" + read(n, "successes") + " done so far)";
        }
        if (n instanceof Invert) return "Swaps SUCCESS and FAILURE";
        if (n instanceof AlwaysSucceed) return "Reports SUCCESS even if its child fails";
        if (n instanceof Guard) return "Runs its child only while its condition holds";
        if (n instanceof Instant) return "Runs its code once and succeeds";
        if (n instanceof behavior.leaf.Condition) return "SUCCESS if its check is true right now, FAILURE if not";
        if (n instanceof WaitUntil) return "RUNNING until its check is true";
        if (n instanceof Delay) return "Waits " + read(n, "millis") + " ms (by the clock, not by ticks)";
        if (info.type().equals("Node (lambda)")) return "A node written as a lambda";
        return settings(n);
    }

    private static ParallelPolicy policy(Parallel parallel) {
        Object policy = read(parallel, "policy");
        return policy instanceof ParallelPolicy ? (ParallelPolicy) policy : null;
    }

    /** A custom node's simple settings, like "target = 1.0". Skips the framework's own fields. */
    private static String settings(Node node) {
        StringBuilder text = new StringBuilder();
        for (Field field : fields(node.getClass())) {
            Class<?> type = field.getType();
            if (!(type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type)
                    || type == Boolean.class) || field.isSynthetic()
                    || field.getDeclaringClass().getName().startsWith("behavior.")) {
                continue;
            }
            try {
                field.setAccessible(true);
                if (text.length() > 0) text.append(", ");
                text.append(field.getName()).append(" = ").append(field.get(node));
            } catch (RuntimeException | IllegalAccessException ignored) {
                // Leave it out
            }
        }
        return text.toString();
    }

    private static List<Field> fields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) fields.add(field);
            }
        }
        return fields;
    }

    private static Object read(Object target, String name) {
        for (Field field : fields(target.getClass())) {
            if (field.getName().equals(name)) {
                try {
                    field.setAccessible(true);
                    return field.get(target);
                } catch (RuntimeException | IllegalAccessException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
