import hive.HiveMatch;
import tunedemo.TuneDemo;
import visualizer.TreeVisualizer;

/**
 * Every laptop example in one page: pick one from the "Example" drop-down at the top, then
 * Play, Step or Restart it. Switching stops the one that was running.
 *
 * Run it (see examples/README.md for more ways):
 *     VS Code:         Run and Debug → "Examples"
 *     Android Studio:  the "Examples" run configuration
 *     Terminal:        ./gradlew :examples:run
 * then open http://localhost:8765.
 *
 * Each example also runs on its own (its main), and adds itself here with its setUp method.
 */
public final class Examples {
    private Examples() {}

    public static void main(String[] args) throws Exception {
        if (java.util.Arrays.asList(args).contains("--console")) {
            BehaviorBasics.main(args);   // Just print the basics, without the web page
            return;
        }
        new TreeVisualizer()
                .example("Behavior tree basics", BehaviorBasics::setUp)
                .example("Hive game", HiveMatch::setUp)
                .example("Tuning demo (sdk auto-tuner)", TuneDemo::setUp)
                .start();
    }
}
