package behavior;

import java.util.function.BooleanSupplier;

import behavior.decorator.AlwaysSucceed;
import behavior.decorator.Guard;
import behavior.decorator.Invert;
import behavior.decorator.Repeat;
import behavior.decorator.Retry;
import behavior.decorator.Timeout;
import behavior.runtime.Clock;

/**
 * The one building block of a behavior tree. Leaves do work, composites and decorators
 * decide which children run. Every node follows the same contract, so any node can go
 * anywhere another node can:
 * <ul>
 *   <li>{@link #tick()} is called once per loop while the node is active and must return quickly
 *       (never loop or sleep inside it). A node that finished starts over on its next tick.</li>
 *   <li>{@link #halt()} is called when a parent abandons the node while it is RUNNING. It must be
 *       safe to call at any time, and must stop anything the node started (motors, paths).</li>
 * </ul>
 * The default methods wrap this node in a decorator, e.g. {@code intake.run().withTimeout(2000)}.
 */
public interface Node {
    Status tick();

    default void halt() {}

    /** Fails if not finished within {@code millis}; halts this node when time runs out. */
    default Node withTimeout(long millis) {
        return new Timeout(this, millis, Clock.system());
    }

    /** Tries again after a failure, up to {@code attempts} tries in total. */
    default Node retry(int attempts) {
        return new Retry(this, attempts);
    }

    /** Runs again after each success, {@code times} successes in total. */
    default Node repeat(int times) {
        return new Repeat(this, times);
    }

    /** Runs again after each success until it fails or is halted. */
    default Node repeatForever() {
        return new Repeat(this, Repeat.FOREVER);
    }

    /** Swaps SUCCESS and FAILURE. */
    default Node invert() {
        return new Invert(this);
    }

    /** Reports SUCCESS even when this node fails, e.g. to keep a sequence going. */
    default Node alwaysSucceed() {
        return new AlwaysSucceed(this);
    }

    /** Runs only while {@code condition} holds; halts and fails as soon as it doesn't. */
    default Node onlyWhile(BooleanSupplier condition) {
        return new Guard(condition, this);
    }
}
