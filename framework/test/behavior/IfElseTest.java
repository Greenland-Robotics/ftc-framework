package behavior;

import static behavior.Status.RUNNING;
import static behavior.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class IfElseTest {
    @Test
    void choosesABranchOnceAndFinishesIt() {
        AtomicBoolean condition = new AtomicBoolean(true);
        ScriptedNode then = ScriptedNode.returning(RUNNING, SUCCESS);
        ScriptedNode otherwise = ScriptedNode.returning(SUCCESS);
        Node ifElse = Behaviors.ifElse(condition::get, then, otherwise);

        assertEquals(RUNNING, ifElse.tick());
        condition.set(false); // ignored until the next run
        assertEquals(SUCCESS, ifElse.tick());
        assertEquals(0, otherwise.ticks);

        assertEquals(SUCCESS, ifElse.tick());
        assertEquals(1, otherwise.ticks);
    }

    @Test
    void whenSucceedsWithoutRunningAnythingIfTheConditionIsFalse() {
        ScriptedNode then = ScriptedNode.returning(SUCCESS);
        assertEquals(SUCCESS, Behaviors.when(() -> false, then).tick());
        assertEquals(0, then.ticks);
    }
}
