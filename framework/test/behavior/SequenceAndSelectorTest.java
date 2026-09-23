package behavior;

import static behavior.Status.FAILURE;
import static behavior.Status.RUNNING;
import static behavior.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import behavior.composite.ReactiveSelector;
import behavior.composite.ReactiveSequence;
import behavior.composite.Selector;
import behavior.composite.Sequence;

class SequenceAndSelectorTest {
    @Test
    void sequenceSucceedsWhenEveryChildSucceeds() {
        ScriptedNode first = ScriptedNode.returning(RUNNING, SUCCESS);
        ScriptedNode second = ScriptedNode.returning(SUCCESS);
        Node sequence = new Sequence(first, second);

        assertEquals(RUNNING, sequence.tick());
        assertEquals(SUCCESS, sequence.tick());
        assertEquals(1, second.ticks);
    }

    @Test
    void sequenceStopsAtTheFirstFailure() {
        ScriptedNode later = ScriptedNode.returning(SUCCESS);
        Node sequence = new Sequence(ScriptedNode.returning(FAILURE), later);

        assertEquals(FAILURE, sequence.tick());
        assertEquals(0, later.ticks);
    }

    @Test
    void latchedSequenceDoesNotRecheckEarlierChildren() {
        ScriptedNode check = ScriptedNode.returning(SUCCESS);
        ScriptedNode work = ScriptedNode.returning(RUNNING);
        Node sequence = new Sequence(check, work);

        sequence.tick();
        sequence.tick();
        assertEquals(1, check.ticks);
        assertEquals(2, work.ticks);
    }

    @Test
    void reactiveSequenceHaltsTheRunningChildWhenAConditionFails() {
        ScriptedNode check = ScriptedNode.returning(SUCCESS, FAILURE);
        ScriptedNode work = ScriptedNode.returning(RUNNING);
        Node sequence = new ReactiveSequence(check, work);

        assertEquals(RUNNING, sequence.tick());
        assertEquals(FAILURE, sequence.tick());
        assertEquals(1, work.halts);
    }

    @Test
    void selectorFallsBackUntilAChildSucceeds() {
        ScriptedNode unused = ScriptedNode.returning(SUCCESS);
        Node selector = new Selector(ScriptedNode.returning(FAILURE), ScriptedNode.returning(SUCCESS), unused);

        assertEquals(SUCCESS, selector.tick());
        assertEquals(0, unused.ticks);
    }

    @Test
    void selectorFailsWhenEveryChildFails() {
        Node selector = new Selector(ScriptedNode.returning(FAILURE), ScriptedNode.returning(FAILURE));
        assertEquals(FAILURE, selector.tick());
    }

    @Test
    void reactiveSelectorLetsAHigherPriorityChildTakeOver() {
        ScriptedNode urgent = ScriptedNode.returning(FAILURE, SUCCESS);
        ScriptedNode fallback = ScriptedNode.returning(RUNNING);
        Node selector = new ReactiveSelector(urgent, fallback);

        assertEquals(RUNNING, selector.tick());
        assertEquals(SUCCESS, selector.tick());
        assertEquals(1, fallback.halts);
    }

    @Test
    void haltStopsTheRunningChildAndRestartsFromTheBeginning() {
        ScriptedNode first = ScriptedNode.returning(SUCCESS);
        ScriptedNode second = ScriptedNode.returning(RUNNING);
        Node sequence = new Sequence(first, second);

        sequence.tick();
        sequence.halt();
        sequence.tick();
        assertEquals(1, second.halts);
        assertEquals(2, first.ticks);
    }
}
