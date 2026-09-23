# framework

The command framework: small building blocks that you implement or combine to describe what the robot does.
Autos are one big tree of commands; TeleOps bind commands to buttons.

This module is **plain Java** with no Android or FTC SDK dependencies, so it stays small, reusable and testable on a normal computer. [`examples/general/`](../examples/general/) shows it running on a laptop.
You normally *use* these classes from the `robot` module rather than edit them.

All classes are in the `commands` package (`import commands.*;`), in [`src/commands/`](src/commands/).

| Kind | Classes |
|------|---------|
| The interface you implement | [`Command`](#command-interface), [`Condition`](#awaitcommand) |
| Run commands | [`CommandRunner`](#commandrunner), [`ButtonAction`](#buttonaction) |
| Combine commands | [`SeriesCommand`](#seriescommand), [`ParallelCommand`](#parallelcommand), [`SwitchCommand`](#switchcommand), [`TimeoutCommand`](#timeoutcommand), [`CancelCommand`](#cancelcommand) |
| Ready-made leaf commands | [`InstantCommand`](#instantcommand), [`SleepCommand`](#sleepcommand), [`AwaitCommand`](#awaitcommand) |

> **Never block.** `init()`, `loop()` and `isFinished()` are called from the OpMode's main loop, so they must return quickly. Do not use `while` loops or `Thread.sleep()`; use `SleepCommand`/`AwaitCommand` instead.

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

To write a custom command, implement this interface. In the `robot` module, access robot hardware via `OpModeBase.INSTANCE`:
```java
public class MyCommand implements Command {
    private OpModeBase robot = OpModeBase.INSTANCE;

    public void init() { /* startup logic */ }
    public void loop() { /* per-tick logic */ }
    public boolean isFinished() { return /* done condition */; }
}
```
Custom commands belong in [`robot/src/robot/commands/`](../robot/src/robot/commands/). See [`examples/ftc/ExampleCommand.java`](../examples/ftc/ExampleCommand.java) for a starting template.

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

## ButtonAction

**Purpose:**
Runs a command on a `CommandRunner` when a button is **pressed** (the moment it goes from released to pressed), not continuously while it is held.

```java
// In TeleOp initialize():
shoot = new ButtonAction(new SeriesCommand(...), commandRunner);

// In TeleOp runLoop():
shoot.update(gamepad2.a);
```
