package sdk.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import behavior.Node;
import behavior.Status;
import sdk.FakeClock;
import sdk.hardware.SimMotor;
import sdk.parts.Arm;
import sdk.parts.Flywheel;
import sdk.parts.Lift;
import subsystem.Subsystem;

/** Auto-tunes simulated mechanisms that start with bad gains, then checks the gains it found. */
class AutoTuneTest {
    private final FakeClock clock = new FakeClock();

    /** Runs {@code node} like an OpMode loop: the world moves, the part updates, the node ticks. */
    private Status run(Node node, Subsystem part, SimMotor motor, double maxSeconds) {
        Status status = Status.RUNNING;
        for (int i = 0; i < maxSeconds * 50 && status == Status.RUNNING; i++) {
            motor.step(0.02);
            clock.advance(20);
            part.periodic();
            status = node.tick();
        }
        return status;
    }

    private void print(TuneReport report) {
        for (String line : report.lines()) System.out.println(report.name() + ": " + line);
    }

    @Test
    void tunesALiftWithGravityAndFriction() {
        SimMotor motor = new SimMotor(2800).lag(0.08).gravity(0.12).friction(0.04).travel(0, 3000);
        Lift lift = new Lift(motor, clock).limits(0, 2900).gains(0.0001, 0);   // Far too weak to start with
        TuneReport report = new TuneReport("lift");

        Status status = run(lift.autoTune(report), lift, motor, 120);
        print(report);
        assertEquals(Status.SUCCESS, status, String.join("\n", report.lines()));
        assertFalse(report.failed());

        double gravity = lift.gains().get(2).get();
        assertEquals(0.12, gravity, 0.04, "gravity");
        assertTrue(lift.codeLine().startsWith(".gains("));

        // The tuned lift moves well: little overshoot, settles quickly
        StepScore score = new StepScore(lift.position(), 2000, 20, clock.nowMillis());
        Node move = lift.goTo(2000);
        for (int i = 0; i < 150; i++) {
            run(move, lift, motor, 0.02);
            score.sample(lift.position(), clock.nowMillis());
        }
        System.out.println("lift after tuning: " + score.describe());
        assertTrue(score.isSettled(), score.describe());
        assertTrue(score.overshoot() < 0.1, score.describe());
        assertTrue(score.settleMillis() < 2000, score.describe());
    }

    @Test
    void tunesAHeavySlowStickyLift() {
        SimMotor motor = new SimMotor(1200).lag(0.25).gravity(0.25).friction(0.1).travel(0, 1500);
        Lift lift = new Lift(motor, clock).limits(0, 1450).tolerance(15);
        TuneReport report = new TuneReport("heavy lift");

        Status status = run(lift.autoTune(report), lift, motor, 180);
        print(report);
        assertEquals(Status.SUCCESS, status, String.join("\n", report.lines()));
        assertEquals(0.25, lift.gains().get(2).get(), 0.05, "gravity");

        StepScore score = new StepScore(lift.position(), 1100, 15, clock.nowMillis());
        Node move = lift.goTo(1100);
        for (int i = 0; i < 200; i++) {
            run(move, lift, motor, 0.02);
            score.sample(lift.position(), clock.nowMillis());
        }
        System.out.println("heavy lift after tuning: " + score.describe());
        assertTrue(score.isSettled(), score.describe());
        assertTrue(score.overshoot() < 0.12, score.describe());
    }

    @Test
    void tunesAnArm() {
        double ticksPerDegree = 4;
        SimMotor motor = new SimMotor(1600).lag(0.1).friction(0.03).travel(0, 150 * ticksPerDegree)
                .gravity(ticks -> 0.18 * Math.cos(Math.toRadians(ticks / ticksPerDegree - 40)));
        Arm arm = new Arm(motor, ticksPerDegree, clock).zeroAt(-40).limits(-40, 110).gains(0.001, 0);
        TuneReport report = new TuneReport("arm");

        Status status = run(arm.autoTune(report), arm, motor, 120);
        print(report);
        assertEquals(Status.SUCCESS, status, String.join("\n", report.lines()));
        assertEquals(0.18, arm.gains().get(2).get(), 0.06, "gravity when level");

        StepScore score = new StepScore(arm.angle(), 60, 2, clock.nowMillis());
        Node move = arm.goTo(60);
        for (int i = 0; i < 150; i++) {
            run(move, arm, motor, 0.02);
            score.sample(arm.angle(), clock.nowMillis());
        }
        System.out.println("arm after tuning: " + score.describe());
        assertTrue(score.isSettled(), score.describe());
        assertTrue(score.overshoot() < 0.12, score.describe());
    }

    @Test
    void tunesAFlywheel() {
        // 28 ticks per turn: 2800 ticks/s is 6000 rpm
        SimMotor motor = new SimMotor(2800).lag(0.3).friction(0.06);
        Flywheel wheel = new Flywheel(motor, 28, clock).maxRpm(9000).gain(0);   // A wrong top speed to start with
        TuneReport report = new TuneReport("shooter");

        Status status = run(wheel.autoTune(report), wheel, motor, 120);
        print(report);
        assertEquals(Status.SUCCESS, status, String.join("\n", report.lines()));
        assertEquals(6000, wheel.gains().get(0).get(), 400, "maxRpm");
        assertEquals(0.06, wheel.gains().get(1).get(), 0.03, "friction");
        assertTrue(wheel.gains().get(2).get() > 0, "kP");

        Node spin = wheel.spinUp(4000);
        assertEquals(Status.SUCCESS, run(spin, wheel, motor, 3));
        assertEquals(4000, wheel.rpm(), 150);
    }

    @Test
    void refusesALiftWithoutLimits() {
        SimMotor motor = new SimMotor(2800);
        Lift lift = new Lift(motor, clock).limits(0, Double.POSITIVE_INFINITY);
        TuneReport report = new TuneReport("lift");
        assertEquals(Status.FAILURE, run(lift.autoTune(report), lift, motor, 1));
        assertTrue(report.failure().contains("limits"));
    }

    @Test
    void spotsAMotorThatRunsBackwards() {
        SimMotor backwards = new SimMotor(-2800).travel(-3000, 3000).at(0);
        Lift lift = new Lift(backwards, clock).limits(-2900, 2900);
        TuneReport report = new TuneReport("lift");
        assertEquals(Status.FAILURE, run(lift.autoTune(report), lift, backwards, 30));
        assertTrue(report.failure().contains("reversed"), report.failure());
    }

    @Test
    void puttingItsGainsBackWhenStopped() {
        SimMotor motor = new SimMotor(2800).lag(0.08).gravity(0.12).friction(0.04).travel(0, 3000);
        Lift lift = new Lift(motor, clock).limits(0, 2900).gains(0.003, 0.0001);
        TuneReport report = new TuneReport("lift");
        Node tune = lift.autoTune(report);
        run(tune, lift, motor, 1);
        tune.halt();
        assertTrue(report.failed());
        assertEquals(0.003, lift.gains().get(0).get(), 1e-12);
    }

    @Test
    void findsTunablePartsOnARobot() {
        SimMotor motor = new SimMotor(1000);
        Object robot = new Object() {
            public final Lift lift = new Lift(motor);
            public final Season scorer = new Season();
        };
        assertEquals(java.util.Arrays.asList("lift", "scorer.arm"), new java.util.ArrayList<>(Tunables.find(robot).keySet()));
    }

    public static final class Season implements Subsystem {
        public final Arm arm = new Arm(new SimMotor(1000), 1);
    }
}
