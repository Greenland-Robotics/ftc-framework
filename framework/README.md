# framework

A small **behavior tree** library: building blocks you combine to describe what the robot does. An Auto is one tree; a TeleOp binds trees to buttons.

This module is **plain Java** with no Android or FTC SDK dependencies, so it is small, readable, and tested on a normal computer (`./gradlew :framework:test`, or the **Test framework** task in VS Code). [`examples/general/`](../examples/general/) runs it on a laptop.

You normally *use* these classes from the `robot` module rather than edit them. Every class is short: reading them is a good way to learn how behavior trees work.

## Contents
- [How a behavior tree runs](#how-a-behavior-tree-runs)
- [Building trees](#building-trees)
- [Node reference](#node-reference)
- [Writing your own Action](#writing-your-own-action)
- [Subsystems](#subsystems)
- [Running trees and binding buttons](#running-trees-and-binding-buttons)
- [Design: SOLID in this framework](#design-solid-in-this-framework)
- [Coming from the old Command API](#coming-from-the-old-command-api)

---

## How a behavior tree runs

Everything is a [`Node`](src/behavior/Node.java). Every loop, the OpMode **ticks** the tree's root, which ticks the children it chooses, and so on. Each tick returns a [`Status`](src/behavior/Status.java):

| Status | Meaning |
|--------|---------|
| `RUNNING` | Not done yet: tick me again next loop |
| `SUCCESS` | Finished and did its job |
| `FAILURE` | Finished without doing its job. Selectors, retries and fallbacks react to this |

When a parent stops caring about a child that is still `RUNNING` (a timeout expired, a button was released, a guard stopped holding), it calls the child's **`halt()`**. Nodes that start hardware must stop it in `halt()`, so nothing is ever left running by accident.

> **Never block.** `tick()` runs inside the OpMode loop and must return quickly: no `while` loops, no `sleep()`. To wait, return `RUNNING` (or use `delay`/`waitUntil`).

### Latched vs reactive
- **Latched** (`sequence`, `selector`) remember which child is running and resume there. This is what you want most of the time: "do A, then B, then C".
- **Reactive** (`reactiveSequence`, `reactiveSelector`) start from the first child *every tick*. Put conditions first: if one changes, the running child is halted. Use this for "keep doing X only while Y holds" or "switch to the more important thing as soon as it's possible".

Children before the running one are re-run by reactive composites, so make them conditions, not actions.

---

## Building trees

Add `import static behavior.Behaviors.*;` and build trees from functions; decorators are methods on any node:

```java
Node auto = sequence(
        robot.drive.follow(paths.toGoal),
        robot.intake.eject(),
        selector(                                          // first child to succeed wins
                sequence(
                        parallel(robot.drive.follow(paths.toStack), robot.intake.collect())
                                .withTimeout(3000),        // fail if it takes longer
                        condition(robot.intake::hasSample)
                ).retry(2),                                // two tries...
                robot.drive.driveTo(PARK)                  // ...then give up and park
        )
);
```

Conditions are plain lambdas (`BooleanSupplier`), so anything that returns a boolean works: `() -> gamepad1.a`, `robot.intake::hasSample`.

---

## Node reference

### Composites (many children), in [`behavior.composite`](src/behavior/composite/)
| Function | Class | Does |
|----------|-------|------|
| `sequence(a, b, c)` | [`Sequence`](src/behavior/composite/Sequence.java) | One after another. Succeeds when all succeed; fails at the first failure |
| `selector(a, b, c)` | [`Selector`](src/behavior/composite/Selector.java) | One after another until one succeeds (a fallback). Fails if all fail |
| `reactiveSequence(...)` | [`ReactiveSequence`](src/behavior/composite/ReactiveSequence.java) | A sequence that re-checks earlier children every tick |
| `reactiveSelector(...)` | [`ReactiveSelector`](src/behavior/composite/ReactiveSelector.java) | A selector where a higher-priority child can take over every tick |
| `parallel(a, b)` | [`Parallel`](src/behavior/composite/Parallel.java) + `ALL_SUCCEED` | All at once. Succeeds when all succeed; fails (halting the rest) when one fails |
| `race(a, b)` | [`Parallel`](src/behavior/composite/Parallel.java) + `ANY_SUCCEEDS` | All at once. The first to succeed wins; the rest are halted |
| `ifElse(cond, a, b)` | [`IfElse`](src/behavior/composite/IfElse.java) | Checks `cond` once when it starts, then runs `a` or `b` to the end |
| `when(cond, a)` | [`IfElse`](src/behavior/composite/IfElse.java) | Runs `a` if `cond` is true when it starts; otherwise succeeds |

### Leaves (do something or check something), in [`behavior.leaf`](src/behavior/leaf/)
| Function | Class | Does |
|----------|-------|------|
| `instant(() -> ...)` | [`Instant`](src/behavior/leaf/Instant.java) | Runs code once and succeeds, e.g. set a servo |
| `condition(() -> ...)` | [`Condition`](src/behavior/leaf/Condition.java) | SUCCESS if true right now, FAILURE if not |
| `waitUntil(() -> ...)` | [`WaitUntil`](src/behavior/leaf/WaitUntil.java) | RUNNING until true, then SUCCESS |
| `delay(ms)` | [`Delay`](src/behavior/leaf/Delay.java) | Waits without blocking the robot |
| `succeed()` / `fail()` | | Finish immediately with that result |
| (subclass) | [`Action`](src/behavior/Action.java) | Base class for your own leaves: see below |

### Decorators (wrap one node), in [`behavior.decorator`](src/behavior/decorator/)
| Method | Class | Does |
|--------|-------|------|
| `.withTimeout(ms)` | [`Timeout`](src/behavior/decorator/Timeout.java) | Fails (and halts the node) if it isn't done in time |
| `.retry(n)` | [`Retry`](src/behavior/decorator/Retry.java) | Tries again after failing, `n` tries in total |
| `.repeat(n)` / `.repeatForever()` | [`Repeat`](src/behavior/decorator/Repeat.java) | Runs again after succeeding; stops on failure |
| `.invert()` | [`Invert`](src/behavior/decorator/Invert.java) | Swaps SUCCESS and FAILURE |
| `.alwaysSucceed()` | [`AlwaysSucceed`](src/behavior/decorator/AlwaysSucceed.java) | Reports SUCCESS even on failure, e.g. "try for 2 s, then carry on regardless": `x.withTimeout(2000).alwaysSucceed()` |
| `.onlyWhile(cond)` | [`Guard`](src/behavior/decorator/Guard.java) | Checks `cond` every tick; halts the node and fails as soon as it's false |

---

## Writing your own Action

Extend [`Action`](src/behavior/Action.java) for anything that takes more than one loop. It calls your hooks in order and handles restarting and halting for you:

```java
class RaiseArm extends Action {
    private final DcMotor motor;
    RaiseArm(DcMotor motor) { this.motor = motor; }

    @Override protected void start() {                  // first tick
        motor.setTargetPosition(1200);
        motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        motor.setPower(1);
    }

    @Override protected Status update() {               // every tick
        return motor.isBusy() ? Status.RUNNING : Status.SUCCESS;
    }

    @Override protected void end(boolean interrupted) { // finished or halted
        motor.setPower(0);
    }
}
```

Most Actions live inside a [subsystem](#subsystems), which hands them out from methods like `arm.raise()`.

---

## Subsystems

A [`Subsystem`](src/subsystem/Subsystem.java) is one mechanism (drivetrain, intake, arm) that **owns its hardware**. Nothing else touches its motors or sensors; instead it offers:
- **behaviors**: methods returning a `Node`, like `intake.collect()`
- **checks**: methods returning `boolean`, like `intake.hasSample()`, for conditions
- **`periodic()`**: called once per loop before behaviors tick; read sensors or run controllers here

Subsystems are created in [`robot/src/robot/Robot.java`](../robot/src/robot/Robot.java). See [`examples/ftc/Intake.java`](../examples/ftc/Intake.java) for a complete one.

Two behaviors that drive the same motor at the same time will fight over it (the last `setPower` wins). Design bindings and trees so that one behavior uses a mechanism at a time.

---

## Running trees and binding buttons

| Class | Responsibility |
|-------|----------------|
| [`BehaviorRunner`](src/behavior/runtime/BehaviorRunner.java) | Ticks the active trees once per loop and drops finished ones. `run`, `cancel`, `cancelAll` |
| [`Bindings`](src/behavior/input/Bindings.java) | Connects buttons to trees: `onPress`, `whileHeld`, `toggleOnPress` |
| [`EdgeDetector`](src/behavior/input/EdgeDetector.java) | Turns "button is down" into single PRESSED/RELEASED events |
| [`Clock`](src/behavior/runtime/Clock.java) | Tells time; tests use a fake one |

The OpMode base classes own a runner and bindings and call them every loop, so OpModes only declare trees and bindings.

---

## Design: SOLID in this framework

The framework is small on purpose, so each principle is easy to find in the code:

| Principle | Where to see it |
|-----------|-----------------|
| **S**ingle responsibility | Each class does one thing. `Timeout` only times out, `Retry` only retries: you combine them (`x.withTimeout(3000).retry(2)`) instead of one class with a pile of flags and constructors. `EdgeDetector` detects presses; `Bindings` maps them to behaviors; `BehaviorRunner` runs them |
| **O**pen/closed | New behavior means a new class, not an edit: write an `Action`, a decorator, or a [`ParallelPolicy`](src/behavior/composite/ParallelPolicy.java) (how `parallel` and `race` differ) without touching existing code |
| **L**iskov substitution | Every node honors the same `tick`/`halt` contract, so any node works anywhere: a whole Auto can be one child of a `selector`. The [tests](test/behavior/) check the contract |
| **I**nterface segregation | `Node` has one required method. Conditions are Java's `BooleanSupplier`; `Clock` and `Subsystem` are one method each |
| **D**ependency inversion | Timed nodes depend on the `Clock` interface, not the system clock (which is why they're testable). Subsystems receive their hardware, and OpModes receive subsystems through `Robot`, instead of reaching for globals |

---

## Coming from the old Command API

| Old | New |
|-----|-----|
| `Command` (`init`/`loop`/`isFinished`) | `Node` (`tick` returns a `Status`, plus `halt`); extend `Action` (`start`/`update`/`end`) |
| `SeriesCommand` | `sequence(...)` |
| `ParallelCommand` | `parallel(...)` |
| `InstantCommand` | `instant(() -> ...)` |
| `SleepCommand(ms)` | `delay(ms)` |
| `AwaitCommand(cond)` / `AwaitCommand(cond, ms)` | `waitUntil(cond)` / `waitUntil(cond).withTimeout(ms)` |
| `TimeoutCommand(cmd, ms)` | `cmd.withTimeout(ms)` (now reports FAILURE on timeout; add `.alwaysSucceed()` to carry on) |
| `SwitchCommand(cond, a, b)` | `ifElse(cond, a, b)`, or `when(cond, a)` |
| `CancelCommand` | `runner.cancel(node)`, or `.onlyWhile(cond)` |
| `CommandRunner` | `BehaviorRunner` |
| `ButtonAction` | `controls.onPress(button, node)` in `TeleOpBase.bindControls` |
| `Condition` | `java.util.function.BooleanSupplier` |
| `OpModeBase.INSTANCE.motor` | a subsystem that owns the motor: `robot.intake...` |
