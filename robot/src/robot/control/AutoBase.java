package robot.control;

import static behavior.Behaviors.delay;
import static behavior.Behaviors.sequence;

import com.pedropathing.geometry.Pose;

import behavior.Node;
import pedro.Poses;
import robot.Robot;
import sdk.auto.Alliance;
import sdk.auto.AutoMenu;

/**
 * Base class for autonomous OpModes. During init, gamepad 1 picks the alliance, anything you
 * add to the {@link #menu()}, and a start delay. When start is pressed the menu locks, and your
 * {@link #routine()} builds the whole Auto as one behavior tree from those choices.
 *
 * <p>Write poses for the blue alliance with {@link #pose(double, double, double)}: on red they
 * are flipped to match the field ({@link Robot#SYMMETRY}).
 */
public abstract class AutoBase extends OpModeBase {
    private AutoMenu menu;
    private AutoMenu.Choice<Alliance> alliance;
    private AutoMenu.Number startDelay;

    /** Add menu lines, build paths that don't depend on the choices... The robot exists by now. */
    protected void initialize() {}

    /** Runs every loop during init, after the menu, e.g. to show what the camera sees. */
    protected void initLoop() {}

    /**
     * The whole autonomous as one behavior tree. Called once, when start is pressed, so the
     * choices in the menu (and the camera's answer) are final: set the starting pose here,
     * then return the tree. The start delay is added in front for you.
     */
    protected abstract Node routine();

    /** Runs every loop alongside the routine, e.g. for telemetry. */
    protected void runLoop() {}

    /** The alliance picked in the menu. */
    protected Alliance alliance() {
        return alliance.get();
    }

    /** The menu shown during init: add your own lines in {@link #initialize()}. */
    protected AutoMenu menu() {
        return menu;
    }

    /** A pose written for the blue alliance, flipped for red: x and y in inches, heading in degrees. */
    protected Pose pose(double x, double y, double headingDegrees) {
        return Poses.forAlliance(alliance(), Robot.SYMMETRY, new Pose(x, y, Math.toRadians(headingDegrees)));
    }

    @Override
    protected final void onInit() {
        menu = new AutoMenu(driver);
        alliance = menu.choice("Alliance", Alliance.values());
        initialize();
        startDelay = menu.number("Start delay (s)", 0, 15, 1);
    }

    @Override
    protected final void onInitLoop() {
        menu.update();
        for (String line : menu.show()) {
            telemetry.addLine(line);
        }
        initLoop();
    }

    @Override
    protected final void onStart() {
        menu.lock();
        Node routine = routine();
        long delayMillis = Math.round(startDelay.get() * 1000);
        behaviors.run(delayMillis > 0 ? sequence(delay(delayMillis), routine) : routine);
    }

    @Override
    protected final void onLoop() {
        runLoop();
    }
}
