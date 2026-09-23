# Greenland Robotics FTC Framework

A template for FIRST Tech Challenge robots. Build Autos and TeleOps by combining small **commands** ("drive this path", "shoot", "wait 1 second") instead of writing long `while` loops, with [Pedro Pathing](https://pedropathing.com) for driving and path following.

Works with **Android Studio** and **VS Code**, including a dev container that needs no Java or Android SDK on your computer: just run `./open-workspace`.

## Repository map

```
ftc-framework/
├── robot/               ← YOUR ROBOT CODE: hardware, OpModes, custom commands
├── framework/           ← The command framework (Command, SeriesCommand, ...)
├── pedro/               ← Pedro Pathing: drivetrain config, tuning OpModes, FollowPath
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
| [`robot/`](robot/README.md) | The app installed on the robot: your OpModes, commands and hardware (`control/OpModeBase.java`) | All the time |
| [`pedro/`](pedro/README.md) | `Constants.java` (motor names, odometry, tuned values), the **Tuning** OpMode, `FollowPath` | When setting up or tuning a robot |
| [`framework/`](framework/README.md) | The command building blocks. Plain Java, no Android | Only to add new kinds of commands |
| [`examples/`](examples/README.md) | General command examples you can run on a laptop (with a live web view of the command tree), and FTC OpMode examples/boilerplates | Never: copy from it |
| [`docs/`](docs/setup.md) | Setup guide and licenses | Never |
| `FtcRobotController/` | The FTC Robot Controller app shell | Never |
| `open-workspace`, [`scripts/`](scripts/bootstrap.sh), [`.devcontainer/`](.devcontainer/) | The dev container and the scripts that set it up. See [setup option A](docs/setup.md#a-vs-code-dev-container-recommended) | Rarely |

Each folder's README explains what's inside and documents its classes.

## Where do I...

| I want to... | Go to |
|--------------|-------|
| Set up my computer and deploy to the robot | [`docs/setup.md`](docs/setup.md) |
| Write an Auto or TeleOp | Copy a boilerplate from [`examples/ftc/`](examples/ftc/) into [`robot/src/robot/opmode/`](robot/src/robot/opmode/) |
| Add motors, servos or sensors | `initHardware()` in [`OpModeBase.java`](robot/src/robot/control/OpModeBase.java) |
| Write a custom command (e.g. `Shoot`) | [`robot/src/robot/commands/`](robot/src/robot/commands/) |
| Set drive motor names or odometry | [`Constants.java`](pedro/src/pedro/Constants.java) |
| Tune Pedro Pathing | [`pedro/README.md`](pedro/README.md#tuning-a-new-robot) |
| Look up a command (`SeriesCommand`, `AwaitCommand`, ...) | [`framework/README.md`](framework/README.md) |
| Learn how commands work, without a robot | Run the [command tree visualizer](examples/README.md#running-the-command-tree-visualizer) |
| Add a library | `build.dependencies.gradle` |

## Quick start
1. Click **Use this template** on GitHub to create your team's repository and clone it. On a Mac, run `./open-workspace` to get a ready-to-go VS Code; otherwise follow [`docs/setup.md`](docs/setup.md).
2. Set your drive motor names and odometry in [`Constants.java`](pedro/src/pedro/Constants.java), deploy, and run the **Tuning** OpMode.
3. Add your hardware to [`OpModeBase.java`](robot/src/robot/control/OpModeBase.java).
4. Copy `BoilerplateAuto.java` / `BoilerplateTeleOp.java` from [`examples/ftc/`](examples/ftc/) into `robot/src/robot/opmode/` and start building commands. See the [robot guide](robot/README.md).

## How the modules fit together

```
robot ──► pedro ──► framework
  │                    ▲
  └────────────────────┘
  └──► FtcRobotController (FTC SDK)
```

`framework` knows nothing about the robot; `pedro` adds driving on top of it; `robot` uses both and is what gets installed on the robot.

| Module | Java package | Example import |
|--------|--------------|----------------|
| `framework` | `commands` | `import commands.SeriesCommand;` |
| `pedro` | `pedro` | `import pedro.FollowPath;` |
| `robot` | `robot.control`, `robot.opmode`, `robot.commands` | `import robot.control.AutoBase;` |

## Questions?
- Each module's README documents its classes with purpose, usage and examples.
- For detailed code reference, browse the `.java` files in each module's `src/` folder.
- For path generation, use the [Pedro Pathing Visualizer](https://visualizer.pedropathing.com).
- For FTC SDK reference, see [FTC documentation](https://ftc-docs.firstinspires.org/).
- For advanced help, use an AI such as [Claude](https://claude.ai) or [ChatGPT](https://chatgpt.com) with your code attached
- If nothing else works, ask Josh.
