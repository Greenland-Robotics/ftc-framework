package behavior;

import static behavior.Status.FAILURE;
import static behavior.Status.RUNNING;
import static behavior.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import behavior.decorator.Timeout;
import behavior.leaf.Delay;

class DecoratorTest {
    @Test
    void timeoutFailsAndHaltsAChildThatTakesTooLong() {
        FakeClock clock = new FakeClock();
        ScriptedNode slow = ScriptedNode.returning(RUNNING);
        Node timeout = new Timeout(slow, 100, clock);

        assertEquals(RUNNING, timeout.tick());
        clock.advance(100);
        assertEquals(FAILURE, timeout.tick());
        assertEquals(1, slow.halts);
    }

    @Test
    void timeoutRestartsItsTimerEachRun() {
        FakeClock clock = new FakeClock();
        Node timeout = new Timeout(new Delay(50, clock), 100, clock);

        timeout.tick();
        clock.advance(60);
        assertEquals(SUCCESS, timeout.tick());
        timeout.tick(); // second run starts at t=60
        clock.advance(60);
        assertEquals(SUCCESS, timeout.tick());
    }

    @Test
    void retryTriesAgainAfterFailures() {
        ScriptedNode flaky = ScriptedNode.returning(FAILURE, FAILURE, SUCCESS);
        Node retry = flaky.retry(3);

        assertEquals(RUNNING, retry.tick());
        assertEquals(RUNNING, retry.tick());
        assertEquals(SUCCESS, retry.tick());
    }

    @Test
    void retryGivesUpAfterTheLastAttempt() {
        Node retry = ScriptedNode.returning(FAILURE).retry(2);

        assertEquals(RUNNING, retry.tick());
        assertEquals(FAILURE, retry.tick());
    }

    @Test
    void repeatRunsTheChildTheRequestedNumberOfTimes() {
        ScriptedNode child = ScriptedNode.returning(SUCCESS);
        Node repeat = child.repeat(3);

        assertEquals(RUNNING, repeat.tick());
        assertEquals(RUNNING, repeat.tick());
        assertEquals(SUCCESS, repeat.tick());
        assertEquals(3, child.ticks);
    }

    @Test
    void invertAndAlwaysSucceedChangeTheResult() {
        assertEquals(FAILURE, ScriptedNode.returning(SUCCESS).invert().tick());
        assertEquals(SUCCESS, ScriptedNode.returning(FAILURE).invert().tick());
        assertEquals(SUCCESS, ScriptedNode.returning(FAILURE).alwaysSucceed().tick());
        assertEquals(RUNNING, ScriptedNode.returning(RUNNING).alwaysSucceed().tick());
    }

    @Test
    void onlyWhileHaltsTheChildWhenTheConditionStopsHolding() {
        AtomicBoolean safe = new AtomicBoolean(true);
        ScriptedNode work = ScriptedNode.returning(RUNNING);
        Node guarded = work.onlyWhile(safe::get);

        assertEquals(RUNNING, guarded.tick());
        safe.set(false);
        assertEquals(FAILURE, guarded.tick());
        assertEquals(1, work.halts);
    }
}
