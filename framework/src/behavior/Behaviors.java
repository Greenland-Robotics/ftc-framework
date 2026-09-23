package behavior;

import java.util.function.BooleanSupplier;

import behavior.composite.IfElse;
import behavior.composite.Parallel;
import behavior.composite.ParallelPolicy;
import behavior.composite.ReactiveSelector;
import behavior.composite.ReactiveSequence;
import behavior.composite.Selector;
import behavior.composite.Sequence;
import behavior.leaf.Condition;
import behavior.leaf.Delay;
import behavior.leaf.Instant;
import behavior.leaf.WaitUntil;
import behavior.runtime.Clock;

/**
 * Short names for building trees. Add {@code import static behavior.Behaviors.*;} and write:
 * <pre>
 * sequence(
 *     robot.drive.follow(paths.toGoal),
 *     selector(scoreHigh(), scoreLow()),
 *     delay(250))
 * </pre>
 * Decorators are methods on every node: {@code .withTimeout(ms)}, {@code .retry(n)},
 * {@code .repeat(n)}, {@code .repeatForever()}, {@code .invert()}, {@code .alwaysSucceed()},
 * {@code .onlyWhile(condition)}.
 */
public final class Behaviors {
    private Behaviors() {}

    // --- Composites ---

    /** One after another; stops at the first failure. See {@link Sequence}. */
    public static Node sequence(Node... children) {
        return new Sequence(children);
    }

    /** One after another until one succeeds. See {@link Selector}. */
    public static Node selector(Node... children) {
        return new Selector(children);
    }

    /** A sequence that re-checks earlier children every tick. See {@link ReactiveSequence}. */
    public static Node reactiveSequence(Node... children) {
        return new ReactiveSequence(children);
    }

    /** A selector that re-checks earlier children every tick. See {@link ReactiveSelector}. */
    public static Node reactiveSelector(Node... children) {
        return new ReactiveSelector(children);
    }

    /** All at once; succeeds when all succeed, fails when any fails. */
    public static Node parallel(Node... children) {
        return new Parallel(ParallelPolicy.ALL_SUCCEED, children);
    }

    /** All at once; the first to succeed wins and the others are halted. */
    public static Node race(Node... children) {
        return new Parallel(ParallelPolicy.ANY_SUCCEEDS, children);
    }

    /** Picks a branch once, when it starts. See {@link IfElse}. */
    public static Node ifElse(BooleanSupplier condition, Node then, Node otherwise) {
        return new IfElse(condition, then, otherwise);
    }

    /** Runs {@code then} only if {@code condition} is true when it starts; otherwise succeeds. */
    public static Node when(BooleanSupplier condition, Node then) {
        return new IfElse(condition, then, succeed());
    }

    // --- Leaves ---

    /** SUCCESS if {@code check} is true right now, FAILURE if not. */
    public static Node condition(BooleanSupplier check) {
        return new Condition(check);
    }

    /** Waits until {@code check} is true. */
    public static Node waitUntil(BooleanSupplier check) {
        return new WaitUntil(check);
    }

    /** Runs {@code action} once and succeeds. */
    public static Node instant(Runnable action) {
        return new Instant(action);
    }

    /** Waits {@code millis} milliseconds. */
    public static Node delay(long millis) {
        return new Delay(millis, Clock.system());
    }

    /** Succeeds immediately. */
    public static Node succeed() {
        return () -> Status.SUCCESS;
    }

    /** Fails immediately. */
    public static Node fail() {
        return () -> Status.FAILURE;
    }
}
