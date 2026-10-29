package sdk.teleop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import sdk.FakeClock;

class ControlsTest {
    private final FakeController gamepad = new FakeController();

    @Test
    void smallStickMovesReadAsZero() {
        gamepad.axes.put(Controller.Axis.LEFT_Y, 0.03);
        assertEquals(0, gamepad.leftStickY().getAsDouble());
        gamepad.axes.put(Controller.Axis.LEFT_Y, 0.6);
        assertEquals(0.6, gamepad.leftStickY().getAsDouble(), 1e-9);
    }

    @Test
    void triggersWorkAsButtons() {
        gamepad.axes.put(Controller.Axis.RIGHT_TRIGGER, 0.4);
        assertFalse(gamepad.rightTrigger().getAsBoolean());
        gamepad.axes.put(Controller.Axis.RIGHT_TRIGGER, 0.9);
        assertTrue(gamepad.rightTrigger().getAsBoolean());
    }

    @Test
    void sticksBecomeDriveCommands() {
        DriveControls drive = new DriveControls(gamepad);
        gamepad.axes.put(Controller.Axis.LEFT_Y, 1.0);     // Up
        gamepad.axes.put(Controller.Axis.LEFT_X, 1.0);     // Right
        gamepad.axes.put(Controller.Axis.RIGHT_X, -1.0);   // Turn left
        assertEquals(1, drive.forward(), 1e-9);
        assertEquals(-1, drive.strafe(), 1e-9);            // Strafe left is positive, so right is negative
        assertEquals(1, drive.turn(), 1e-9);               // Turning left is positive
        assertTrue(drive.isFieldCentric());
    }

    @Test
    void slowModeWhileTheBumperIsHeld() {
        DriveControls drive = new DriveControls(gamepad);
        gamepad.axes.put(Controller.Axis.LEFT_Y, 1.0);
        gamepad.down.add(Controller.Button.RIGHT_BUMPER);
        assertEquals(0.4, drive.forward(), 1e-9);
    }

    @Test
    void resetHeadingFiresOncePerPress() {
        DriveControls drive = new DriveControls(gamepad);
        gamepad.down.add(Controller.Button.BACK);
        assertTrue(drive.resetHeadingPressed());
        assertFalse(drive.resetHeadingPressed());
    }

    @Test
    void matchClockCountsDown() {
        FakeClock clock = new FakeClock();
        MatchClock match = new MatchClock(120, clock);
        assertEquals(120, match.timeLeft(), 1e-9);
        match.start();
        clock.advance(95_000);
        assertEquals(25, match.timeLeft(), 1e-9);
        clock.advance(60_000);
        assertEquals(0, match.timeLeft(), 1e-9);
    }
}
