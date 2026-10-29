package sdk.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import behavior.Node;
import behavior.Status;
import sdk.FakeClock;
import sdk.FakeMotor;
import sdk.Loop;

class RollerTest {
    private final FakeClock clock = new FakeClock();
    private final FakeMotor motor = new FakeMotor(0);

    @Test
    void collectsUntilTheSensorSeesAPiece() {
        boolean[] piece = {false};
        Roller roller = new Roller(motor, clock).power(0.8).sensor(() -> piece[0]);
        Loop loop = new Loop(clock, roller, null);
        Node collect = roller.collect();
        assertEquals(Status.RUNNING, loop.tick(collect));
        assertEquals(0.8, motor.power, 1e-9);
        piece[0] = true;
        assertEquals(Status.SUCCESS, loop.tick(collect));
        assertEquals(0, motor.power, 1e-9);
    }

    @Test
    void collectWithoutASensorFailsAtSetUp() {
        Roller roller = new Roller(motor, clock);
        assertThrows(IllegalStateException.class, roller::collect);
    }

    @Test
    void ejectsBackwardsForAWhile() {
        Roller roller = new Roller(motor, clock).power(0.5);
        Loop loop = new Loop(clock, roller, null);
        Node eject = roller.eject(100);
        assertEquals(Status.RUNNING, loop.tick(eject));
        assertEquals(-0.5, motor.power, 1e-9);
        assertEquals(Status.SUCCESS, loop.runUntilDone(eject, 20));
        assertEquals(0, motor.power, 1e-9);
    }

    @Test
    void stopsWhenHalted() {
        Roller roller = new Roller(motor, clock);
        Node run = roller.run();
        run.tick();
        assertEquals(1, motor.power, 1e-9);
        run.halt();
        assertEquals(0, motor.power, 1e-9);
    }
}
