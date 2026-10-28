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
| [`hive/`](general/hive/) | This season's hive game, simulated: drive a robot with the keyboard and watch its TeleOp tree react. The Auto and TeleOp share one tree |

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
- Click ▼ on a node to fold it up (Alt-click folds everything inside it too). A folded node still lists what is running inside it. **Collapse** folds everything except the top two levels and anything running in parallel; **Expand all** opens it all again. What you fold stays folded after Restart.
- Drag the bar between the tree and the side panels to give either more room; double-click it to reset. With a field, **⛶ Full screen** (or double-clicking the field) shows just the field, as big as it fits, with what the tree is running listed underneath. <kbd>Esc</kbd> leaves.
- Name a node with `.named("...")` (e.g. `condition(() -> gamepad1.a).named("A held?")`) and the page shows that name instead of just `Condition`.

If port 8765 is taken, pick another: `./gradlew :examples:run -Dvisualizer.port=8766` (add `-Dvisualizer.browser=false` to not open a browser).

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

For a simulated robot, the visualizer can also draw a picture next to the tree and pass keys to your program (the hive game below uses all of these):

| Method | Does |
|--------|------|
| `.afterEachTick(() -> ...)` | Runs after every tick: move your pretend world one loop |
| `.scene(() -> json)` | Draws shapes (rectangles, circles, lines, text) next to the tree, many times a second. See [`FieldDrawing.java`](general/hive/FieldDrawing.java) |
| `.key("Drive forward", "ArrowUp")` | Passes that key to the program instead of using it as a page shortcut, and lists it under **Controls**. Read it with `isHeld("ArrowUp")` |
| `.setting("Match", value -> ..., "A", "B")` | A drop-down in the page; changing it calls your code and restarts |

### The hive game
[`hive/`](general/hive/) simulates this season's game so you can practice the TeleOp, and watch its behavior tree, without a robot:

| Where | How |
|-------|-----|
| VS Code | **Run and Debug** → **Hive game TeleOp**, or **Run Task…** → **Run hive game TeleOp** |
| Android Studio | The **Hive game TeleOp** run configuration |
| Terminal | `./gradlew :examples:hive` |

Open **http://localhost:8765** and press **Play**. You drive the red robot:

| Keys | Does |
|------|------|
| <kbd>↑</kbd> <kbd>↓</kbd> | Drive forward / back |
| <kbd>←</kbd> <kbd>→</kbd> | Turn |
| <kbd>Q</kbd> <kbd>E</kbd> | Slide sideways (mecanum) |
| <kbd>Shift</kbd> | Drive slowly while held |
| <kbd>Space</kbd> | Intake, while held (holds 3 balls) |
| <kbd>F</kbd> | Shoot a ball; hold to keep shooting |
| <kbd>A</kbd> | Line up a shot, while held: the robot drives itself to a spot in range of your hive's up end, facing it (the same behavior the Auto uses) |

**The game.** Each alliance has its own hive: a seesaw with a cup at each end. Balls that land in a cup weigh down that end (a wiffle ball is 1 unit, a wiffle softball 3). When one end outweighs the other by 20 units, the seesaw pivots to it: **+20 points** for that alliance, and the cup that went down tips its balls back out onto the floor. So you score by filling whichever end of your hive is up, again and again. The dotted line in front of your robot shows where a shot would land; it turns green when it would land in the up end's cup.

The blue robot plays by itself: it runs the Auto. Use the drop-downs to take it off the field, or to play a full match (30 s Auto, then the 2:00 TeleOp). The speed menu slows the whole game down.

**One tree for Auto and TeleOp.** [`HiveRoutines.java`](general/hive/HiveRoutines.java) builds both from the same pieces: `shootOne` (spin up the flywheel, then feed a ball), `lineUpShot` and `collectBall`. The Auto chains them together by itself; the TeleOp runs three lanes in parallel (wheels, intake, shooter), each waiting on a driver button before starting one of those pieces. Fold the lanes up with **Collapse** to see at a glance what each one is doing.

The routines only talk to the subsystems (`Drive`, `Intake`, `Shooter`) and a `Driver`, never to the simulation. To run them on the robot, give them subsystems that own real hardware, with the same methods, and a `Driver` that reads the gamepad.

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
