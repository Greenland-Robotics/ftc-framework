package sdk.ftc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import sdk.tuning.SavedGains;
import sdk.tuning.Tunables;

/**
 * Where tuned gains live on the robot, and the call that uses them. Add one line at the end of
 * Robot's constructor:
 * <pre>
 * Tuning.applySaved(this);
 * </pre>
 * From then on, gains saved by the Tune OpMode replace the numbers in Robot.java (every OpMode
 * shows which parts use saved tuning). Paste the Tune OpMode's code line into Robot.java and
 * press back in the Tune OpMode to forget the saved copy, so the code is the only source again.
 */
public final class Tuning {
    private static final SavedGains SAVED = new SavedGains(new File(AppUtil.FIRST_FOLDER, "tuning.properties"));
    private static List<String> applied = Collections.emptyList();

    private Tuning() {}

    /** The robot's tuning file: /sdcard/FIRST/tuning.properties. */
    public static SavedGains saved() {
        return SAVED;
    }

    /** Puts saved gains on every tunable part of {@code robot}. */
    public static void applySaved(Object robot) {
        applied = new ArrayList<>(SAVED.apply(Tunables.find(robot)));
    }

    /** The parts using saved gains, as of the last {@link #applySaved}. */
    public static List<String> applied() {
        return Collections.unmodifiableList(applied);
    }
}
