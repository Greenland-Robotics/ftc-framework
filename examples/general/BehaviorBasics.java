import static behavior.Behaviors.*;

import java.util.Arrays;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import behavior.runtime.BehaviorRunner;
import visualizer.TreeVisualizer;

/**
 * The behavior tree building blocks, running on a laptop with a pretend robot.
 *
 * The "robot" is a few variables. Running this opens a web page that shows the tree live: which
 * nodes are ticked, what each returned (RUNNING, SUCCESS, FAILURE), which were halted, and what
 * each printed. Press Step to call runner.tick() once, or Play to tick every 20 ms like an OpMode.
 * On a real robot, AutoBase/TeleOpBase run that loop for you.
 *
 * Run it (see examples/README.md for more ways):
 *     VS Code:         Run and Debug → "Behavior tree visualizer"
 *     Android Studio:  the "Behavior tree visualizer" run configuration
 *     Terminal:        ./gradlew :examples:run
 * then open http://localhost:8765. Pass --console to just print, without the web page.
 */
public class BehaviorBasics {
    static double armPosition;
    static int grabAttempts;
    static boolean batteryOk;
    static boolean lowering;

    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains("--console")) {
            runInConsole();
            return;
        }
        new TreeVisualizer(BehaviorBasics::routine)
                .afterEachTick(BehaviorBasics::simulate)
                .watch("armPosition", () -> armPosition)
                .watch("grabAttempts", () -> grabAttempts)
                .watch("batteryOk", () -> batteryOk)
                .start();
    }

    /** Builds the tree. Restart in the web page calls this again, so it also resets the "robot". */
    static Node routine() {
        armPosition = 0;
        grabAttempts = 0;
        batteryOk = true;
        lowering = false;

        return sequence(
                instant(() -> log("start")),

                // Both at once; done when both succeed
                parallel(
                        new MoveArm(1.0),
                        sequence(delay(500), instant(() -> log("500 ms passed while the arm moves")))
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
                waitUntil(() -> false).withTimeout(1000).alwaysSucceed(),
                instant(() -> log("done"))
        );
    }

    /** Pretend physics, run after every tick: the battery sags once the arm is halfway down. */
    static void simulate() {
        if (lowering && armPosition <= 0.5) {
            batteryOk = false;
        }
    }

    /** The same tree without the web page. main() plays the role of the OpMode's main loop. */
    static void runInConsole() throws InterruptedException {
        BehaviorRunner runner = new BehaviorRunner();
        runner.run(routine());

        // Stand-in for the OpMode main loop. Never write a loop like this inside a behavior.
        while (!runner.isIdle()) {
            runner.tick();
            simulate();
            Thread.sleep(20);
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
            double step = Math.signum(target - armPosition) * 0.05;
            armPosition = Math.abs(target - armPosition) <= 0.05 ? target : armPosition + step;
            return armPosition == target ? Status.SUCCESS : Status.RUNNING;
        }

        @Override
        protected void end(boolean interrupted) {
            log(interrupted
                    ? String.format("arm stopped at %.2f (halted)", armPosition)
                    : "arm reached " + target);
        }
    }

    /** Printed lines show up in the web page, next to the node that printed them. */
    static void log(String message) {
        System.out.println(message);
    }
}
