# examples

Code to learn from and copy. **Nothing in this folder is built into the robot app**, so you can't break the build by editing it.

| Folder | What it is | Needs |
|--------|------------|-------|
| [`general/`](general/) | How behavior trees work on their own: composites, decorators, a reactive guard and a custom Action. Runs on any computer | Only the `framework` module |
| [`ftc/`](ftc/) | A subsystem and OpModes built on `AutoBase`/`TeleOpBase`, Pedro Pathing and robot hardware | The FTC SDK, `pedro` and the `robot` module |

## general/
| File | Shows |
|------|-------|
| [`BehaviorBasics.java`](general/BehaviorBasics.java) | `sequence`, `parallel`, `selector` with `retry`, a `reactiveSequence` guard halting an arm, `withTimeout`, and a custom `Action`, shown live in the behavior tree visualizer |
| [`visualizer/`](general/visualizer/) | The behavior tree visualizer: runs a tree on your computer and shows it in a web page |

### Running the behavior tree visualizer
Pick whichever fits how you work:

| Where | How |
|-------|-----|
| VS Code (with or without the dev container) | **Run and Debug** (▷ in the sidebar) → **Behavior tree visualizer**, or **Terminal** → **Run Task…** → **Run behavior tree visualizer** |
| Android Studio | Choose **Behavior tree visualizer** in the run configuration dropdown next to ▶ and press Run |
| Terminal | `./gradlew :examples:run` |

Then open **http://localhost:8765** (it opens by itself when not in a container). Stop the program to quit.

The page shows every node in the tree and what is happening to it:
- **Step** calls `runner.tick()` once, so you can follow a single tick through the tree. The nodes ticked in that step glow: that is the path the tick took.
- **Play** ticks every 20 ms, like an OpMode. Use **Speed** to slow it down. `delay` and `withTimeout` still go by the clock, not by ticks.
- **Restart** builds the tree again.
- Each node's color is what it returned the last time it was ticked (`RUNNING`, `SUCCESS`, `FAILURE`), or orange if its parent **halted** it. It also shows how many times it was ticked, restarted and halted. Watch the `reactiveSequence` re-tick its battery check every step, and halt the arm when the check fails.
- Anything a node prints appears under that node and in **Printed**. **Robot state** shows the values passed to `watch(...)`.

In the dev container, VS Code forwards port 8765 to your computer (see `forwardPorts` in [`.devcontainer/devcontainer.json`](../.devcontainer/devcontainer.json)), so the same address works in your normal browser. If it doesn't open, check the **Ports** tab in VS Code's bottom panel.

To just print, without the web page:
```bash
./gradlew :examples:run --args=--console
```

To visualize your own tree, copy `BehaviorBasics.java`, change `routine()`, and point `mainClass` in [`build.gradle`](build.gradle) (and the VS Code launch config) at your class:
```java
public static void main(String[] args) throws Exception {
    new TreeVisualizer(MyTree::routine)          // Called again on Restart: reset your "robot" in it
            .watch("arm position", () -> armPosition)
            .start();
}
```
It works with custom nodes too: it finds each node's children with reflection. It is only for running on a computer, so it isn't part of the robot app.

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
