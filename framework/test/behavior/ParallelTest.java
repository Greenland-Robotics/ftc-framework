package behavior;

import static behavior.Status.FAILURE;
import static behavior.Status.RUNNING;
import static behavior.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import behavior.composite.Parallel;
import behavior.composite.ParallelPolicy;

class ParallelTest {
    @Test
    void allSucceedWaitsForEveryChildAndDoesNotReTickFinishedOnes() {
        ScriptedNode quick = ScriptedNode.returning(SUCCESS);
        ScriptedNode slow = ScriptedNode.returning(RUNNING, SUCCESS);
        Node parallel = new Parallel(ParallelPolicy.ALL_SUCCEED, quick, slow);

        assertEquals(RUNNING, parallel.tick());
        assertEquals(SUCCESS, parallel.tick());
        assertEquals(1, quick.ticks);
    }

    @Test
    void allSucceedFailsFastAndHaltsTheOthers() {
        ScriptedNode running = ScriptedNode.returning(RUNNING);
        Node parallel = new Parallel(ParallelPolicy.ALL_SUCCEED, running, ScriptedNode.returning(FAILURE));

        assertEquals(FAILURE, parallel.tick());
        assertEquals(1, running.halts);
    }

    @Test
    void anySucceedsIsARace() {
        ScriptedNode loser = ScriptedNode.returning(RUNNING);
        Node race = new Parallel(ParallelPolicy.ANY_SUCCEEDS, loser, ScriptedNode.returning(RUNNING, SUCCESS));

        assertEquals(RUNNING, race.tick());
        assertEquals(SUCCESS, race.tick());
        assertEquals(1, loser.halts);
    }

    @Test
    void anySucceedsFailsOnlyWhenEveryChildFails() {
        Node race = new Parallel(ParallelPolicy.ANY_SUCCEEDS, ScriptedNode.returning(FAILURE), ScriptedNode.returning(RUNNING, FAILURE));

        assertEquals(RUNNING, race.tick());
        assertEquals(FAILURE, race.tick());
    }
}
