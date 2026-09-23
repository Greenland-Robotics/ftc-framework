import commands.*;
import visualizer.TreeVisualizer;

/**
 * Combining the built-in commands, with no robot needed.
 *
 * The "robot" here is just a few variables. Running this opens a web page that shows the command
 * tree live: which commands are running, finished or skipped, and what each one printed. Press
 * Step to call commandRunner.update() once, or Play to call it every 20 ms like an OpMode does.
 * On a real robot, AutoBase/TeleOpBase run that loop for you.
 *
 * Run it (see examples/README.md for more ways):
 *     VS Code:         Run and Debug → "Command tree visualizer"
 *     Android Studio:  the "Command tree visualizer" run configuration
 *     Terminal:        ./gradlew :examples:run
 * then open http://localhost:8765.
 */
public class CommandBasics {
    static double armPosition;
    static boolean clawOpen;

    public static void main(String[] args) throws Exception {
        new TreeVisualizer(CommandBasics::routine)
                .watch("armPosition", () -> armPosition)
                .watch("clawOpen", () -> clawOpen)
                .start();
    }

    /** Builds the routine. Restart in the web page calls this again, so it also resets the "robot". */
    static Command routine() {
        armPosition = 0;
        clawOpen = false;

        return new SeriesCommand(
                // Runs once, then the series moves on
                new InstantCommand(() -> {
                    clawOpen = true;
                    log("open claw");
                }),

                // Both branches run at the same time; the parallel ends when both are done
                new ParallelCommand(
                        new MoveArm(1.0),
                        new SeriesCommand(
                                new SleepCommand(500),
                                new InstantCommand(() -> log("500 ms passed while the arm moves"))
                        )
                ),

                // Waits, without blocking the loop, until a condition is true
                new AwaitCommand(() -> armPosition >= 1.0),

                // Picks a branch when it starts, like an if/else
                new SwitchCommand(
                        () -> clawOpen,
                        new InstantCommand(() -> log("claw is open: grab")),
                        new InstantCommand(() -> log("claw is closed: skip"))
                ),

                // Gives up on a command that takes too long
                new TimeoutCommand(new AwaitCommand(() -> false), 1000),
                new InstantCommand(() -> log("done"))
        );
    }

    /** A custom command: moves the arm a little every loop until it reaches the target. */
    static class MoveArm implements Command {
        private final double target;

        MoveArm(double target) {
            this.target = target;
        }

        public void init() {
            log("arm moving to " + target);
        }

        public void loop() {
            armPosition = Math.min(target, armPosition + 0.02);
        }

        public boolean isFinished() {
            if (armPosition >= target) {
                log("arm reached " + target);
                return true;
            }
            return false;
        }
    }

    /** Printed lines show up in the web page, next to the command that printed them. */
    static void log(String message) {
        System.out.println(message);
    }
}
