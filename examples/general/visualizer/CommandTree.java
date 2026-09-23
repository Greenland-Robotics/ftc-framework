package visualizer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import commands.*;

/**
 * Wraps every command in a tree with a {@link Traced} command that records when it is started,
 * looped and finished, so the web UI can show what the tree is doing.
 *
 * The framework's commands keep their children in private fields, so the children are found
 * (and swapped for their traced wrappers) with reflection. This works for custom commands too,
 * without changing the framework.
 */
final class CommandTree {
    enum Status { PENDING, RUNNING, FINISHED }

    /** One command in the tree and what has happened to it since the last restart. */
    static final class Node {
        final int id;
        final Command command;
        final String label;   // How the parent refers to it: "1", "if true", "else", ...
        final List<Node> children = new ArrayList<>();

        Status status = Status.PENDING;
        int inits;
        int loops;
        long startNanos;
        long endNanos;
        final List<String> output = new ArrayList<>();

        Node(int id, Command command, String label) {
            this.id = id;
            this.command = command;
            this.label = label;
        }

        String type() {
            String name = command.getClass().getSimpleName();
            return name.isEmpty() ? command.getClass().getName() : name;
        }
    }

    /** Receives the calls made to traced commands. */
    interface Listener {
        void enter(Node node);

        void exit();

        /** An exception is passing up through this command. */
        void threw(Node node);
    }

    /** Stands in for a command in its parent and reports every call to the listener. */
    static final class Traced implements Command {
        final Command inner;
        final Node node;
        private final Listener listener;

        Traced(Command inner, Node node, Listener listener) {
            this.inner = inner;
            this.node = node;
            this.listener = listener;
        }

        public void init() {
            node.inits++;
            node.status = Status.RUNNING;
            node.startNanos = System.nanoTime();
            listener.enter(node);
            try {
                inner.init();
            } catch (RuntimeException e) {
                listener.threw(node);
                throw e;
            } finally {
                listener.exit();
            }
        }

        public void loop() {
            node.loops++;
            listener.enter(node);
            try {
                inner.loop();
            } catch (RuntimeException e) {
                listener.threw(node);
                throw e;
            } finally {
                listener.exit();
            }
        }

        public boolean isFinished() {
            boolean finished;
            listener.enter(node);
            try {
                finished = inner.isFinished();
            } catch (RuntimeException e) {
                listener.threw(node);
                throw e;
            } finally {
                listener.exit();
            }
            if (finished && node.status == Status.RUNNING) {
                node.status = Status.FINISHED;
                node.endNanos = System.nanoTime();
            } else if (!finished && node.status == Status.FINISHED) {
                node.status = Status.RUNNING;
            }
            return finished;
        }
    }

    final Traced root;

    private final Listener listener;
    private final Map<Command, Traced> traced = new IdentityHashMap<>();
    private int nextId;

    CommandTree(Command root, Listener listener) {
        this.listener = listener;
        this.root = trace(root, null);
    }

    private Traced trace(Command command, String label) {
        if (command instanceof Traced) {
            return (Traced) command;
        }
        Traced existing = traced.get(command);
        if (existing != null) {
            return existing;   // The same command object used twice: share one node
        }

        Node node = new Node(nextId++, command, label);
        Traced wrapper = new Traced(command, node, listener);
        traced.put(command, wrapper);

        for (Field field : fields(command.getClass())) {
            if (!field.isSynthetic()) {   // Skip e.g. an inner class's reference to its outer object
                traceChildren(command, field, node);
            }
        }
        return wrapper;
    }

    /** Replaces the commands held in one field (directly, in a List or in an array) with traced ones. */
    private void traceChildren(Command parent, Field field, Node node) {
        Object value;
        try {
            field.setAccessible(true);
            value = field.get(parent);
        } catch (RuntimeException | IllegalAccessException e) {
            return;   // Not reachable by reflection (e.g. a JDK class): show it without children
        }

        if (value instanceof Command) {
            Traced child = trace((Command) value, childLabel(parent, field.getName(), -1));
            try {
                field.set(parent, child);
                node.children.add(child.node);
            } catch (RuntimeException | IllegalAccessException e) {
                // Can't swap it in, so it would never update: show the parent without it
            }
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof Command) {
                    Traced child = trace((Command) list.get(i), childLabel(parent, field.getName(), i));
                    list.set(i, child);
                    node.children.add(child.node);
                }
            }
        } else if (value != null && value.getClass().isArray()
                && Command.class.isAssignableFrom(value.getClass().getComponentType())) {
            for (int i = 0; i < Array.getLength(value); i++) {
                Object element = Array.get(value, i);
                if (element instanceof Command) {
                    Traced child = trace((Command) element, childLabel(parent, field.getName(), i));
                    Array.set(value, i, child);
                    node.children.add(child.node);
                }
            }
        }
    }

    private static String childLabel(Command parent, String field, int index) {
        if (parent instanceof SeriesCommand) return String.valueOf(index + 1);
        if (parent instanceof ParallelCommand || parent instanceof CancelCommand) return "";
        if (parent instanceof SwitchCommand) return field.equals("action") ? "if true" : "else";
        if (parent instanceof TimeoutCommand) return "";
        return index < 0 ? field : field + "[" + index + "]";
    }

    /**
     * How the UI lays out a node's children: "series" (top to bottom), "parallel" (side by side),
     * "switch" (two labelled branches), "wrapper" (one child inside) or "group" (unknown).
     */
    static String kind(Node node) {
        Command c = node.command;
        if (c instanceof SeriesCommand) return "series";
        if (c instanceof ParallelCommand || c instanceof CancelCommand) return "parallel";
        if (c instanceof SwitchCommand) return "switch";
        if (c instanceof TimeoutCommand) return "wrapper";
        return node.children.isEmpty() ? "leaf" : "group";
    }

    /** A one-line description of what the command does, from its settings. */
    static String describe(Node node) {
        Command c = node.command;
        if (c instanceof SeriesCommand) return "Runs its commands one after another";
        if (c instanceof ParallelCommand) return "Runs its commands at the same time; done when all are done";
        if (c instanceof CancelCommand) return "Runs its commands at the same time; can be cancelled";
        if (c instanceof InstantCommand) return "Runs its action once, on its first loop()";
        if (c instanceof SleepCommand) return "Waits " + millis(read(c, "targetTime")) + " (by the clock, not by ticks)";
        if (c instanceof TimeoutCommand) return "Stops waiting for its command after " + millis(read(c, "timeoutMillis"));
        if (c instanceof AwaitCommand) {
            return Boolean.TRUE.equals(read(c, "useTimeout"))
                    ? "Waits until its condition is true, or " + millis(read(c, "timeoutDuration"))
                    : "Waits until its condition is true";
        }
        if (c instanceof SwitchCommand) {
            if (node.inits == 0) return "Checks its condition once, in init(), and runs that branch";
            return "Condition was " + read(c, "eval") + " at init()";
        }
        return settings(c);
    }

    /** For a SwitchCommand that has started: the branch it chose not to run, or null. */
    static Node skippedBranch(Node node) {
        if (!(node.command instanceof SwitchCommand) || node.inits == 0) return null;
        boolean eval = Boolean.TRUE.equals(read(node.command, "eval"));
        for (Node child : node.children) {
            if (child.label.equals(eval ? "else" : "if true")) return child;
        }
        return null;
    }

    /** A custom command's simple settings, like "target = 1.0". */
    private static String settings(Command command) {
        StringBuilder text = new StringBuilder();
        for (Field field : fields(command.getClass())) {
            Class<?> type = field.getType();
            if (!(type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type)
                    || type == Boolean.class) || field.isSynthetic()) {
                continue;
            }
            try {
                field.setAccessible(true);
                if (text.length() > 0) text.append(", ");
                text.append(field.getName()).append(" = ").append(field.get(command));
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

    private static String millis(Object value) {
        if (!(value instanceof Number)) return "?";
        double ms = ((Number) value).doubleValue();
        return (ms == Math.rint(ms) ? String.valueOf((long) ms) : String.valueOf(ms)) + " ms";
    }
}
