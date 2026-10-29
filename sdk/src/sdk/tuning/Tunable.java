package sdk.tuning;

import java.util.List;

import behavior.Node;

/**
 * A part whose control numbers can be tuned: {@code Lift}, {@code Arm} and {@code Flywheel}. The
 * Tune OpMode finds every one on the robot by itself (see {@link Tunables#find}), so there's
 * nothing to set up beyond giving lifts and arms their {@code .limits(...)}.
 */
public interface Tunable {
    /** Its gains, in the order to show them. */
    List<Gain> gains();

    /**
     * Tunes itself: runs experiments (moving the mechanism within its limits), works out the
     * gains, sets them, checks them with test moves and refines them. Progress and the final
     * numbers go to {@code report}. Halting it stops the mechanism safely.
     */
    Node autoTune(TuneReport report);

    /** One test move with the current gains, scored into {@code report}: for tuning by hand. */
    Node stepTest(TuneReport report);

    /** What it is doing now (position, rpm), for graphs. */
    double measured();

    /** What it is trying to do, for graphs. */
    double setpoint();

    /** The current gains as code to paste where the part is created, e.g. {@code .gains(0.004, 0.0002).gravity(0.11)}. */
    String codeLine();
}
