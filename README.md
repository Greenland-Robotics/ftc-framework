# Greenland Robotics FTC Framework

A template for FIRST Tech Challenge robots. Build Autos and TeleOps as **behavior trees**: small building blocks ("drive this path", "collect a sample", "try this, otherwise that") combined into routines, instead of long `while` loops. Each mechanism is a **subsystem** that owns its hardware. [Pedro Pathing](https://pedropathing.com) handles driving and path following.

Works with **Android Studio** and **VS Code**, including a dev container that needs no Java or Android SDK on your computer: just run `./open-workspace`.

## Repository map

```
ftc-framework/
├── robot/               ← YOUR ROBOT CODE: subsystems, OpModes
├── framework/           ← The behavior tree framework (sequence, selector, Action, ...)
├── pedro/               ← Pedro Pathing: drivetrain config, tuning OpModes, the Drive subsystem
├── examples/            ← Examples to learn from and copy into robot/
├── docs/                ← Setup guide and licenses
├── FtcRobotController/  ← FTC SDK app shell (don't edit)
├── scripts/             ← bootstrap.sh: installs and runs the dev container tooling
├── open-workspace       ← Opens VS Code inside the dev container
├── .devcontainer/       ← The dev container (Java, Android SDK, adb)
└── build files          ← Gradle build (rarely edited)
```

| Folder | What it is | How often you edit it |
|--------|------------|-----------------------|
| [`robot/`](robot/README.md) | The app installed on the robot: `Robot.java`, your subsystems and OpModes | All the time |
| [`pedro/`](pedro/README.md) | `Constants.java` (motor names, odometry, tuned values), the **Tuning** OpMode, the `Drive` subsystem | When setting up or tuning a robot |
| [`framework/`](framework/README.md) | The behavior tree building blocks and `Subsystem`. Plain Java, no Android, unit-tested | Only to add new kinds of nodes |
| [`examples/`](examples/README.md) | A behavior tree demo you can run on a laptop, and FTC examples (subsystem, Auto, TeleOp) and boilerplates | Never: copy from it |
| [`docs/`](docs/setup.md) | Setup guide and licenses | Never |
| `FtcRobotController/` | The FTC Robot Controller app shell | Never |
| `open-workspace`, [`scripts/`](scripts/bootstrap.sh), [`.devcontainer/`](.devcontainer/) | The dev container and the scripts that set it up. See [setup option A](docs/setup.md#a-vs-code-dev-container-recommended) | Rarely |

Each folder's README explains what's inside and documents its classes.

## Where do I...

| I want to... | Go to |
|--------------|-------|
| Set up my computer and deploy to the robot | [`docs/setup.md`](docs/setup.md) |
| Write an Auto or TeleOp | Copy a boilerplate from [`examples/ftc/`](examples/ftc/) into [`robot/src/robot/opmode/`](robot/src/robot/opmode/) |
| Add a mechanism (motors, servos, sensors) | A subsystem in [`robot/src/robot/subsystems/`](robot/src/robot/subsystems/), registered in [`Robot.java`](robot/src/robot/Robot.java) |
| Write a custom behavior (e.g. raise the arm) | An `Action` inside your subsystem. See [`framework/README.md`](framework/README.md#writing-your-own-action) |
| Set drive motor names or odometry | [`Constants.java`](pedro/src/pedro/Constants.java) |
| Tune Pedro Pathing | [`pedro/README.md`](pedro/README.md#tuning-a-new-robot) |
| Look up a node (`sequence`, `selector`, `retry`, ...) | [`framework/README.md`](framework/README.md#node-reference) |
| Learn how behavior trees work, without a robot | [`examples/general/`](examples/general/) |
| Add a library | `build.dependencies.gradle` |

## Quick start
1. Click **Use this template** on GitHub to create your team's repository and clone it. On a Mac, run `./open-workspace` to get a ready-to-go VS Code; otherwise follow [`docs/setup.md`](docs/setup.md).
2. Set your drive motor names and odometry in [`Constants.java`](pedro/src/pedro/Constants.java), deploy, and run the **Tuning** OpMode.
3. Add a subsystem for each mechanism and register it in [`Robot.java`](robot/src/robot/Robot.java). [`examples/ftc/Intake.java`](examples/ftc/Intake.java) shows how.
4. Copy `BoilerplateAuto.java` / `BoilerplateTeleOp.java` from [`examples/ftc/`](examples/ftc/) into `robot/src/robot/opmode/` and build your behavior trees. See the [robot guide](robot/README.md).

## How the modules fit together

```
robot ──► pedro ──► framework
  │                    ▲
  └────────────────────┘
  └──► FtcRobotController (FTC SDK)
```

`framework` knows nothing about the robot; `pedro` adds the `Drive` subsystem on top of it; `robot` builds the robot from subsystems and is what gets installed on it.

| Module | Java package | Example import |
|--------|--------------|----------------|
| `framework` | `behavior`, `behavior.*`, `subsystem` | `import static behavior.Behaviors.*;` |
| `pedro` | `pedro` | `import pedro.Drive;` |
| `robot` | `robot`, `robot.control`, `robot.opmode`, `robot.subsystems` | `import robot.control.AutoBase;` |

## Questions?
- Each module's README documents its classes with purpose, usage and examples.
- For detailed code reference, browse the `.java` files in each module's `src/` folder.
- For path generation, use the [Pedro Pathing Visualizer](https://visualizer.pedropathing.com).
- For FTC SDK reference, see [FTC documentation](https://ftc-docs.firstinspires.org/).
- For advanced help, use an AI such as [Claude](https://claude.ai) or [ChatGPT](https://chatgpt.com) with your code attached
- If nothing else works, ask Josh.
