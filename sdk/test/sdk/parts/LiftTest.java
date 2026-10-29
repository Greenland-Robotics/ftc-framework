package sdk.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import behavior.Node;
import behavior.Status;
import sdk.FakeClock;
import sdk.FakeMotor;
import sdk.Loop;

class LiftTest {
    private FakeClock clock;
    private FakeMotor motor;
    private Lift lift;
    private Loop loop;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        motor = new FakeMotor(3000).pull(0.1).hardStop(0);
        lift = new Lift(motor, clock).limits(0, 2000).gravity(0.1);
        loop = new Loop(clock, lift, motor);
    }

    @Test
    void goesToTargetAndSucceeds() {
        assertEquals(Status.SUCCESS, loop.runUntilDone(lift.goTo(1200), 200));
        assertEquals(1200, lift.position(), 20);
        assertTrue(lift.atTarget());
    }

    @Test
    void clampsTargetsToItsLimits() {
        loop.runUntilDone(lift.goTo(5000), 300);
        assertEquals(2000, lift.target());
        assertEquals(2000, lift.position(), 20);
    }

    @Test
    void holdsWhereItIsWhenHalted() {
        Node up = lift.goTo(1500);
        for (int i = 0; i < 10; i++) loop.tick(up);
        up.halt();
        double haltedAt = lift.position();
        for (int i = 0; i < 100; i++) loop.idle();
        // The target stays at 1500: it keeps holding instead of dropping
        assertTrue(lift.position() > haltedAt);
        assertEquals(1500, lift.position(), 30);
    }

    @Test
    void doesNotDriveDuringInit() {
        motor.position = 300;
        lift.initPeriodic();
        assertEquals(0, motor.power);
        assertEquals(300, lift.target());
    }

    @Test
    void manualMovesWithTheStickAndStopsWhenLetGo() {
        double[] stick = {1};
        Node manual = lift.manual(() -> stick[0]);
        for (int i = 0; i < 25; i++) loop.tick(manual);
        double moved = lift.position();
        assertTrue(moved > 200, "moved " + moved);
        stick[0] = 0;
        for (int i = 0; i < 50; i++) loop.tick(manual);
        assertEquals(moved, lift.position(), 400);
        assertTrue(lift.target() <= 2000);
    }

    @Test
    void homesWhenItStallsAndZeroes() {
        motor.position = 400;   // Powered on part-way up
        motor.hardStop(-400);   // The real bottom is 400 ticks below where the encoder thinks
        Node home = lift.home();
        assertEquals(Status.SUCCESS, loop.runUntilDone(home, 300));
        assertEquals(0, lift.position(), 1e-9);
        assertEquals(0, lift.target(), 1e-9);
    }

    @Test
    void homesOnTheSwitchWhenItHasOne() {
        boolean[] pressed = {false};
        lift.homeSwitch(() -> pressed[0]);
        Node home = lift.home();
        loop.tick(home);
        loop.tick(home);   // The part sets the motor at the start of each loop, after the tick before it
        assertTrue(motor.power < 0, "drives toward home");
        pressed[0] = true;
        assertEquals(Status.SUCCESS, loop.tick(home));
    }
}
