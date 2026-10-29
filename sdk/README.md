# sdk

**Ready-made robot parts and helpers**, so a new mechanism is one line in `Robot.java` instead of a page of motor code. Every part is a [subsystem](../framework/README.md#subsystems) that hands out behavior tree nodes (`robot.lift.goTo(HIGH)`, `robot.claw.setTo(OPEN)`), and each comes with guardrails: soft limits, safe stops, no motor fighting, readable errors.

The parts are what FTC games have needed season after season ([table below](#what-past-games-needed)). You use them; you rarely edit them.

## Contents
- [Quick start](#quick-start)
- [What's in it](#whats-in-it)
- [Parts](#parts): Lift, Arm, Roller, ServoPart, Flywheel
- [Gamepads and driving](#gamepads-and-driving)
- [Auto helpers](#auto-helpers): the init menu, alliance poses
- [Vision](#vision)
- [Tuning](#tuning): auto-tune or tune by hand, save on the robot
- [Guardrails](#guardrails)
- [Adding your own](#adding-your-own): mix, then promote
- [What past games needed](#what-past-games-needed)

---

## Quick start

```java
// robot/src/robot/Robot.java
public final Lift lift;
public final ServoPart claw;
public final Roller intake;

public Robot(HardwareMap hardwareMap) {
    Hardware hw = new Hardware(hardwareMap);
    drive  = register(new Drive(Constants.createFollower(hardwareMap)));
    lift   = register(new Lift(hw.motor("lift")).limits(0, 3000).gravity(0.08));
    claw   = register(new ServoPart(hw.servo("claw")).moveTime(250).startAt(CLOSED));
    intake = register(new Roller(hw.motor("intake")).sensor(hw.distanceBelow("intakeSensor", 5)));
}
```

```java
// A TeleOp: gamepad 1 (driver) already drives; give gamepad 2 (operator) the mechanisms
controls.onPress(operator.a(), robot.claw.toggle(OPEN, CLOSED));
controls.onPress(operator.y(), robot.lift.goTo(HIGH));
controls.whileHeld(operator.rightBumper(), robot.intake.run());
controls.whileHeld(operator.leftBumper(), robot.lift.manual(operator.rightStickY()));
```

```java
// An Auto
sequence(
        robot.drive.driveTo(pose(60, 84, 135)),     // written for blue, flipped for red
        robot.lift.goTo(HIGH),
        robot.claw.setTo(OPEN),
        robot.lift.goTo(DOWN))
```

Full robots for three seasons, built only from these parts, are in [`examples/ftc/seasons/`](../examples/ftc/seasons/).

---

## What's in it

Everything is in the `sdk` package, in [`src/sdk/`](src/sdk/):

| Package | What | Uses the FTC SDK? |
|---------|------|-------------------|
| [`sdk.parts`](src/sdk/parts/) | `Lift`, `Arm`, `Roller`, `ServoPart`, `Flywheel` | No |
| [`sdk.teleop`](src/sdk/teleop/) | `Controller` (a gamepad), `DriveControls`, `MatchClock` | No |
| [`sdk.auto`](src/sdk/auto/) | `AutoMenu`, `Alliance`, `FieldSymmetry`, `PoseMemory` | No |
| [`sdk.vision`](src/sdk/vision/) | `Randomization` (LEFT/CENTER/RIGHT), `Vote` | No |
| [`sdk.tuning`](src/sdk/tuning/) | `Tunable`, `TuneReport`, `StepScore`, `SavedGains`, `Tunables` (finds parts to tune) | No |
| [`sdk.hardware`](src/sdk/hardware/) | `Motor`, `Servo` (what parts talk to), `PieceColor`, `SimMotor` (a pretend motor with physics) | No |
| [`sdk.ftc`](src/sdk/ftc/) | `Hardware` (gets motors/servos/sensors by name), `GamepadController`, `Camera`, `TagReader`, `PropFinder`, `Tuning` (the robot's tuning file) | **Yes, and only this one** |

**The one rule:** only `sdk.ftc` touches the FTC SDK. Parts talk to the tiny `Motor` and `Servo` interfaces, and `sdk.ftc` plugs real hardware into them. That is why the parts are unit-tested on a laptop (`./gradlew :sdk:testDebugUnitTest`) and why the [hive simulator](../examples/README.md#the-hive-game) runs them with pretend motors. The hive build compiles the sdk without `sdk/ftc/`, so an FTC import anywhere else fails that build straight away.

---

## Parts

Every part follows the same pattern:
- **Set it up** with chained settings where it's created in `Robot.java`.
- **`periodic()`** (called for you every loop) reads its sensors and drives its motor.
- **Behaviors** (methods returning `Node`) set a goal and wait for it. When one ends or is halted, the part is left safe: rollers stop, lifts hold.
- **Checks** (methods returning a value) for conditions: `robot.lift.atTarget()`, `robot.intake.hasPiece()`.

### Lift
Anything a motor moves along a line, in encoder ticks: lift, slide, extendo, hang winch.

```java
lift = register(new Lift(hw.motor("lift").reversed()).limits(0, 3100).gravity(0.1));
```

| Setting | Does |
|---------|------|
| `.limits(min, max)` | Never goes outside these. **Set them first**: every target is clamped, and the tuner only moves inside them. Default `0` to no maximum |
| `.gravity(power)` | Power that holds it still against its weight |
| `.friction(power)` | Extra push to get it unstuck when it's off target, so it doesn't stop just short |
| `.gains(kP, kD)` | How hard it pushes toward the target (default `0.005, 0`) |
| `.tolerance(ticks)` | How close counts as there (default 20) |
| `.maxPower(p)` | Caps power, e.g. while testing |
| `.manualSpeed(ticksPerSecond)` | Speed of `manual` at full stick |
| `.homeSwitch(sensor)` / `.homePower(p)` | How `home()` finds the bottom |

| Behavior / check | Does |
|------------------|------|
| `goTo(ticks)` | Moves there and succeeds; keeps holding afterwards |
| `manual(stick)` | Moves with a stick while running, holds when let go. Bind with `whileHeld` |
| `home()` | Drives down until the switch (or a stall), then calls that 0 |
| `off()` | Stops holding: the motor goes limp |
| `position()`, `target()`, `atTarget()` | |

The encoder starts at 0 when the robot powers on, so keep the lift down at power-on, or run `home()` first.

Don't guess `gains`, `gravity` and `friction`: [tune them](#tuning).

### Arm
Like `Lift`, but it swings around a pivot and works in **degrees**: 0° is horizontal, 90° straight up. Gravity pulls hardest when it's level, so it pushes back by `gravity × cos(angle)`.

```java
// 1425 ticks per turn of the arm; it rests at -40° when the robot powers on
arm = register(new Arm(hw.motor("arm"), 1425 / 360.0).zeroAt(-40).limits(-40, 110).gravity(0.12));
robot.arm.goTo(45)
```

### Roller
A motor or CR servo that just spins: intake, transfer, conveyor, carousel.

```java
intake = register(new Roller(hw.motor("intake")).power(0.8).sensor(hw.distanceBelow("intakeSensor", 5)));
carousel = register(new Roller(hw.crServo("carousel")));
```

| Behavior / check | Does |
|------------------|------|
| `run()` / `reverse()` | Spins until halted |
| `runFor(ms)` | Spins for a while, e.g. a duck carousel |
| `runUntil(condition)` | Spins until something is true |
| `collect()` | Spins until the sensor sees a piece (needs `.sensor(...)`, checked at init) |
| `eject(ms)` | Spins out for a while |
| `hasPiece()` | |

### ServoPart
A servo moved between a few positions: claw, wrist, gate, kicker, drone latch. Servos can't report when they've arrived, so `setTo` waits a `moveTime` you measure once.

```java
claw = register(new ServoPart(hw.servo("claw")).range(0.2, 0.8).moveTime(250).startAt(CLOSED));
wrist = register(new ServoPart(hw.servo("wristLeft")).pairedWith(hw.servo("wristRight")));
```

| | Does |
|-|------|
| `.range(min, max)` | Clamps every position, to keep it off hard stops |
| `.moveTime(ms)` | How long `setTo` waits |
| `.pairedWith(servo)` | A second servo on the same joint facing the other way: always gets `1 - position` |
| `.startAt(position)` | Moves there during init (servos may move in init; motors may not) |
| `setTo(position)` | Moves and waits `moveTime` |
| `toggle(a, b)` | Goes to `a`, or to `b` if already at `a` |

### Flywheel
A wheel held at a set speed to launch pieces. It holds the rpm itself, so a shot only dips the speed for a moment.

```java
shooter = register(new Flywheel(hw.motor("shooter").coast(), 28).maxRpm(6000).tolerance(120));

// Shoot one: spin, and feed as soon as it's up to speed
race(robot.shooter.spin(4200),
     sequence(waitUntil(robot.shooter::atSpeed), robot.feeder.setTo(PUSH), robot.feeder.setTo(REST)))
```

| | Does |
|-|------|
| `.maxRpm(rpm)`, `.friction(power)`, `.gain(kP)`, `.tolerance(rpm)` | Its control numbers: [tune them](#tuning) |
| `spin(rpm)` | Holds that speed until halted, then stops |
| `spinUp(rpm)` | Succeeds once at speed and keeps spinning until `stop()` |
| `stop()` | |
| `rpm()`, `targetRpm()`, `atSpeed()` | |

---

## Gamepads and driving

TeleOps and Autos get `driver` (gamepad 1) and `operator` (gamepad 2) as a [`Controller`](src/sdk/teleop/Controller.java). Every button is ready to pass to a binding, and the sticks behave the way you'd expect:

- **Up is positive.** A raw FTC gamepad reads *negative* when the stick is pushed up; `Controller` fixes that.
- Small stick wobbles read as 0 (`deadband`, default 0.05).
- Triggers work as buttons (`rightTrigger()`, pressed past halfway) or as amounts (`rightTriggerAmount()`).
- PlayStation names work too: `cross()`, `circle()`, `square()`, `triangle()`, `share()`, `options()`.

```java
controls.onPress(operator.a(), ...);                 // also operator.cross()
controls.whileHeld(operator.rightTrigger(), ...);
robot.lift.manual(operator.rightStickY())
driver.rumble(500);
```

[`DriveControls`](src/sdk/teleop/DriveControls.java) turns the driver's sticks into drive commands. `TeleOpBase` uses one with these defaults: left stick moves, right stick turns, field-centric, **right bumper held = 40% speed**, **back (share) = reset heading**. To change it, override `driveControls()` in your TeleOp:

```java
@Override
protected DriveControls driveControls() {
    return new DriveControls(driver).robotCentric().slowWhileHeld(driver.leftBumper(), 0.3).turnScale(0.7);
}
```

[`MatchClock`](src/sdk/teleop/MatchClock.java) counts down the 2:00 TeleOp. In a TeleOp, `timeLeft()` reads it, and `onEndgame()` runs once at `endgameSeconds()` left (default 30; it buzzes both gamepads). A binding can watch the clock like a button:

```java
controls.onPress(() -> timeLeft() < 10, robot.drive.driveTo(PARK));   // park by itself at 0:10
```

---

## Auto helpers

### The init menu
Every Auto shows an [`AutoMenu`](src/sdk/auto/AutoMenu.java) on the Driver Station during init. Gamepad 1's dpad picks a line (▲▼) and changes it (◀▶). It always has **Alliance** and **Start delay**. Add your own lines in `initialize()` and read them in `routine()`, which runs at start, once the choices are final:

```java
enum Start { NEAR, FAR }
private AutoMenu.Choice<Start> start;

@Override protected void initialize() { start = menu().choice("Start", Start.values()); }

@Override protected Node routine() {
    robot.drive.setStartingPose(start.get() == Start.NEAR ? pose(56, 8, 90) : pose(20, 122, 144));
    ...
}
```

### One Auto for both alliances
Write every pose for **blue** with `pose(x, y, degrees)` (inches, Pedro coordinates). On red it is flipped to the other side by the season's [`FieldSymmetry`](src/sdk/auto/FieldSymmetry.java), set once in `Robot.SYMMETRY`:

| Symmetry | Flip | Seasons |
|----------|------|---------|
| `MIRROR` | across the middle line (x → 144 − x) | DECODE, CENTERSTAGE |
| `ROTATE` | half a turn about the center (x and y flip) | INTO THE DEEP |

Check the season's field drawing: symmetry is a property of each year's field.

### From the Auto into the TeleOp
When any OpMode ends, its last pose is saved ([`PoseMemory`](src/sdk/auto/PoseMemory.java)), and the TeleOp starts from it. Field-centric driving is then right straight away; if it isn't (say, the robot was moved by hand), the driver presses back to reset the heading.

---

## Vision

Most seasons hide a three-way answer for the Auto: FREIGHT FRENZY's barcode, POWERPLAY's signal sleeve, CENTERSTAGE's team prop, DECODE's obelisk motif. A [`Camera`](src/sdk/ftc/Camera.java) reads it during init and keeps voting over recent frames ([`Vote`](src/sdk/vision/Vote.java)), so one bad frame doesn't change the answer. The answer is a [`Randomization`](src/sdk/vision/Randomization.java): `LEFT`, `CENTER`, `RIGHT`, or `UNKNOWN` until enough frames agree.

```java
// Robot.java: DECODE's obelisk tags
camera = register(new Camera(hw, "Webcam 1",
        new TagReader().map(21, LEFT).map(22, CENTER).map(23, RIGHT)));

// CENTERSTAGE: the biggest patch of our alliance's color, by which third of the picture it's in
camera = register(new Camera(hw, "Webcam 1", new PropFinder(ColorRange.RED, ColorRange.BLUE, () -> alliance)));

// The Auto's routine(): always have a fallback
Randomization answer = robot.camera.result().orElse(CENTER);
```

`TagReader.tag(id)` also gives a tag's range and bearing, for lining up on a goal or backdrop tag.

---

## Tuning

`Lift`, `Arm` and `Flywheel` hold positions and speeds with a few control numbers (gains). The **Tune mechanisms** OpMode (in the Driver Station's "Tuning" group) finds them for you. It lists every lift, arm and flywheel on `Robot` by itself, including ones inside season subsystems (`scorer.lift`). Pick one with gamepad 1 (dpad, A) and press start (▶).

### Auto-tune: press Y
The mechanism runs experiments on itself, **only inside its `.limits(...)`**, and reports what it found on the Driver Station:

```
1/3 Finding gravity and friction...
   gravity 0.121, friction 0.0756 (moves up at 0.197 power, down at 0.0456)
2/3 Swinging it around the middle...
   swings ±15.9 every 0.24 s: kP 0.0035, kD 0.000105
3/3 Test moves...
   round 1: up overshoot 0%, settles in 640 ms; down overshoot 1%, settles in 540 ms
Done. Paste this where it's created in Robot.java:
   .gains(0.0035, 0.000105).gravity(0.121).friction(0.0756)
```

| Mechanism | What it does |
|-----------|--------------|
| `Lift`, `Arm` | 1. Raises the power slowly until it starts moving up, lifts it clear of the bottom, then lowers the power until it starts down: halfway between is **gravity**, half the gap is **friction**. 2. Pushes it up and down around the middle of its range by a fixed amount (a relay test) and measures how far and how fast it swings, giving a first **kP** and **kD**. 3. Moves between 30% and 70% of its range, scores each move (overshoot, settling time), and adjusts: overshoot → softer kP and more kD, too slow → more kP, stops short → more friction. Up to six rounds, keeping the best |
| `Flywheel` | 1. Runs at 35% and 75% power and measures the rpm: the line through them gives **maxRpm** and **friction**. 2. Spins up from 40% to 70% of top speed with a small kP, then double, and so on, until spin-up overshoots or the speed wobbles. Uses **half the largest kP that passed**, for a safety margin, and sets the **tolerance** from the wobble |

It stops with a clear message, and puts the old gains back, if something's wrong: no limits set, the motor runs backwards ("add `.reversed()`"), it doesn't move at full power, or the speed doesn't change with power (encoder unplugged). Press **B** to stop it at any time.

### By hand
▲▼ picks a gain, ◀▶ changes it by 10%, the bumpers halve or double it. **X** runs a scored test move (30% → 70% → 30% of the range, or a spin-up for a flywheel) and reports overshoot and settling time. Repeat until you like it. [Panels](http://192.168.43.1:8001) graphs where it is against where it's going while you do.

### Keeping the numbers
- **START** saves the part's gains on the robot (`/sdcard/FIRST/tuning.properties`). `Robot.java` calls `Tuning.applySaved(this)` at the end of its constructor, so **every OpMode uses saved gains from then on**, in place of the numbers in `Robot.java`. Each OpMode lists the parts using saved tuning during init.
- The screen always shows the gains as **code to paste** into `Robot.java` (`.gains(...).gravity(...)...`). Paste it, then press **BACK** to forget the saved copy, so the code is the one place the numbers live (and they survive a new robot or a reinstall).
- To see the file from a laptop: `adb pull /sdcard/FIRST/tuning.properties`.

### Try it without a robot
`./gradlew :examples:tune` (or **Tuning demo** in VS Code and Android Studio) runs the same tuner on a simulated lift, arm or flywheel ([`SimMotor`](src/sdk/hardware/SimMotor.java): inertia, gravity, friction, hard stops) and shows it in the [behavior tree visualizer](../examples/README.md#running-the-behavior-tree-visualizer), with a graph of each move. The [tests](test/sdk/tuning/AutoTuneTest.java) tune four such mechanisms and check the result.

### Making your own part tunable
Implement [`Tunable`](src/sdk/tuning/Tunable.java): list its gains, and hand out an `autoTune` and a `stepTest` node. [`StepScore`](src/sdk/tuning/StepScore.java) scores a move, and [`TuneReport`](src/sdk/tuning/TuneReport.java) collects what to show. The Tune OpMode and saving then work with it as they do with the built-in parts.

---

## Guardrails

| Mistake | What the sdk does |
|---------|-------------------|
| Misspelled hardware name | Fails at init: `No motor named "lfit"... Configured motors: fl, fr, bl, br, lift` |
| Lift or arm sent past its end | Every target is clamped to `.limits(...)` |
| A behavior halted mid-move (timeout, released button) | Lifts and arms hold where they are; rollers and flywheels stop |
| Two behaviors driving the same part | They set a target instead of fighting over the motor: the last one wins |
| Stick Y backwards | `Controller` makes up positive |
| Motors moving during init | Parts only read sensors in init (`initPeriodic`); servos may `startAt` a position |
| `collect()` on a roller without a sensor | Fails at init, saying what to add |
| Slow loops | Bulk reads are on, and repeated motor powers are skipped; the loop time is on telemetry |
| Lost position between Auto and TeleOp | The pose carries over; encoders are never reset behind your back |
| Guessed gains | The Tune OpMode measures them, only moving inside the part's limits, and refuses without limits |
| Forgetting which gains a robot runs | Saved tuning is listed on the Driver Station in every OpMode's init |

---

## Adding your own

You never have to edit the sdk to add something. A part is just a subsystem that hands out nodes, the same thing your own code does, so they mix freely in one tree. Pick the first step that fits:

**1. Use a part as-is.** One line in `Robot.java`. Most mechanisms are a `Lift`, `Arm`, `Roller`, `ServoPart` or `Flywheel`.

**2. Wrap parts in a season subsystem** (most common). Put game-specific logic in your own class in `robot/subsystems/`, built from parts, with methods named in the game's words:

```java
public class Scorer implements Subsystem {
    public final Lift lift;
    public final ServoPart claw;

    public Scorer(Hardware hw) {
        lift = new Lift(hw.motor("lift")).limits(0, 3100).gravity(0.1);
        claw = new ServoPart(hw.servo("claw")).moveTime(200);
    }

    public Node scoreBasket() {
        return sequence(lift.goTo(HIGH_BASKET), claw.setTo(OPEN), lift.goTo(DOWN));
    }

    @Override public void initPeriodic() { lift.initPeriodic(); }
    @Override public void periodic() { lift.periodic(); }   // Pass the loop on to the parts inside
}
```

The [season examples](../examples/ftc/seasons/) all do this (`Launcher`, `SampleIntake`, `Scorer`, `PixelScorer`), and so does the hive game's `Intake` and `Shooter`.

**3. Write it raw** when no part fits: a subsystem with its own [`Action`](../framework/README.md#writing-your-own-action), like [`examples/ftc/Intake.java`](../examples/ftc/Intake.java). Get its hardware with `hw.motor(...)`, `hw.servo(...)` or `hw.device(SomeFtcClass.class, "name")` so a wrong name still fails clearly. It goes in the same trees as the parts.

**4. Promote it** into the sdk when it proved useful and isn't tied to one game (it would have helped last season too):
- [ ] Move it to `sdk/src/sdk/parts/` (or the package that fits).
- [ ] Replace FTC types with `sdk.hardware.Motor` / `Servo` / a `BooleanSupplier`. Anything that must use the FTC SDK goes in `sdk.ftc` instead.
- [ ] Take a `Clock` in a second constructor if it waits, as the other parts do, so tests can fake time.
- [ ] Follow the part pattern: chained settings, `periodic()` does the hardware, behaviors set goals and leave things safe on `end`.
- [ ] Add a test in `sdk/test/` with the fakes there (`FakeMotor`, `FakeClock`, `Loop`, or `SimMotor` for physics).
- [ ] If it holds a position or speed with gains, make it [`Tunable`](#making-your-own-part-tunable).
- [ ] Add it to this README and to the season table below.

Its behavior names stay the same, so OpModes using it don't change: only its line in `Robot.java` does.

Parts don't hand out their raw `DcMotor`: one mechanism has one owner. To do something a part can't, wrap it (2) or write your own (3).

---

## What past games needed

| Season | Needed | sdk parts |
|--------|--------|-----------|
| 2025-26 DECODE | roller intake, flywheel launcher, feeder servo, purple/green sensing, obelisk motif tags, aiming at the goal tag | `Roller`, `Flywheel`, `ServoPart`, `hw.color` (`PieceColor`), `TagReader` |
| 2024-25 INTO THE DEEP | extendo, lift to baskets and chambers, claw and wrist, throwing back the other alliance's samples, hang | `Lift` × 2, `ServoPart`, `hw.color`, `FieldSymmetry.ROTATE` |
| 2023-24 CENTERSTAGE | lift to the backdrop, pixel claws, drone latch, team prop left/center/right, hang winch | `Lift`, `ServoPart` × 3, `PropFinder`, `Lift` |
| 2022-23 POWERPLAY | lift with four junction heights, claw, signal sleeve parking zone | `Lift` presets, `ServoPart`, `TagReader` (an AprilTag sleeve) |
| 2021-22 FREIGHT FRENZY | duck carousel, lift to three hub levels, intake, barcode | `Roller.runFor`, `Lift`, `Roller`, `PropFinder` |
| 2020-21 ULTIMATE GOAL | flywheel shooter, ring feeder, wobble goal arm, ring stack 0/1/4 | `Flywheel`, `ServoPart`, `Arm`, `PropFinder` (stack height as LEFT/CENTER/RIGHT) |

Every year: pick alliance, start and delay during init (`AutoMenu`), one Auto for both sides (`pose(...)`), carry the pose into TeleOp (`PoseMemory`), end-game timing (`timeLeft()`, `onEndgame()`), park (`drive.driveTo`).
