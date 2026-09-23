# examples

Code to learn from and copy. **Nothing in this folder is built into the robot app**, so you can't break the build by editing it.

| Folder | What it is | Needs |
|--------|------------|-------|
| [`general/`](general/) | How the command framework works on its own: combining commands and writing a custom one. Runs on any computer | Only the `framework` module |
| [`ftc/`](ftc/) | FTC OpModes and commands built on `AutoBase`/`TeleOpBase`, Pedro Pathing and robot hardware | The FTC SDK, `pedro` and the `robot` module |

## general/
| File | Shows |
|------|-------|
| [`CommandBasics.java`](general/CommandBasics.java) | `SeriesCommand`, `ParallelCommand`, `InstantCommand`, `SleepCommand`, `AwaitCommand`, `SwitchCommand`, `TimeoutCommand` and a custom command, shown live in the command tree visualizer |
| [`visualizer/`](general/visualizer/) | The command tree visualizer: runs a command tree on your computer and shows it in a web page |

### Running the command tree visualizer
Pick whichever fits how you work:

| Where | How |
|-------|-----|
| VS Code (with or without the dev container) | **Run and Debug** (▷ in the sidebar) → **Command tree visualizer**, or **Terminal** → **Run Task…** → **Run command tree visualizer** |
| Android Studio | Choose **Command tree visualizer** in the run configuration dropdown next to ▶ and press Run |
| Terminal | `./gradlew :examples:run` |

Then open **http://localhost:8765** (it opens by itself when not in a container). Stop the program to quit.

The page shows every command in the tree and what is happening to it:
- **Step** calls `commandRunner.start()` the first time, then `commandRunner.update()` once per press, so you can follow each tick.
- **Play** calls `update()` every 20 ms, like an OpMode. Use **Speed** to slow it down. `SleepCommand` and timeouts still go by the clock, not by ticks.
- **Restart** builds the routine again.
- Each command shows whether it is running, done, stopped by its parent (e.g. a `TimeoutCommand`) or skipped (e.g. the branch a `SwitchCommand` didn't take), plus how many times its `init()` and `loop()` ran.
- Anything a command prints appears under that command and in **Printed**. **Robot state** shows the values passed to `watch(...)`.

In the dev container, VS Code forwards port 8765 to your computer (see `forwardPorts` in [`.devcontainer/devcontainer.json`](../.devcontainer/devcontainer.json)), so the same address works in your normal browser. If it doesn't open, check the **Ports** tab in VS Code's bottom panel.

To visualize your own tree, copy `CommandBasics.java`, change `routine()`, and point `mainClass` in [`build.gradle`](build.gradle) (or the VS Code launch config) at your class:
```java
public static void main(String[] args) throws Exception {
    new TreeVisualizer(MyTree::routine)          // Called again on Restart: reset your "robot" in it
            .watch("arm position", () -> armPosition)
            .start();
}
```
It works with custom commands too: it finds each command's children with reflection. It runs on a computer only (it uses the JDK's built-in web server), so it isn't part of the robot app.

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
