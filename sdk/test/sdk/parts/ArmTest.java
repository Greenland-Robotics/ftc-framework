package sdk.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import behavior.Status;
import sdk.FakeClock;
import sdk.FakeMotor;
import sdk.Loop;

class ArmTest {
    @Test
    void readsDegreesFromTicksAndItsZero() {
        FakeMotor motor = new FakeMotor(1000);
        Arm arm = new Arm(motor, 4, new FakeClock()).zeroAt(-40);
        motor.position = 200;   // 50 degrees up from where it rested
        arm.initPeriodic();
        assertEquals(10, arm.angle(), 1e-9);
    }

    @Test
    void pushesUpHardestWhenHorizontal() {
        FakeClock clock = new FakeClock();
        FakeMotor motor = new FakeMotor(1000);
        Arm arm = new Arm(motor, 1, clock).gravity(0.2).gains(0, 0);
        motor.position = 0;
        arm.periodic();
        assertEquals(0.2, motor.power, 1e-9);
        motor.position = 90;
        arm.periodic();
        assertEquals(0, motor.power, 1e-9);
    }

    @Test
    void goesToAnAngle() {
        FakeClock clock = new FakeClock();
        FakeMotor motor = new FakeMotor(400);
        Arm arm = new Arm(motor, 2, clock).limits(0, 120);
        Loop loop = new Loop(clock, arm, motor);
        assertEquals(Status.SUCCESS, loop.runUntilDone(arm.goTo(45), 300));
        assertTrue(Math.abs(arm.angle() - 45) <= 2);
    }
}
