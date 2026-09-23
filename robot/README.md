# robot

**Your robot code.** This is where you spend most of your time.
This module is the Robot Controller app: the FTC SDK builds it, together with `framework` and `pedro`, into the app installed on the robot.

Code lives in [`src/robot/`](src/robot/):

| Path | What goes here |
|------|----------------|
| [`Robot.java`](src/robot/Robot.java) | Builds the robot from its subsystems. **The only place that uses the hardware map** |
| [`subsystems/`](src/robot/subsystems/) | One class per mechanism (`Intake`, `Arm`...), owning its hardware |
| [`opmode/`](src/robot/opmode/) | Your Autos and TeleOps. Start by copying a boilerplate from [`examples/ftc/`](../examples/ftc/) |
| [`control/`](src/robot/control/) | The OpMode base classes. You extend them; you rarely edit them |

Examples and boilerplates to copy from are in [`examples/ftc/`](../examples/ftc/). The behavior tree building blocks (`sequence`, `selector`, `Action`...) are documented in [`framework/README.md`](../framework/README.md).

## Writing your robot code
1. **Configure the drivetrain** in [`Constants.java`](../pedro/src/pedro/Constants.java) (motor names, directions, odometry) and tune it with the **Tuning** OpMode. See [`pedro/`](../pedro/README.md).
2. **Add a subsystem per mechanism** in `subsystems/`: it owns the motors and sensors and offers behaviors like `collect()` and checks like `hasSample()`. [`examples/ftc/Intake.java`](../examples/ftc/Intake.java) is a complete example.
3. **Register each subsystem** in [`Robot.java`](src/robot/Robot.java): add a field and create it with `register(...)`.
4. **Write OpModes** in `opmode/`:
    - Autonomous: extend `AutoBase` and return the whole routine as one tree from `routine()`.
    - TeleOp: extend `TeleOpBase` and connect buttons to behaviors in `bindControls()`.

```
OpMode (AutoBase / TeleOpBase)
   │ uses robot.drive, robot.intake ...
   ▼
Robot ── creates ──► Subsystems (Drive, Intake, ...) ── own ──► motors, sensors
   │                        │ hand out
   │                        ▼
   └─ periodic() ◄───── behavior trees (Nodes) run by the base class
```

---

## WARNING: Do NOT write `while` loops
The base classes run the main loop and tick your behaviors for you. Code in behaviors and in `runLoop()` must return quickly: never write `while (opModeIsActive()) {...}` or call `sleep()` there, or the whole robot freezes. To wait, use `delay(ms)` or `waitUntil(...)`.

---

## Notes
- When writing your opmodes, always include the line `///@author <your-name>` just above the `@TeleOp` or `@Autonomous` annotation. This provides clean documentation about who wrote the opmode, which is useful for collaborative purposes and knowing who to blame when something goes wrong. Jokes aside, it is very important when collaborating on code to sign your work.
  - Example:
    ```java
    ///@author Josh Kelley
    @TeleOp(name = "My TeleOp")
    public class MyTeleOp extends TeleOpBase {...}
    ```
- If you are using the PlayStation controllers, use the button map below:

  | PlayStation | Gamepad (in code) |
  |-------------|-------------------|
  | X           | A                 |
  | O           | B                 |
  | △           | Y                 |
  | □           | X                 |

  For example: `gamepad1.a` will be automatically mapped to the X button on the PlayStation controller.

- In TeleOp, the following inputs are consumed by the built-in drive logic inside `TeleOpBase` (`gamepad1` only):
  - `left_stick_x`
  - `left_stick_y`
  - `right_stick_x`

  These are passed to `robot.drive.drive(...)`. Using these inputs elsewhere in your TeleOp means one stick controls both driving and whatever else you mapped it to. To change the mapping, override `driveWithGamepad()` in your TeleOp.


---

## Robot

[`Robot`](src/robot/Robot.java) is created by the base class before your OpMode's `initialize()`, and is available as `robot` in every OpMode.

- **Fields** are your subsystems: `robot.drive`, `robot.intake`...
- **`periodic()`** is called every loop and calls each registered subsystem's `periodic()`.
- **`register(subsystem)`** adds a subsystem to that list. Forgetting it means its `periodic()` never runs.

## OpModeBase

The shared main loop. Each loop it:
1. updates every subsystem (`robot.periodic()`),
2. ticks the running behaviors,
3. calls the OpMode's own loop code, then updates telemetry.

When the OpMode stops, running behaviors are halted so nothing keeps moving. Extend `AutoBase` or `TeleOpBase`, not this class.

## AutoBase

Your whole autonomous is one behavior tree. Override:

| Method | When | Use it to |
|--------|------|-----------|
| `initialize()` | During init | Set the starting pose, build paths |
| `routine()` (required) | During init, after `initialize()` | Return the whole Auto as one tree |
| `runLoop()` | Every loop after start | Telemetry |

```java
@Autonomous(name = "Score and park")
public class ScoreAndPark extends AutoBase {
    private Paths paths;

    @Override
    protected void initialize() {
        robot.drive.setStartingPose(Paths.START);
        paths = new Paths(robot.drive);
    }

    @Override
    protected Node routine() {
        return sequence(
                robot.drive.follow(paths.toGoal),
                robot.intake.eject(),
                robot.drive.follow(paths.park));
    }
}
```

By convention, a `static class Paths` inside the Auto holds its paths. Generate them with the [Pedro Pathing Visualizer](https://visualizer.pedropathing.com) and build them with `drive.pathBuilder()`.

## TeleOpBase

Gamepad 1's sticks drive the robot out of the box (field-centric). Override:

| Method | When | Use it to |
|--------|------|-----------|
| `initialize()` | During init | Anything else to set up |
| `bindControls(controls)` (required) | During init | Connect buttons to behaviors |
| `runLoop()` | Every loop after start | Telemetry |
| `driveWithGamepad()` | Every loop after start | Change how the sticks drive the robot |

```java
@Override
protected void bindControls(Bindings controls) {
    controls.onPress(() -> gamepad2.a, robot.intake.collect());       // runs until done
    controls.whileHeld(() -> gamepad2.right_bumper, robot.intake.run()); // stops on release
    controls.toggleOnPress(() -> gamepad2.x, robot.intake.run());     // press on, press off
}
```

While the robot is following a path (for example `robot.drive.driveTo(...)` bound to a button), the sticks are ignored; the driver gets control back when the path ends or is halted.
