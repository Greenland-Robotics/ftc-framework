import commands.*;

/**
 * Combining the built-in commands, with no robot needed.
 *
 * The "robot" here is just a few variables, and main() plays the role of the OpMode's main loop:
 * it calls commandRunner.update() every 20 ms. On a real robot, AutoBase/TeleOpBase run that
 * loop for you.
 *
 * Run it from the repository root:
 *     javac -d build/examples framework/src/commands/*.java examples/general/CommandBasics.java
 *     java -cp build/examples CommandBasics
 */
public class CommandBasics {
    static double armPosition = 0;
    static boolean clawOpen = false;
    static final long startTime = System.currentTimeMillis();

    public static void main(String[] args) throws InterruptedException {
        Command routine = new SeriesCommand(
                // Runs once, then the series moves on
                new InstantCommand(() -> {
                    clawOpen = true;
                    log("open claw");
                }),

                // Both branches run at the same time; the parallel ends when both are done
                new ParallelCommand(
                        new MoveArm(1.0),
                        new SeriesCommand(
                                new SleepCommand(100),
                                new InstantCommand(() -> log("100 ms passed while the arm moves"))
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
                new TimeoutCommand(new AwaitCommand(() -> false), 200),
                new InstantCommand(() -> log("done"))
        );

        CommandRunner commandRunner = new CommandRunner(routine);
        commandRunner.start();

        // Stand-in for the OpMode main loop. Never write a loop like this inside a command.
        while (!commandRunner.isFinished()) {
            commandRunner.update();
            Thread.sleep(20);
        }
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
            armPosition = Math.min(target, armPosition + 0.1);
        }

        public boolean isFinished() {
            if (armPosition >= target) {
                log("arm reached " + target);
                return true;
            }
            return false;
        }
    }

    static void log(String message) {
        System.out.printf("%4d ms  %s%n", System.currentTimeMillis() - startTime, message);
    }
}
