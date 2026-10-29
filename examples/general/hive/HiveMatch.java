package hive;

import static behavior.Behaviors.race;
import static behavior.Behaviors.waitUntil;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import behavior.Node;
import behavior.runtime.BehaviorRunner;
import util.Log;
import visualizer.TreeVisualizer;

/**
 * Plays a match of this season's hive game in the simulator, with the red robot's behavior tree
 * shown live: which lanes are running, which conditions pass, which actions are active.
 *
 * A match is the TeleOp period, or (picked in the page) the Auto period followed by TeleOp. In
 * TeleOp you drive the red robot with the keyboard; in Auto its tree drives it. The blue robot
 * runs the Auto routine all match long, the same tree the TeleOp borrows its pieces from (see
 * {@link HiveRoutines}).
 *
 * Run it (see examples/README.md for more ways):
 *     VS Code:         Run and Debug → "Hive game"
 *     Android Studio:  the "Hive game" run configuration
 *     Terminal:        ./gradlew :examples:hive
 * then open http://localhost:8765 and press Play.
 *
 * What happens (pickups, shots, where they land, pivots, the AI's choices) is logged to the
 * console at debug level, each line starting with the match time. Quieter: -Dlog.level=info.
 */
public final class HiveMatch {
    private static final String TELEOP_ONLY = "TeleOp only (2:00)";
    private static final String FULL_MATCH = "Auto (0:30), then TeleOp";
    private static final String BLUE_AI = "Blue robot: runs the Auto";
    private static final String BLUE_OFF = "Blue robot: off the field";

    private static TreeVisualizer visualizer;
    private static Driver keyboard;
    private static boolean fullMatch;
    private static boolean blueAi = true;

    private static Field field;
    private static Robot red;
    private static BehaviorRunner blueRunner;

    private HiveMatch() {}

    public static void main(String[] args) throws Exception {
        // Debug unless -Dlog.level says otherwise, with the match time on each line
        if (System.getProperty("log.level") == null) Log.setLevel(Log.Level.DEBUG);
        Log.setSink((level, tag, message) -> System.out.println(String.format(Locale.ROOT, "%6.2f s %-5s [%s] %s",
                field == null ? 0 : field.time(), level, tag, message)));
        visualizer = new TreeVisualizer(HiveMatch::newMatch);
        keyboard = new KeyboardDriver(visualizer::isHeld);
        visualizer
                .afterEachTick(HiveMatch::simulate)
                .scene(() -> FieldDrawing.draw(field, red == null ? null : red.body))
                .key("Drive forward / back", "ArrowUp", "ArrowDown")
                .key("Turn left / right", "ArrowLeft", "ArrowRight")
                .key("Slide left / right", "KeyQ", "KeyE")
                .key("Drive slowly (hold)", "ShiftLeft", "ShiftRight")
                .key("Intake (hold)", "Space")
                .key("Shoot (hold to keep shooting)", "KeyF")
                .key("Line up a shot (hold)", "KeyA")
                .setting("Match", value -> fullMatch = value.equals(FULL_MATCH), TELEOP_ONLY, FULL_MATCH)
                .setting("Opponent", value -> blueAi = value.equals(BLUE_AI), BLUE_AI, BLUE_OFF)
                .watch("Balls held", () -> red.intake.ballCount() + " of " + Body.CAPACITY)
                .watch("Flywheel", () -> Math.round(red.body.flywheelSpeed * 100) + "%")
                .watch("Pose", () -> red.drive.pose())
                .watch("Red hive (top : bottom units)", () -> hiveText(field.hive(Alliance.RED)))
                .watch("Blue hive (top : bottom units)", () -> hiveText(field.hive(Alliance.BLUE)))
                .watch("Score (red : blue)", () -> field.score(Alliance.RED) + " : " + field.score(Alliance.BLUE))
                .watch("Ball layout seed", () -> field.seed)
                .watch("Match time", () -> String.format(Locale.ROOT, "%.1f s", field.time()))
                .start();
    }

    /** A new field and robots; Restart (or changing a setting) calls this again. */
    static Node newMatch() {
        // A new ball layout every match, unless -Dhive.seed=N asks to replay one
        long seed = Long.getLong("hive.seed", ThreadLocalRandom.current().nextInt(1_000_000));
        field = new Field(fullMatch, seed);
        Log.info("match", "New match, ball layout seed %d (replay it with -Dhive.seed=%d)", seed, seed);
        red = field.addRobot(Alliance.RED, 16, 20, Math.PI / 2, 1);
        blueRunner = null;
        if (blueAi) {
            Robot blue = field.addRobot(Alliance.BLUE, Field.SIZE - 16, Field.SIZE - 20, -Math.PI / 2, 0.6);
            blueRunner = new BehaviorRunner();
            blueRunner.run(race(
                    HiveRoutines.autonomous(blue, field),
                    waitUntil(() -> field.time() >= field.matchSeconds())));
        }
        return HiveRoutines.match(red, keyboard, field, fullMatch);
    }

    private static String hiveText(Hive hive) {
        String state = hive.down < 0 ? "level" : (hive.down == Hive.TOP ? "top" : "bottom") + " down";
        return hive.weight[Hive.TOP] + " : " + hive.weight[Hive.BOTTOM] + ", " + state;
    }

    /** After each tick of your tree: the opponent's tree ticks, then the world moves one loop. */
    static void simulate() {
        if (blueRunner != null) blueRunner.tick();
        field.step();
    }
}
