package tunedemo;

import static behavior.Behaviors.delay;
import static behavior.Behaviors.sequence;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import behavior.Node;
import behavior.leaf.Delay;
import behavior.runtime.Clock;
import sdk.hardware.SimMotor;
import sdk.parts.Arm;
import sdk.parts.Flywheel;
import sdk.parts.Lift;
import sdk.tuning.Tunable;
import sdk.tuning.TuneReport;
import subsystem.Subsystem;
import util.Log;
import visualizer.Json;
import visualizer.TreeVisualizer;

/**
 * Watch the sdk's auto-tuner tune a simulated lift, arm or flywheel, then show off the gains it
 * found with a few test moves. It is the same code the Tune OpMode runs on the robot; only the
 * motor is pretend ({@link SimMotor}: inertia, gravity, friction, hard stops).
 *
 * Run it (see examples/README.md): VS Code "Tuning demo", Android Studio "Tuning demo", or
 * ./gradlew :examples:tune, then open http://localhost:8765 and press Play. Pick the mechanism
 * from the drop-down. The console prints the tuner's report as it goes.
 */
public final class TuneDemo {
    private static final String LIFT = "Lift (gravity, friction)";
    private static final String ARM = "Arm (gravity changes with angle)";
    private static final String FLYWHEEL = "Flywheel";
    private static final int HISTORY = 300;   // 6 s of loops on the graph

    private static String which = LIFT;
    private static long now;
    private static SimMotor motor;
    private static Subsystem part;
    private static Tunable tunable;
    private static TuneReport report;
    private static final Deque<double[]> history = new ArrayDeque<>();

    private TuneDemo() {}

    public static void main(String[] args) throws Exception {
        TreeVisualizer visualizer = new TreeVisualizer();
        setUp(visualizer);
        visualizer.start();
    }

    /** Puts the tuning demo in the visualizer: the tree, the picture, the drop-down and the values to watch. */
    public static void setUp(TreeVisualizer visualizer) {
        Log.setSink(Log.STANDARD_OUT);
        which = LIFT;
        visualizer
                .routine(TuneDemo::newRun)
                .afterEachTick(TuneDemo::simulate)
                .scene(TuneDemo::draw)
                .setting("Mechanism", value -> which = value, LIFT, ARM, FLYWHEEL)
                .watch("Now → target", () -> String.format(Locale.ROOT, "%.0f → %.0f", tunable.measured(), tunable.setpoint()))
                .watch("Motor power", () -> String.format(Locale.ROOT, "%.2f", motor.power()))
                .watch("Gains", () -> tunable.codeLine())
                .watch("Tuner says", () -> report.lines().isEmpty() ? "" : report.lines().get(report.lines().size() - 1));
    }

    /** A fresh mechanism with bad gains, and the tree: auto-tune, then test moves. Restart calls this again. */
    static Node newRun() {
        now = 0;
        history.clear();
        Clock clock = () -> now;
        report = new TuneReport(which.split(" ")[0].toLowerCase(Locale.ROOT));
        Node demo;
        if (which.equals(FLYWHEEL)) {
            motor = new SimMotor(2800).lag(0.3).friction(0.06);   // 28 ticks per turn: 6000 rpm
            Flywheel wheel = new Flywheel(motor, 28, clock).maxRpm(9000).gain(0);
            part = wheel;
            tunable = wheel;
            demo = sequence(wheel.spinUp(4500), new Delay(1500, clock), wheel.spinUp(3000), new Delay(1500, clock), wheel.stop())
                    .named("Spin at 4500, then 3000 rpm");
        } else if (which.equals(ARM)) {
            motor = new SimMotor(1600).lag(0.1).friction(0.03).travel(0, 600)
                    .gravity(ticks -> 0.18 * Math.cos(Math.toRadians(ticks / 4 - 40)));
            Arm arm = new Arm(motor, 4, clock).zeroAt(-40).limits(-40, 110).gains(0.001, 0);
            part = arm;
            tunable = arm;
            demo = sequence(arm.goTo(90), new Delay(800, clock), arm.goTo(0), new Delay(800, clock), arm.goTo(45))
                    .named("Swing to 90°, 0°, 45°");
        } else {
            motor = new SimMotor(2800).lag(0.08).gravity(0.12).friction(0.04).travel(0, 3000);
            Lift lift = new Lift(motor, clock).limits(0, 2900).gains(0.0001, 0);
            part = lift;
            tunable = lift;
            demo = sequence(lift.goTo(2500), new Delay(800, clock), lift.goTo(400), new Delay(800, clock), lift.goTo(1500))
                    .named("Move to 2500, 400, 1500");
        }
        return sequence(tunable.autoTune(report).named("Auto-tune"), demo).named("Tune, then try it");
    }

    /** After each tick: the pretend world moves one 20 ms loop, then the part reads and drives (periodic). */
    static void simulate() {
        motor.step(0.02);
        now += 20;
        part.periodic();
        history.addLast(new double[] {tunable.measured(), tunable.setpoint()});
        while (history.size() > HISTORY) history.removeFirst();
    }

    // ---- The picture: the mechanism on the left, a graph of the last 6 s on the right ----

    private static String draw() {
        Json json = new Json();
        json.begin().field("width", 200).field("height", 100).name("shapes").beginArray();
        double low = which.equals(FLYWHEEL) ? 0 : which.equals(ARM) ? -40 : 0;
        double high = which.equals(FLYWHEEL) ? 7000 : which.equals(ARM) ? 110 : 3000;
        drawMechanism(json, low, high);
        drawGraph(json, low, high);
        json.endArray().end();
        return json.toString();
    }

    private static void drawMechanism(Json json, double low, double high) {
        double value = tunable.measured();
        if (which.equals(FLYWHEEL)) {
            circle(json, 25, 50, 18, "board", "ink", 0.8);
            double turn = (motor.position() / 28 * 2 * Math.PI) / 40;   // Slowed down so you can see it turn
            line(json, "ink", 1.2, 25, 50, 25 + 16 * Math.cos(turn), 50 + 16 * Math.sin(turn));
            text(json, 25, 22, String.format(Locale.ROOT, "%.0f rpm", value), 4.5);
        } else if (which.equals(ARM)) {
            double angle = Math.toRadians(value);
            circle(json, 15, 50, 2, "ink", "ink", 0.4);
            line(json, "blue", 2.4, 15, 50, 15 + 30 * Math.cos(angle), 50 + 30 * Math.sin(angle));
            text(json, 25, 15, String.format(Locale.ROOT, "%.0f°", value), 4.5);
        } else {
            rect(json, 25, 50, 6, 80, "tile", "line", 0.4);
            double y = 10 + 80 * (value - low) / (high - low);
            rect(json, 25, y, 16, 6, "blue", "ink", 0.4);
            text(json, 25, 3, String.format(Locale.ROOT, "%.0f ticks", value), 4.5);
        }
    }

    private static void drawGraph(Json json, double low, double high) {
        rect(json, 125, 50, 140, 84, "board", "line", 0.4);
        if (history.isEmpty()) return;
        double[] measured = new double[history.size() * 2];
        double[] target = new double[history.size() * 2];
        int i = 0;
        int targets = 0;
        for (double[] sample : history) {
            double x = 55 + 140.0 * i / HISTORY;
            measured[i * 2] = x;
            measured[i * 2 + 1] = 8 + 84 * clamp((sample[0] - low) / (high - low));
            if (!Double.isNaN(sample[1])) {
                target[targets * 2] = x;
                target[targets * 2 + 1] = 8 + 84 * clamp((sample[1] - low) / (high - low));
                targets++;
            }
            i++;
        }
        if (targets > 1) line(json, "good", 0.6, java.util.Arrays.copyOf(target, targets * 2));
        line(json, "blue", 0.9, measured);
        text(json, 125, 95, "where it is (blue) and where it's going (green), last 6 s", 3.6);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static void rect(Json json, double x, double y, double w, double h, String fill, String stroke, double width) {
        json.begin().field("t", "rect").field("x", x).field("y", y).field("w", w).field("h", h)
                .field("a", 0).field("fill", fill).field("stroke", stroke).field("lw", width).end();
    }

    private static void circle(Json json, double x, double y, double r, String fill, String stroke, double width) {
        json.begin().field("t", "circle").field("x", x).field("y", y).field("r", r)
                .field("fill", fill).field("stroke", stroke).field("lw", width).end();
    }

    private static void line(Json json, String stroke, double width, double... points) {
        json.begin().field("t", "line").field("stroke", stroke).field("lw", width).name("pts").beginArray();
        for (double p : points) json.value(p);
        json.endArray().end();
    }

    private static void text(Json json, double x, double y, String text, double size) {
        json.begin().field("t", "text").field("x", x).field("y", y).field("text", text).field("size", size)
                .field("fill", "ink").field("align", "center").field("bold", false).end();
    }
}
