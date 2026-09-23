package behavior;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/** Returns the scripted statuses in order (repeating the last one) and counts ticks and halts. */
final class ScriptedNode implements Node {
    private final Deque<Status> script;
    private boolean running;
    int ticks;
    int halts;

    private ScriptedNode(Status... statuses) {
        script = new ArrayDeque<>(Arrays.asList(statuses));
    }

    static ScriptedNode returning(Status... statuses) {
        return new ScriptedNode(statuses);
    }

    @Override
    public Status tick() {
        ticks++;
        Status status = script.size() > 1 ? script.poll() : script.peek();
        running = status == Status.RUNNING;
        return status;
    }

    @Override
    public void halt() {
        if (running) {
            running = false;
            halts++;
        }
    }
}
