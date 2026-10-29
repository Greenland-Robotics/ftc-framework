package sdk.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import behavior.Node;
import behavior.Status;
import sdk.FakeClock;
import sdk.Loop;
import sdk.hardware.Motor;

class FlywheelTest {
    /** A wheel that takes time to spin up: its speed moves a fifth of the way to the power's speed each loop. */
    private static final class Wheel implements Motor {
        double power;
        double rpm;

        @Override
        public void setPower(double power) {
            this.power = power;
        }

        @Override
        public double velocity() {
            return rpm * 28 / 60;
        }

        void step() {
            rpm += (power * 6000 - rpm) * 0.2;
        }
    }

    @Test
    void spinsUpThenSaysItIsAtSpeed() {
        FakeClock clock = new FakeClock();
        Wheel wheel = new Wheel();
        Flywheel flywheel = new Flywheel(wheel, 28).maxRpm(6000).tolerance(100);
        Loop loop = new Loop(clock, flywheel, null);
        Node spinUp = flywheel.spinUp(4500);
        assertEquals(Status.RUNNING, loop.tick(spinUp));
        assertFalse(flywheel.atSpeed());
        Status status = Status.RUNNING;
        for (int i = 0; i < 100 && status == Status.RUNNING; i++) {
            wheel.step();
            status = loop.tick(spinUp);
        }
        assertEquals(Status.SUCCESS, status);
        assertTrue(flywheel.atSpeed());
        assertEquals(4500, flywheel.rpm(), 100);
    }

    @Test
    void spinStopsTheWheelWhenHalted() {
        Wheel wheel = new Wheel();
        Flywheel flywheel = new Flywheel(wheel, 28);
        Node spin = flywheel.spin(3000);
        spin.tick();
        flywheel.periodic();
        assertTrue(wheel.power > 0);
        spin.halt();
        flywheel.periodic();
        assertEquals(0, wheel.power, 1e-9);
    }

    @Test
    void isNotAtSpeedFromAReadingBeforeItWasRestarted() {
        Wheel wheel = new Wheel();
        Flywheel flywheel = new Flywheel(wheel, 28).tolerance(100);
        wheel.rpm = 3000;
        Node spin = flywheel.spin(3000);
        spin.tick();
        flywheel.periodic();
        assertTrue(flywheel.atSpeed());
        spin.halt();                  // Told to stop...
        flywheel.spin(3000).tick();   // ...and to start again before the next loop
        assertFalse(flywheel.atSpeed(), "the last reading was taken while it was being stopped");
    }

    @Test
    void neverRunsBackwards() {
        Wheel wheel = new Wheel();
        wheel.rpm = 6000;
        Flywheel flywheel = new Flywheel(wheel, 28);
        flywheel.spin(1000).tick();
        flywheel.periodic();
        assertEquals(0, wheel.power, 1e-9);
    }
}
