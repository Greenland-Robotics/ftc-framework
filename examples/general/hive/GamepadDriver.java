package hive;

import java.util.function.BooleanSupplier;

import sdk.teleop.Controller;
import sdk.teleop.DriveControls;

/**
 * The driver's controls, read from an sdk {@link Controller}: the sticks drive (through the
 * sdk's {@link DriveControls}, which also does slow mode), and buttons run the mechanisms.
 * Nothing here knows about the keyboard, so it works with a real gamepad too.
 */
final class GamepadDriver implements Driver {
    private final DriveControls sticks;
    private final BooleanSupplier intake;
    private final BooleanSupplier shoot;
    private final BooleanSupplier aim;

    GamepadDriver(Controller gamepad) {
        sticks = new DriveControls(gamepad)
                .robotCentric()
                .slowWhileHeld(gamepad.leftBumper(), 0.35)
                .turnScale(0.6);
        intake = gamepad.rightBumper();
        shoot = gamepad.rightTrigger();
        aim = gamepad.a();
    }

    @Override
    public double forward() {
        return sticks.forward();
    }

    @Override
    public double strafe() {
        return sticks.strafe();
    }

    @Override
    public double turn() {
        return sticks.turn();
    }

    @Override
    public boolean intake() {
        return intake.getAsBoolean();
    }

    @Override
    public boolean shoot() {
        return shoot.getAsBoolean();
    }

    @Override
    public boolean aim() {
        return aim.getAsBoolean();
    }
}
