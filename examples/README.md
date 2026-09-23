# examples

Code to learn from and copy. **Nothing in this folder is built into the robot app**, so you can't break the build by editing it.

| Folder | What it is | Needs |
|--------|------------|-------|
| [`general/`](general/) | How behavior trees work on their own: composites, decorators, a reactive guard and a custom Action. Runs on any computer | Only the `framework` module |
| [`ftc/`](ftc/) | A subsystem and OpModes built on `AutoBase`/`TeleOpBase`, Pedro Pathing and robot hardware | The FTC SDK, `pedro` and the `robot` module |

## general/
| File | Shows |
|------|-------|
| [`BehaviorBasics.java`](general/BehaviorBasics.java) | `sequence`, `parallel`, `selector` with `retry`, a `reactiveSequence` guard halting an arm, `withTimeout`, and a custom `Action`, driven by a stand-in main loop |

Run it from the repository root to watch the tree execute:
```bash
javac -d build/examples $(find framework/src -name '*.java') examples/general/BehaviorBasics.java
java -cp build/examples BehaviorBasics
```

## ftc/
Copy these into `robot/src/robot/opmode/` (OpModes) or `robot/src/robot/subsystems/` (subsystems). Their `package` lines already match those folders.

| File | What it is |
|------|------------|
| [`BoilerplateAuto.java`](ftc/BoilerplateAuto.java) | Empty Auto to start from |
| [`BoilerplateTeleOp.java`](ftc/BoilerplateTeleOp.java) | Empty TeleOp to start from |
| [`Intake.java`](ftc/Intake.java) | A complete subsystem: owns a motor and a sensor, offers `collect()`/`eject()`/`run()` and `hasSample()`, with a custom `Action` inside |
| [`ExampleAuto.java`](ftc/ExampleAuto.java) | Scores, tries to grab another sample, and uses a `selector` to park if that fails |
| [`ExampleTeleOp.java`](ftc/ExampleTeleOp.java) | A TeleOp with `onPress`, `toggleOnPress` and `whileHeld` bindings |

`ExampleAuto` and `ExampleTeleOp` use `robot.intake`: copy `Intake.java` into `robot/src/robot/subsystems/` and register it in `Robot.java` (the comments there show how). Rename the hardware names (`"intake"`, `"intakeSensor"`) to match your robot's configuration.
