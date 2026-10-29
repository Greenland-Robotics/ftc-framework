# Season examples

Three complete robots from past seasons, built only from [sdk parts](../../../sdk/README.md) plus a season subsystem or two that wrap them. Read them to see how a whole robot fits together, or copy one as a starting point when a new game looks similar.

| Folder | Season | Shows |
|--------|--------|-------|
| [`decode/`](decode/) | 2025-26 DECODE | `Roller` intake, `Flywheel` + feeder `ServoPart` wrapped in a `Launcher`, obelisk motif from `TagReader`, a Start choice in the menu |
| [`intothedeep/`](intothedeep/) | 2024-25 INTO THE DEEP | Two `Lift`s (extendo and lift), a color sensor that throws back the other alliance's samples and retries (`ifElse`, `fail()`, `.retry(3)`), `FieldSymmetry.ROTATE`, hanging with the lift |
| [`centerstage/`](centerstage/) | 2023-24 CENTERSTAGE | Team prop from `PropFinder` in your alliance's color, two claws as `ServoPart`s, a drone that only launches in the end game (`when(() -> timeLeft() <= ...)`), a hang winch on `manual` |

Each folder mirrors `robot/src/robot/`:

```
<season>/
├── Robot.java        builds the robot from sdk parts, sets the season's field symmetry
├── subsystems/       season subsystems that wrap parts in the game's words
└── opmode/           a TeleOp and an Auto
```

**To try one**, copy the folder's contents over `robot/src/robot/` (replacing `Robot.java`), rename the hardware names to match your configuration, and measure the poses and positions on your field: the numbers here are plausible, not tuned.

Like everything in `examples/`, these aren't built into the app, but they are checked to compile against the current sdk.
