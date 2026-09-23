# examples

Code to learn from and copy. **Nothing in this folder is built into the robot app**, so you can't break the build by editing it.

| Folder | What it is | Needs |
|--------|------------|-------|
| [`general/`](general/) | How the command framework works on its own: combining commands and writing a custom one. Runs on any computer | Only the `framework` module |
| [`ftc/`](ftc/) | FTC OpModes and commands built on `AutoBase`/`TeleOpBase`, Pedro Pathing and robot hardware | The FTC SDK, `pedro` and the `robot` module |

## general/
| File | Shows |
|------|-------|
| [`CommandBasics.java`](general/CommandBasics.java) | `SeriesCommand`, `ParallelCommand`, `InstantCommand`, `SleepCommand`, `AwaitCommand`, `SwitchCommand`, `TimeoutCommand` and a custom command, driven by a stand-in main loop |

Run it from the repository root to watch the commands execute:
```bash
javac -d build/examples framework/src/commands/*.java examples/general/CommandBasics.java
java -cp build/examples CommandBasics
```

## ftc/
Copy these into `robot/src/robot/opmode/` (OpModes) or `robot/src/robot/commands/` (commands). Their `package` lines already match those folders.

| File | What it is |
|------|------------|
| [`BoilerplateAuto.java`](ftc/BoilerplateAuto.java) | Empty Auto to start from |
| [`BoilerplateTeleOp.java`](ftc/BoilerplateTeleOp.java) | Empty TeleOp to start from |
| [`ExampleAuto.java`](ftc/ExampleAuto.java) | A complete Auto: paths from the Pedro visualizer, shooting and intaking commands |
| [`ExampleTeleOp.java`](ftc/ExampleTeleOp.java) | A TeleOp with button-triggered command sequences |
| [`ExampleCommand.java`](ftc/ExampleCommand.java) | Template for a custom command that uses robot hardware |

The full examples use hardware and commands (`intake`, `servo`, `Shoot`, `StartIntake`) that a real robot would define, so they won't compile until you add those to your robot.
