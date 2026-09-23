# robot

**Your robot code.** This is where you spend most of your time.
This module is the Robot Controller app: the FTC SDK builds it, together with `framework` and `pedro`, into the app installed on the robot.

Code lives in [`src/robot/`](src/robot/):

| Package | What goes here |
|---------|----------------|
| [`opmode/`](src/robot/opmode/) | Your Autos and TeleOps. Start by copying a boilerplate from [`examples/ftc/`](../examples/ftc/) |
| [`commands/`](src/robot/commands/) | Your custom `Command` classes (e.g. `Shoot`, `StartIntake`) |
| [`control/`](src/robot/control/) | The OpMode base classes. Edit `OpModeBase.java` to add your robot's hardware |

Examples and boilerplates to copy from are in [`examples/ftc/`](../examples/ftc/).

## Writing your robot code
1. **Configure [`Constants.java`](../pedro/src/pedro/Constants.java)** in the `pedro` module — set motor names, directions, and pod offsets to match your hardware config
2. **Add your hardware** to `initHardware()` in `OpModeBase.java`
3. **Create an OpMode** in the `opmode/` package:
    - Autonomous: extend `AutoBase` — copy [`examples/ftc/BoilerplateAuto.java`](../examples/ftc/BoilerplateAuto.java) as your starting point
    - TeleOp: extend `TeleOpBase` — copy [`examples/ftc/BoilerplateTeleOp.java`](../examples/ftc/BoilerplateTeleOp.java) as your starting point
4. **Override the required abstract methods** (`buildCommands`, `initialize`, `runLoop`, etc.)
5. **Build your command trees** using [`SeriesCommand`, `ParallelCommand`](../framework/README.md), [`FollowPath`](../pedro/README.md#followpath), and your own custom `Command` classes
6. **Wire buttons to actions** in teleop using `ButtonAction`
7. **Tune** using the `Tuning` opmode suite (see [`pedro/`](../pedro/README.md))

---

## WARNING: Do NOT write `while` loops
Code that is written inside functions with `loop` in their name(`runLoop()`, `loop()`, etc.) are **automatically** run inside the main loop.
You do NOT have to write a `while (opModeIsActive) {...}` inside of them. Doing so will break the functionality of the entire framework. 
It is exceedingly rare that you would ever have to write one, if at all

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

  These are forwarded to Pedro Pathing's `setTeleOpDrive()`. Using these inputs elsewhere in your teleop code means that one action will control both driving and whatever else you mapped it to. If you need to change or override the drive logic, see `TeleOpBase.loopInternal()`.

---

## OpModeBase

**Purpose:**
Abstract base for all OpModes (autonomous or teleop).
Handles hardware initialization and provides access to the Pedro Pathing follower and a shared `INSTANCE` reference used by commands.

### Key Properties
- `follower` — The Pedro Pathing `Follower` instance, used for all path following and pose tracking
- `INSTANCE` — A static, volatile reference to the currently running opmode. Used by commands to access robot hardware via `OpModeBase.INSTANCE`

### Main Methods

#### `public double getX()`, `public double getY()`, `public double getHeading()`
Returns the robot's current X position, Y position, or heading, respectively, as reported by the Pedro Pathing localizer. Available in any Auto or TeleOp that extends this class.

#### `private void initHardware()`
Initializes all hardware. You will need to modify this method for your specific robot — declare and configure motors, servos, and any other devices here.

The following are **internal** methods. You won't need to interact with them directly.

#### `protected abstract void initInternal()`
Implemented by `AutoBase` and `TeleOpBase`. Runs during the init phase before `waitForStart()`.

#### `protected abstract void loopInternal()`
Implemented by `AutoBase` and `TeleOpBase`. Runs every iteration of the main loop after start.

#### `public void runOpMode()`
The main entrypoint for the opmode. Calls `initHardware()`, creates the follower, sets `INSTANCE`, calls `initInternal()`, waits for start, then runs the main loop.

---

## AutoBase

**Purpose:**
`AutoBase` is an abstract class for autonomous opmodes.
It extends `OpModeBase` and integrates the command system, so your entire autonomous routine is expressed as a tree of commands that run automatically.

### Common Usage
- Extend `AutoBase` in your autonomous opmode class.
- Override `buildCommands()` to construct your command objects.
- Override `initialize()` to set the robot's starting pose, build paths, and assemble the `CommandRunner`.
- Override `runLoop()` for anything you want to run continuously alongside the commands (e.g., telemetry).

### What does "Override" mean?
Overriding means providing your own implementation of a method that is declared in a parent class:
```java
@Override // Not strictly required but recommended for readability
protected void initialize() {
    follower.setStartingPose(new Pose(0, 0, Math.toRadians(0)));
}
```
These methods are declared in `AutoBase` but *you* define what they actually do.

### Key Methods

#### `protected abstract void buildCommands()`
Override to instantiate your `Command` objects.
```java
@Override
protected void buildCommands() {
    driveToGoal = new SeriesCommand(
        new FollowPath(paths.toGoal),
        new Shoot()
    );
}
```

#### `protected abstract void initialize()`
Override to set the starting pose, build your `Paths`, and create the `CommandRunner` with the full sequence.
```java
@Override
protected void initialize() {
    follower.setStartingPose(new Pose(26, 128, Math.toRadians(-38)));
    paths = new Paths(follower);
    commandRunner = new CommandRunner(new SeriesCommand(
        driveToGoal,
        intakeAndScore
    ));
}
```

#### `protected abstract void runLoop()`
Override to add logic that runs every loop tick during the autonomous (alongside command execution). Great for telemetry:
```java
@Override
protected void runLoop() {
    telemetry.addData("x", getX());
    telemetry.addData("y", getY());
}
```

### The `Paths` inner class
By convention, a `static class Paths` inside your Auto holds all `PathChain` objects. Generate path code from [Pedro Pathing Visualizer](https://visualizer.pedropathing.com) and paste it in here.

---

## TeleOpBase

**Purpose:**
Base class for teleop opmodes. Integrates Pedro Pathing's teleop drive and the command system for button-triggered actions.

### Key Properties
- `commandRunner` — A `CommandRunner` instance, available for use with `ButtonAction`
- `driveMode` — Set to `false` to temporarily disable the driver-controlled drive (e.g., when a path command takes over)

### Key Methods

#### `protected abstract void initialize()`
Override to set up your `ButtonAction` objects and any other init logic.

#### `protected abstract void runLoop()`
Override to define your main teleop loop. Call `buttonAction.update(gamepad.button)` here for each action.

### Built-in Drive Logic
Each loop tick, `TeleOpBase` automatically calls:
```java
follower.setTeleOpDrive(
    -gamepad1.left_stick_y,
    -gamepad1.left_stick_x,
    -gamepad1.right_stick_x,
    false
);
```
This gives you full mecanum drive out of the box. Set `driveMode = false` to pause it (useful when a `FollowPath` command is running in teleop).

---
