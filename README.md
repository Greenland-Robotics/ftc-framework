# Greenland Robotics Framework: Documentation

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

## Where is my code?
All code is under `TeamCode/src/main/java/gcsrobotics/`. Here you will find the following packages:

- `examples`: Example opmodes and commands (as `.java.md` files) that show how to use the framework for Autos and TeleOps
- `control`: The base classes for all opmodes — `OpModeBase`, `AutoBase`, and `TeleOpBase`. You will only go here to change the hardware configuration in `OpModeBase.java`
- `opmode`: This is where your opmodes will reside. It contains boilerplate files to get you started. **This is where you spend most of your time.**
- `pedroPathing`: Pedro Pathing integration — `Constants.java` (tune this for your robot) and the `Tuning` opmode suite
- `vertices`: The command system. These are the building blocks you combine to create autonomous routines and teleop actions
- `commands`: Pre-built commands like `FollowPath`. Add your own custom commands here

The rest of the repository is FTC SDK plumbing that you should not need to touch:

| Path | What it is |
|------|------------|
| `FtcRobotController/` | The FTC Robot Controller app shell that `TeamCode` is built into |
| `build.common.gradle`, `build.gradle` | FTC SDK build configuration |
| `build.dependencies.gradle` | Library versions (FTC SDK, Pedro Pathing, Panels) — edit this to add a library |
| `gradle/vscode.gradle` | Classpath export used by VS Code (ignored by Android Studio) |
| `.vscode/` | Shared VS Code settings, tasks and recommended extensions |
| `libs/ftc.debug.keystore` | Signing key for debug builds |

---

# Installation / Setup

## 1. Create your repository
1. On the main GitHub page for this repo, click **Use this template** → **Create a new repository** (or click **Fork**). Give it a name.
2. Click the green **Code** button on *your* new repository and copy the URL.

## 2a. Android Studio
1. Open Android Studio and go to **File** → **New** → **Project from Version Control**. Paste in the URL and continue.
2. It may take a few minutes to sync and build, but you'll know you're ready when you see three folders in the left-hand panel: `FtcRobotController`, `TeamCode`, and `Gradle Scripts`.
3. Plug in or connect to your Control Hub and press the green **Run** button to deploy.

## 2b. VS Code
**Prerequisites** (Android Studio installs these for you; without it, install them yourself):
- **JDK 17 or 21** (for example [Temurin](https://adoptium.net/)).
- **The Android SDK**, with `ANDROID_HOME` set to its location (Android Studio puts it at `~/Library/Android/sdk` on macOS, `%LOCALAPPDATA%\Android\Sdk` on Windows and `~/Android/Sdk` on Linux). Alternatively, create a `local.properties` file in the repo root containing `sdk.dir=<path to sdk>`.
- **`adb`** on your `PATH` (it lives in `<sdk>/platform-tools`) for deploying to the robot.

**Setup:**
1. Clone your repository and open the folder in VS Code (**File** → **Open Folder**).
2. Install the recommended **Extension Pack for Java** when prompted.
3. Run **Terminal** → **Run Task…** → **Refresh VS Code classpath**. This downloads the FTC libraries and exports them so VS Code can autocomplete and check your code. Run it again whenever you change `build.dependencies.gradle`.

**Everyday use** (all under **Terminal** → **Run Task…**):

| Task | What it does |
|------|--------------|
| **Build** (`Ctrl/Cmd+Shift+B`) | Compiles the Robot Controller app |
| **Connect to Control Hub (Wi-Fi)** | Connects `adb` to a Control Hub whose Wi-Fi network you have joined (not needed over USB) |
| **Deploy to robot** | Installs the app on the connected Control Hub/phone and starts it |
| **Refresh VS Code classpath** | Updates VS Code's view of the libraries |
| **Clean** | Deletes build outputs |

> VS Code only checks your code for errors as you type; the **Build** task is what actually compiles it with the FTC toolchain. If they ever disagree, trust **Build**.

---

# WARNING: Do NOT write `while` loops
Code that is written inside functions with `loop` in their name(`runLoop()`, `loop()`, etc.) are **automatically** run inside the main loop.
You do NOT have to write a `while (opModeIsActive) {...}` inside of them. Doing so will break the functionality of the entire framework. 
It is exceedingly rare that you would ever have to write one, if at all

# Method Documentation
This documentation covers all classes and methods relevant to writing opmodes with this framework.

---

## Table of Contents
- [`OpModeBase`](#opmodebase)
- [`AutoBase`](#autobase)
- [`TeleOpBase`](#teleopbase)
- [`Constants`](#constants)
- [`Command`](#command-interface)
- [`CommandRunner`](#commandrunner)
- [`SeriesCommand`](#seriescommand)
- [`ParallelCommand`](#parallelcommand)
- [`InstantCommand`](#instantcommand)
- [`SleepCommand`](#sleepcommand)
- [`AwaitCommand`](#awaitcommand)
- [`TimeoutCommand`](#timeoutcommand)
- [`CancelCommand`](#cancelcommand)
- [`SwitchCommand`](#switchcommand)
- [`ButtonAction`](#buttonaction)
- [`FollowPath`](#followpath)

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

## Constants

**Purpose:**
Central location for all tunable robot constants — motor names, directions, odometry pod offsets, and path constraints. This is the **first file you configure** when setting up for a new robot.

### Key Fields

#### Drive Constants (`MecanumConstants driveConstants`)
- Motor names: must match your hardware configuration file in the Driver Station app
  - `rightFrontMotorName`, `rightRearMotorName`, `leftFrontMotorName`, `leftRearMotorName`
- Motor directions: `FORWARD` or `REVERSE` per motor
- `maxPower`: Maximum drive power (0–1)

#### Localizer Constants (`PinpointConstants localizerConstants`)
- `hardwareMapName`: The name of the Pinpoint device in your hardware config (default: `"pinpoint"`)
- `forwardPodY`, `strafePodX`: Pod offsets in the configured `distanceUnit`
- `encoderResolution`: Pod type, e.g. `goBILDA_4_BAR_POD`
- `forwardEncoderDirection`, `strafeEncoderDirection`: `FORWARD` or `REVERSED`

#### Path Constraints (`PathConstraints pathConstraints`)
Limits for velocity and acceleration during path following. Tune these to prevent wheel slip.

#### `public static Follower createFollower(HardwareMap hardwareMap)`
Used internally by `OpModeBase` — builds and returns the configured `Follower`. You don't call this directly.

#### Adding your own constants
Add static fields for servo positions, motor targets, PID coefficients, etc.:
```java
// eg.
public static double CLAW_CLOSED = 0.2;
public static double CLAW_OPEN = 0.8;
public static int ARM_UP = 1200;
```

---

## Command Interface

**Purpose:**
The core interface of the Vertices command system. All commands implement this:

```java
public interface Command {
    void init();          // Called once when the command starts
    void loop();          // Called every loop tick while the command is running
    boolean isFinished(); // Return true to end the command
}
```

To write a custom command, implement this interface. Access robot hardware via `OpModeBase.INSTANCE`:
```java
public class MyCommand implements Command {
    private OpModeBase robot = OpModeBase.INSTANCE;

    public void init() { /* startup logic */ }
    public void loop() { /* per-tick logic */ }
    public boolean isFinished() { return /* done condition */; }
}
```
See `commands/ExampleCommand.java` as a starting template.

---

## CommandRunner

**Purpose:**
Manages and executes a list of active commands. It runs all added commands concurrently, removing each one as it finishes.

### Key Methods

#### `public CommandRunner(Command... commands)`
Constructs a runner pre-loaded with commands (used in `AutoBase.initialize()`).

#### `public void run(Command command)`
Adds and immediately starts a command. Used by `ButtonAction` in teleop.

#### `public void update()`
Called every loop tick — runs `loop()` on all active commands and removes finished ones.

#### `public boolean isFinished()`
Returns `true` when all commands have completed.

---

## SeriesCommand

**Purpose:**
Runs a list of commands **one at a time**, in order. The next command starts only after the previous one finishes.

```java
new SeriesCommand(
    new FollowPath(paths.toGoal),
    new Shoot(),
    new FollowPath(paths.toIntake)
)
```

#### `addCommands(Command... commands)`
Dynamically appends more commands before the series starts.

---

## ParallelCommand

**Purpose:**
Runs multiple commands **simultaneously**. Finishes when **all** commands have finished.

```java
new ParallelCommand(
    new FollowPath(paths.toIntake),
    new StartIntake()
)
```

#### `addCommands(Command... commands)`
Appends more commands before execution starts.

---

## InstantCommand

**Purpose:**
Executes a single `Runnable` action on the first loop tick, then immediately finishes. Great for one-liners like setting a servo position.

```java
new InstantCommand(() -> claw.setPosition(0.5))
```

---

## SleepCommand

**Purpose:**
Pauses the command chain for a specified number of milliseconds without blocking the rest of the program.

```java
new SleepCommand(1000) // wait 1 second
```
Unlike `Thread.sleep()`, this keeps the opmode loop running while waiting.

---

## AwaitCommand

**Purpose:**
Blocks the command chain until a `Condition` becomes `true`. Optionally times out after a set duration.

```java
// Wait indefinitely
new AwaitCommand(() -> sensor.getDistance() < 5)

// Wait with a 3-second timeout
new AwaitCommand(() -> sensor.getDistance() < 5, 3000)
```
`Condition` is a functional interface: `boolean getValue()`.

---

## TimeoutCommand

**Purpose:**
Wraps another command and forcefully finishes it if it takes longer than the specified timeout.

```java
new TimeoutCommand(new FollowPath(paths.toGoal), 3000) // give up after 3 seconds
```

---

## CancelCommand

**Purpose:**
Wraps one or more commands and gives you external control to cancel or pause them mid-execution.

### Key Methods
- `cancel()` — Permanently stops all wrapped commands and marks as finished
- `disable()` — Temporarily pauses the wrapped commands
- `enable()` — Resumes paused commands
- `isDisabled()` — Returns the current disabled state

```java
CancelCommand movingArm = new CancelCommand(new SeriesCommand(armUp, deliver));
// later:
movingArm.cancel(); // abort if something goes wrong
```

---

## SwitchCommand

**Purpose:**
Evaluates a `Condition` at the moment it starts, and runs either the `action` command (if `true`) or a `fallback` command (if `false`). Think of it as an `if/else` for commands.

```java
// With fallback
new SwitchCommand(
    () -> isRed,       // condition
    new DriveLeft(),   // run if true
    new DriveRight()   // run if false
)

// Without fallback (no-op if false)
new SwitchCommand(() -> hasSample, new Shoot())
```

---

# How to Use This Framework

1. **Configure `Constants.java`** — set motor names, directions, and pod offsets to match your hardware config
2. **Add your hardware** to `initHardware()` in `OpModeBase.java`
3. **Create an OpMode** in the `opmode/` package:
    - Autonomous: extend `AutoBase` — use `BoilerplateAuto.java` as your starting point
    - TeleOp: extend `TeleOpBase` — use `BoilerplateTeleOp.java` as your starting point
4. **Override the required abstract methods** (`buildCommands`, `initialize`, `runLoop`, etc.)
5. **Build your command trees** using `SeriesCommand`, `ParallelCommand`, `FollowPath`, and your own custom `Command` classes
6. **Wire buttons to actions** in teleop using `ButtonAction`
7. **Tune** using the `Tuning` opmode suite (in `pedroPathing/Tuning.java`)

---

## Questions?
- Each class and method is documented with purpose, usage, and examples in this file.
- For detailed code reference, browse the `.java` files under `TeamCode/src/main/java/gcsrobotics/`.
- For path generation, use the [Pedro Pathing Visualizer](https://visualizer.pedropathing.com).
- For FTC SDK reference, see [FTC documentation](https://ftc-docs.firstinspires.org/).
- For advanced help, use an AI such as [Claude](claude.ai) or [ChatGPT](chatgpt.com) with your code attached
- If nothing else works, ask Josh.
