package behavior.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import behavior.Node;
import behavior.input.EdgeDetector.Edge;
import behavior.runtime.BehaviorRunner;

/**
 * Connects buttons to behaviors. Declare bindings once; {@link #update()} (called every loop by
 * TeleOpBase) watches the buttons and starts or halts behaviors on the {@link BehaviorRunner}.
 * <pre>
 * controls.onPress(() -> gamepad2.a, robot.intake.collect());
 * controls.whileHeld(() -> gamepad2.right_bumper, robot.intake.run());
 * </pre>
 */
public final class Bindings {
    private final BehaviorRunner runner;
    private final List<Binding> bindings = new ArrayList<>();

    public Bindings(BehaviorRunner runner) {
        this.runner = Objects.requireNonNull(runner, "runner");
    }

    /** Starts {@code behavior} when the button is pressed; it runs until it finishes. */
    public void onPress(BooleanSupplier button, Node behavior) {
        bind(button, edge -> {
            if (edge == Edge.PRESSED) {
                runner.run(behavior);
            }
        });
    }

    /** Runs {@code behavior} while the button is held and halts it when released. */
    public void whileHeld(BooleanSupplier button, Node behavior) {
        bind(button, edge -> {
            if (edge == Edge.PRESSED) {
                runner.run(behavior);
            } else if (edge == Edge.RELEASED) {
                runner.cancel(behavior);
            }
        });
    }

    /** Each press starts {@code behavior} if it isn't running, or halts it if it is. */
    public void toggleOnPress(BooleanSupplier button, Node behavior) {
        bind(button, edge -> {
            if (edge != Edge.PRESSED) {
                return;
            }
            if (runner.isRunning(behavior)) {
                runner.cancel(behavior);
            } else {
                runner.run(behavior);
            }
        });
    }

    /** Checks every bound button once; call once per loop. */
    public void update() {
        for (Binding binding : bindings) {
            binding.reaction.accept(binding.detector.update());
        }
    }

    private void bind(BooleanSupplier button, Consumer<Edge> reaction) {
        bindings.add(new Binding(new EdgeDetector(button), reaction));
    }

    private static final class Binding {
        final EdgeDetector detector;
        final Consumer<Edge> reaction;

        Binding(EdgeDetector detector, Consumer<Edge> reaction) {
            this.detector = detector;
            this.reaction = reaction;
        }
    }
}
