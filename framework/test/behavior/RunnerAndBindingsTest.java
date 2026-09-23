package behavior;

import static behavior.Status.RUNNING;
import static behavior.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import behavior.input.Bindings;
import behavior.runtime.BehaviorRunner;

class RunnerAndBindingsTest {
    private final BehaviorRunner runner = new BehaviorRunner();
    private final AtomicBoolean button = new AtomicBoolean();

    @Test
    void runnerTicksUntilTheBehaviorFinishes() {
        ScriptedNode behavior = ScriptedNode.returning(RUNNING, SUCCESS);
        runner.run(behavior);
        runner.run(behavior); // already running: ignored

        runner.tick();
        runner.tick();
        runner.tick();
        assertEquals(2, behavior.ticks);
        assertTrue(runner.isIdle());
    }

    @Test
    void cancelHaltsARunningBehavior() {
        ScriptedNode behavior = ScriptedNode.returning(RUNNING);
        runner.run(behavior);
        runner.tick();
        runner.cancel(behavior);

        assertEquals(1, behavior.halts);
        assertFalse(runner.isRunning(behavior));
    }

    @Test
    void onPressStartsTheBehaviorOncePerPress() {
        Bindings bindings = new Bindings(runner);
        ScriptedNode behavior = ScriptedNode.returning(RUNNING);
        bindings.onPress(button::get, behavior);

        button.set(true);
        bindings.update();
        bindings.update(); // still held: no second start
        assertTrue(runner.isRunning(behavior));

        button.set(false);
        bindings.update();
        assertTrue(runner.isRunning(behavior)); // keeps running after release
    }

    @Test
    void whileHeldHaltsTheBehaviorOnRelease() {
        Bindings bindings = new Bindings(runner);
        ScriptedNode behavior = ScriptedNode.returning(RUNNING);
        bindings.whileHeld(button::get, behavior);

        button.set(true);
        bindings.update();
        runner.tick();
        button.set(false);
        bindings.update();

        assertFalse(runner.isRunning(behavior));
        assertEquals(1, behavior.halts);
    }

    @Test
    void toggleStartsAndStopsOnAlternatePresses() {
        Bindings bindings = new Bindings(runner);
        ScriptedNode behavior = ScriptedNode.returning(RUNNING);
        bindings.toggleOnPress(button::get, behavior);

        press(bindings);
        assertTrue(runner.isRunning(behavior));
        runner.tick();
        press(bindings);
        assertFalse(runner.isRunning(behavior));
    }

    private void press(Bindings bindings) {
        button.set(true);
        bindings.update();
        button.set(false);
        bindings.update();
    }
}
