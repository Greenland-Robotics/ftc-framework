# robot

**Your robot code.** This is where you spend most of your time.
This module is the Robot Controller app: the FTC SDK builds it, together with `framework` and `pedro`, into the app installed on the robot.

Code lives in [`src/robot/`](src/robot/):

| Path | What goes here |
|------|----------------|
| [`Robot.java`](src/robot/Robot.java) | Builds the robot from its subsystems. **The only place that uses the hardware map** |
| [`subsystems/`](src/robot/subsystems/) | Your own season subsystems: usually [sdk parts](../sdk/README.md) wrapped in the game's words (`Scorer.scoreBasket()`) |
| [`opmode/`](src/robot/opmode/) | Your Autos and TeleOps. Start by copying a boilerplate from [`examples/ftc/`](../examples/ftc/) |
| [`control/`](src/robot/control/) | The OpMode base classes (you extend them; you rarely edit them), and the **Tune mechanisms** OpMode |

Examples and boilerplates to copy from are in [`examples/ftc/`](../examples/ftc/). The behavior tree building blocks (`sequence`, `selector`, `Action`...) are documented in [`framework/README.md`](../framework/README.md).

## Writing your robot code
1. **Configure the drivetrain** in [`Constants.java`](../pedro/src/pedro/Constants.java) (motor names, directions, odometry) and tune it with the **Tuning** OpMode. See [`pedro/`](../pedro/README.md).
2. **Add each mechanism** in [`Robot.java`](src/robot/Robot.java): add a field and create it with `register(...)`. Most are an [sdk part](../sdk/README.md#parts) (`Lift`, `Arm`, `Roller`, `ServoPart`, `Flywheel`) given its hardware by name: `lift = register(new Lift(hw.motor("lift")).limits(0, 3000));`.
3. **Group parts in a season subsystem** in `subsystems/` when they work together (a lift and claw that score), or write a mechanism from scratch there when no part fits. See [Adding your own](../sdk/README.md#adding-your-own).
4. **Write OpModes** in `opmode/`:
    - Autonomous: extend `AutoBase` and return the whole routine as one tree from `routine()`.
    - TeleOp: extend `TeleOpBase` and connect buttons to behaviors in `bindControls()`.

5. **Tune** lifts, arms and flywheels with the **Tune mechanisms** OpMode (Driver Station, "Tuning" group): press Y to auto-tune, START to save on the robot. See [Tuning](../sdk/README.md#tuning).

Whole robots for past seasons, built this way, are in [`examples/ftc/seasons/`](../examples/ftc/seasons/).

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
- Use `driver` (gamepad 1) and `operator` (gamepad 2) rather than `gamepad1`/`gamepad2`: every button is ready for a binding (`operator.a()`), and sticks read **positive when pushed up** (raw FTC gamepads read negative). PlayStation names work too:

  | PlayStation | In code |
  |-------------|---------|
  | ✕           | `a()` or `cross()` |
  | ○           | `b()` or `circle()` |
  | △           | `y()` or `triangle()` |
  | □           | `x()` or `square()` |

- In TeleOp, the driver's left stick, right stick X, right bumper (slow) and back (reset heading) are used by the built-in driving. Using them elsewhere means one input does two things. To change the mapping, override `driveControls()`; see [Gamepads and driving](../sdk/README.md#gamepads-and-driving).


---

## Robot

[`Robot`](src/robot/Robot.java) is created by the base class before your OpMode's `initialize()`, and is available as `robot` in every OpMode.

- **Fields** are your subsystems: `robot.drive`, `robot.intake`...
- **`hw`** ([`sdk.ftc.Hardware`](../sdk/src/sdk/ftc/Hardware.java)) gets motors, servos and sensors by their configured names, and fails at init with the names that exist if one is wrong.
- **`SYMMETRY`** is this season's [field symmetry](../sdk/README.md#one-auto-for-both-alliances), used to flip Auto poses for red.
- **`initPeriodic()`** is called every loop during init (sensors only: nothing may move before start), **`periodic()`** every loop after start. Each calls the same method on every registered subsystem.
- **`register(subsystem)`** adds a subsystem to that list. Forgetting it means it never updates.
- **`Tuning.applySaved(this)`** at the end of the constructor puts gains saved by the Tune OpMode on the parts. Keep it last.

## OpModeBase

The shared main loop. During init, each loop it lets subsystems read their sensors (`robot.initPeriodic()`) and runs the OpMode's init code (the Auto menu). After start, each loop it:
1. updates every subsystem (`robot.periodic()`),
2. ticks the running behaviors,
3. calls the OpMode's own loop code, then updates telemetry (with the loop time).

When the OpMode stops, running behaviors are halted so nothing keeps moving, and the robot's pose is saved for the next OpMode. It also makes `driver` and `operator`. Extend `AutoBase` or `TeleOpBase`, not this class.

## AutoBase

Your whole autonomous is one behavior tree. During init, gamepad 1 picks the alliance, a start delay and any lines you add on the [Auto menu](../sdk/README.md#the-init-menu). Override:

| Method | When | Use it to |
|--------|------|-----------|
| `initialize()` | During init | Add menu lines (`menu().choice(...)`), build paths that don't depend on the choices |
| `initLoop()` | Every loop during init | Show what the camera sees |
| `routine()` (required) | **At start**, once the menu is locked | Set the starting pose, return the whole Auto as one tree |
| `runLoop()` | Every loop after start | Telemetry |

Helpers: `alliance()`, `menu()`, and `pose(x, y, degrees)`, which takes a pose written for blue and flips it for red.

```java
@Autonomous(name = "Score and park")
public class ScoreAndPark extends AutoBase {
    @Override
    protected Node routine() {
        robot.drive.setStartingPose(pose(56, 8, 90));
        return sequence(
                robot.drive.driveTo(pose(60, 84, 135)),
                robot.intake.eject(500),
                robot.drive.driveTo(pose(36, 60, 90)));
    }
}
```

For curved paths, a `static class Paths` inside the Auto holds them. Generate them with the [Pedro Pathing Visualizer](https://visualizer.pedropathing.com) and build them with `drive.pathBuilder()` (in `routine()`, if they depend on the alliance).

## TeleOpBase

Gamepad 1's sticks drive the robot out of the box (field-centric, right bumper for slow, back to reset the heading), starting from where the Auto left it. Override:

| Method | When | Use it to |
|--------|------|-----------|
| `initialize()` | During init | Anything else to set up |
| `bindControls(controls)` (required) | During init | Connect buttons to behaviors |
| `runLoop()` | Every loop after start | Telemetry |
| `driveControls()` | During init | Change how the sticks drive (robot-centric, slow button...) |
| `onEndgame()` / `endgameSeconds()` | At 0:30 left (by default) | Add to the end-game buzz |

Use `timeLeft()` for anything timed.

```java
@Override
protected void bindControls(Bindings controls) {
    controls.onPress(operator.a(), robot.intake.collect());          // runs until done
    controls.whileHeld(operator.rightBumper(), robot.intake.run());  // stops on release
    controls.toggleOnPress(operator.x(), robot.intake.run());        // press on, press off
    controls.onPress(() -> timeLeft() < 10, robot.drive.driveTo(PARK)); // at 0:10, by itself
}
```

While the robot is following a path (for example `robot.drive.driveTo(...)` bound to a button), the sticks are ignored; the driver gets control back when the path ends or is halted.
