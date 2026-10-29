package sdk.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import behavior.Node;
import behavior.Status;
import sdk.FakeClock;
import sdk.hardware.Servo;

class ServoPartTest {
    private final FakeClock clock = new FakeClock();
    private final double[] main = {Double.NaN};
    private final double[] partner = {Double.NaN};
    private final Servo mainServo = position -> main[0] = position;
    private final Servo partnerServo = position -> partner[0] = position;

    @Test
    void waitsItsMoveTimeThenSucceeds() {
        ServoPart claw = new ServoPart(mainServo, clock).moveTime(200);
        Node open = claw.setTo(0.7);
        assertEquals(Status.RUNNING, open.tick());
        assertEquals(0.7, main[0], 1e-9);
        clock.advance(199);
        assertEquals(Status.RUNNING, open.tick());
        clock.advance(1);
        assertEquals(Status.SUCCESS, open.tick());
    }

    @Test
    void clampsToItsRangeAndMirrorsItsPartner() {
        ServoPart wrist = new ServoPart(mainServo, clock).range(0.2, 0.8).pairedWith(partnerServo).moveTime(0);
        wrist.setTo(1).tick();
        assertEquals(0.8, main[0], 1e-9);
        assertEquals(0.2, partner[0], 1e-9);
    }

    @Test
    void togglesBetweenTwoPositions() {
        ServoPart claw = new ServoPart(mainServo, clock).moveTime(0).startAt(0.1);
        Node toggle = claw.toggle(0.1, 0.9);
        toggle.tick();
        assertEquals(0.9, claw.position(), 1e-9);
        toggle.tick();
        assertEquals(0.1, claw.position(), 1e-9);
    }
}
