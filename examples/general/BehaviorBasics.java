import static behavior.Behaviors.*;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import behavior.runtime.BehaviorRunner;

/**
 * The behavior tree building blocks, running on a laptop with a pretend robot.
 *
 * The "robot" is a few variables, and main() plays the role of the OpMode's main loop: it ticks
 * a BehaviorRunner every 20 ms. On a real robot, AutoBase/TeleOpBase run that loop for you.
 *
 * Run it from the repository root:
 *     javac -d build/examples $(find framework/src -name '*.java') examples/general/BehaviorBasics.java
 *     java -cp build/examples BehaviorBasics
 */
public class BehaviorBasics {
    static double armPosition = 0;
    static int grabAttempts = 0;
    static boolean batteryOk = true;
    static boolean lowering = false;
    static final long startTime = System.currentTimeMillis();

    public static void main(String[] args) throws InterruptedException {
        Node routine = sequence(
                instant(() -> log("start")),

                // Both at once; done when both succeed
                parallel(
                        new MoveArm(1.0),
                        sequence(delay(100), instant(() -> log("100 ms passed while the arm moves")))
                ),

                // Fallback: try to grab (up to 3 tries); if that never works, do something else
                selector(
                        condition(BehaviorBasics::grab).retry(3),
                        instant(() -> log("could not grab: skipping"))
                ),

                // Reactive: the battery check runs every tick and stops the arm if it fails
                reactiveSequence(
                        condition(() -> batteryOk),
                        sequence(
                                instant(() -> {
                                    lowering = true;
                                    log("lowering arm (the battery dies halfway down)");
                                }),
                                new MoveArm(0.0)
                        )
                ).alwaysSucceed(),

                // Give up on something that takes too long
                waitUntil(() -> false).withTimeout(200).alwaysSucceed(),
                instant(() -> log("done"))
        );

        BehaviorRunner runner = new BehaviorRunner();
        runner.run(routine);

        // Stand-in for the OpMode main loop. Never write a loop like this inside a behavior.
        while (!runner.isIdle()) {
            runner.tick();
            Thread.sleep(20);
            if (lowering && armPosition <= 0.5) {
                batteryOk = false; // pretend the battery sagged
            }
        }
    }

    /** Pretend gripper that only works on the second try. */
    static boolean grab() {
        grabAttempts++;
        log("grab attempt " + grabAttempts + (grabAttempts >= 2 ? ": got it" : ": missed"));
        return grabAttempts >= 2;
    }

    /** A custom Action: moves the arm a little each tick until it reaches the target. */
    static class MoveArm extends Action {
        private final double target;

        MoveArm(double target) {
            this.target = target;
        }

        @Override
        protected void start() {
            log("arm moving to " + target);
        }

        @Override
        protected Status update() {
            double step = Math.signum(target - armPosition) * 0.1;
            armPosition = Math.abs(target - armPosition) <= 0.1 ? target : armPosition + step;
            return armPosition == target ? Status.SUCCESS : Status.RUNNING;
        }

        @Override
        protected void end(boolean interrupted) {
            log(interrupted
                    ? String.format("arm stopped at %.1f (halted)", armPosition)
                    : "arm reached " + target);
        }
    }

    static void log(String message) {
        System.out.printf("%4d ms  %s%n", System.currentTimeMillis() - startTime, message);
    }
}
