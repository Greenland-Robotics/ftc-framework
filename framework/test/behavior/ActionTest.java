package behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ActionTest {
    /** Runs for two ticks, recording its lifecycle. */
    static final class TwoTickAction extends Action {
        final StringBuilder log = new StringBuilder();
        private int updates;

        @Override protected void start() { log.append("start "); updates = 0; }
        @Override protected Status update() { log.append("update "); return ++updates < 2 ? Status.RUNNING : Status.SUCCESS; }
        @Override protected void end(boolean interrupted) { log.append(interrupted ? "halted " : "ended "); }
    }

    @Test
    void runsStartUpdateEndInOrder() {
        TwoTickAction action = new TwoTickAction();
        assertEquals(Status.RUNNING, action.tick());
        assertEquals(Status.SUCCESS, action.tick());
        assertEquals("start update update ended ", action.log.toString());
    }

    @Test
    void restartsAfterFinishing() {
        TwoTickAction action = new TwoTickAction();
        action.tick();
        action.tick();
        action.tick();
        assertEquals("start update update ended start update ", action.log.toString());
    }

    @Test
    void haltEndsARunningActionOnce() {
        TwoTickAction action = new TwoTickAction();
        action.halt(); // not running: nothing happens
        action.tick();
        action.halt();
        action.halt();
        assertEquals("start update halted ", action.log.toString());
    }
}
