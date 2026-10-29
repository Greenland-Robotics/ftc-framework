package robot.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import behavior.Node;
import behavior.input.EdgeDetector;
import sdk.ftc.Tuning;
import sdk.tuning.Gain;
import sdk.tuning.TuneReport;
import sdk.tuning.Tunable;
import sdk.tuning.Tunables;

/**
 * Tunes the robot's lifts, arms and flywheels, with gamepad 1. It finds them by itself (every
 * public {@code Lift}, {@code Arm} and {@code Flywheel} on {@code Robot}, also inside season
 * subsystems). Pick one, then either:
 * <ul>
 *   <li><b>Y: auto-tune.</b> The mechanism runs experiments on itself within its limits and
 *       reports the gains it found, already set.</li>
 *   <li><b>By hand.</b> Change a gain with the dpad and bumpers, press X for a scored test move,
 *       repeat. Panels (http://192.168.43.1:8001) graphs where it is against where it's going.</li>
 * </ul>
 * START saves the gains on the robot; from then on every OpMode uses them (Robot.java calls
 * {@code Tuning.applySaved}). The screen also shows the code line to paste into Robot.java.
 */
@TeleOp(name = "Tune mechanisms", group = "Tuning")
public class Tune extends OpModeBase {
    private Map<String, Tunable> parts;
    private List<String> names;
    private TelemetryManager panels;

    private int selectedPart;
    private int selectedGain;
    private boolean open;
    private boolean started;
    private TuneReport report;
    private Node running;
    private String status = "";

    private EdgeDetector up;
    private EdgeDetector down;
    private EdgeDetector left;
    private EdgeDetector right;
    private EdgeDetector smaller;
    private EdgeDetector bigger;
    private EdgeDetector choose;
    private EdgeDetector back;
    private EdgeDetector test;
    private EdgeDetector autoTune;
    private EdgeDetector save;
    private EdgeDetector forget;

    @Override
    protected void onInit() {
        parts = Tunables.find(robot);
        names = new ArrayList<>(parts.keySet());
        panels = PanelsTelemetry.INSTANCE.getTelemetry();
        up = edge(driver.dpadUp());
        down = edge(driver.dpadDown());
        left = edge(driver.dpadLeft());
        right = edge(driver.dpadRight());
        smaller = edge(driver.leftBumper());
        bigger = edge(driver.rightBumper());
        choose = edge(driver.a());
        back = edge(driver.b());
        test = edge(driver.x());
        autoTune = edge(driver.y());
        save = edge(driver.start());
        forget = edge(driver.back());
    }

    @Override
    protected void onInitLoop() {
        handle();
        draw();
    }

    @Override
    protected void onStart() {
        started = true;
    }

    @Override
    protected void onLoop() {
        handle();
        draw();
    }

    // ---- Buttons ----

    private void handle() {
        boolean upPressed = pressed(up);
        boolean downPressed = pressed(down);
        boolean leftPressed = pressed(left);
        boolean rightPressed = pressed(right);
        boolean smallerPressed = pressed(smaller);
        boolean biggerPressed = pressed(bigger);
        boolean choosePressed = pressed(choose);
        boolean backPressed = pressed(back);
        boolean testPressed = pressed(test);
        boolean autoPressed = pressed(autoTune);
        boolean savePressed = pressed(save);
        boolean forgetPressed = pressed(forget);
        if (running != null && !behaviors.isRunning(running)) running = null;

        if (names.isEmpty()) return;
        if (!open) {
            if (upPressed) selectedPart = (selectedPart + names.size() - 1) % names.size();
            if (downPressed) selectedPart = (selectedPart + 1) % names.size();
            if (choosePressed) {
                open = true;
                selectedGain = 0;
                report = new TuneReport(name());
                status = "";
            }
            return;
        }

        if (backPressed) {
            if (running != null) {
                behaviors.cancel(running);
                running = null;
                status = "Stopped.";
            } else {
                open = false;
            }
            return;
        }
        if (running != null) return;   // The other buttons wait until the test is over

        List<Gain> gains = part().gains();
        if (upPressed) selectedGain = (selectedGain + gains.size() - 1) % gains.size();
        if (downPressed) selectedGain = (selectedGain + 1) % gains.size();
        Gain gain = gains.get(selectedGain);
        if (rightPressed) scale(gain, 1.1);
        if (leftPressed) scale(gain, 1 / 1.1);
        if (biggerPressed) scale(gain, 2);
        if (smallerPressed) scale(gain, 0.5);

        if ((testPressed || autoPressed) && !started) {
            status = "Press start (▶) first: tests move the mechanism.";
        } else if (testPressed) {
            report.clear();
            running = part().stepTest(report);
            behaviors.run(running);
            status = "Test move...";
        } else if (autoPressed) {
            running = part().autoTune(report);
            behaviors.run(running);
            status = "Auto-tuning: keep clear. B stops it.";
        }

        if (savePressed) {
            try {
                Tuning.saved().save(name(), part());
                status = "Saved on the robot: every OpMode uses these now.";
            } catch (IOException e) {
                status = "Couldn't save: " + e.getMessage();
            }
        }
        if (forgetPressed) {
            try {
                Tuning.saved().forget(name());
                status = "Forgot the saved gains: Robot.java's are used from the next OpMode.";
            } catch (IOException e) {
                status = "Couldn't forget: " + e.getMessage();
            }
        }
    }

    private static void scale(Gain gain, double factor) {
        double value = gain.get();
        gain.set(value == 0 ? (factor > 1 ? 0.001 : 0) : value * factor);
    }

    // ---- Screen ----

    private void draw() {
        if (names.isEmpty()) {
            telemetry.addLine("No Lift, Arm or Flywheel found on Robot.");
            telemetry.addLine("Parts must be public fields of Robot (or of a public subsystem field).");
            return;
        }
        if (!open) {
            telemetry.addLine("Pick a mechanism: dpad ▲▼, A to open");
            for (int i = 0; i < names.size(); i++) {
                String saved = Tuning.saved().has(names.get(i)) ? "  (saved tuning)" : "";
                telemetry.addLine((i == selectedPart ? "> " : "  ") + names.get(i) + saved);
            }
            return;
        }

        Tunable part = part();
        telemetry.addLine(name() + (running != null ? "   [running]" : "")
                + (Tuning.saved().has(name()) ? "   (saved on robot)" : ""));
        telemetry.addData("Now", "%s → %s", TuneReport.number(part.measured()), TuneReport.number(part.setpoint()));
        List<Gain> gains = part.gains();
        for (int i = 0; i < gains.size(); i++) {
            telemetry.addLine((i == selectedGain ? "> " : "  ") + gains.get(i).name() + " = " + TuneReport.number(gains.get(i).get()));
        }
        telemetry.addLine("Code: " + part.codeLine());
        if (!status.isEmpty()) telemetry.addLine(status);
        for (String line : report.lastLines(9)) telemetry.addLine(line);
        telemetry.addLine("");
        telemetry.addLine("Y auto-tune   X test move   B stop/back");
        telemetry.addLine("▲▼ gain   ◀▶ ÷/× 1.1   LB/RB ÷/× 2");
        telemetry.addLine("START save on robot   BACK forget saved");

        panels.addData(name() + " now", part.measured());
        if (!Double.isNaN(part.setpoint())) panels.addData(name() + " target", part.setpoint());
        panels.update();
    }

    private Tunable part() {
        return parts.get(name());
    }

    private String name() {
        return names.get(selectedPart);
    }

    private static EdgeDetector edge(BooleanSupplier button) {
        return new EdgeDetector(button);
    }

    private static boolean pressed(EdgeDetector detector) {
        return detector.update() == EdgeDetector.Edge.PRESSED;
    }
}
