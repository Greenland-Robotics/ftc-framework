# pedro

Everything [Pedro Pathing](https://pedropathing.com) related: drivetrain and localizer configuration, the tuning OpModes, and the `Drive` subsystem that follows paths.

All classes are in the `pedro` package, in [`src/pedro/`](src/pedro/).

| File | What it is | Edit it? |
|------|------------|----------|
| [`Constants.java`](src/pedro/Constants.java) | Motor names/directions, odometry setup and tuned values | **Yes**: first thing to configure for a new robot, then fill in tuned values |
| [`Drive.java`](src/pedro/Drive.java) | The drivetrain subsystem: follows paths, drives from the gamepad, knows the robot's pose | No |
| [`FollowPath.java`](src/pedro/FollowPath.java) | The Action behind `drive.follow(...)` and `drive.driveTo(...)` | No |
| [`Tuning.java`](src/pedro/Tuning.java) | The **Tuning** OpMode (in the Driver Station's "Pedro Pathing" group) | No |

## Tuning a new robot
1. Set motor names, directions and odometry settings in `Constants.java` to match your Driver Station hardware configuration.
2. Deploy, then run the **Tuning** OpMode and work through the tuners in order, following the tuning guide on the [Pedro Pathing website](https://pedropathing.com).
3. Copy each tuned value into `Constants.java`.

Build paths with the [Pedro Pathing Visualizer](https://visualizer.pedropathing.com) and paste the generated code into your Auto's `Paths` class.

---

## Constants

**Purpose:**
Central location for all tunable robot constants — motor names, directions, odometry pod offsets, and path constraints. This is the **first file you configure** when setting up for a new robot.

### Key Fields

#### Drive Constants (`MecanumConstants driveConstants`)
- Motor names: must match your hardware configuration file in the Driver Station app
  - `rightFrontMotorName`, `rightRearMotorName`, `leftFrontMotorName`, `leftRearMotorName`
- Motor directions: `FORWARD` or `REVERSE` per motor
- `maxPower`: Maximum drive power (0–1)

#### Localizer Constants (`PinpointConstants localizerConstants`)
- `hardwareMapName`: The name of the Pinpoint device in your hardware config (default: `"pinpoint"`)
- `forwardPodY`, `strafePodX`: Pod offsets in the configured `distanceUnit`
- `encoderResolution`: Pod type, e.g. `goBILDA_4_BAR_POD`
- `forwardEncoderDirection`, `strafeEncoderDirection`: `FORWARD` or `REVERSED`

#### Path Constraints (`PathConstraints pathConstraints`)
Limits for velocity and acceleration during path following. Tune these to prevent wheel slip.

#### `public static Follower createFollower(HardwareMap hardwareMap)`
Builds the configured `Follower`. You don't call this directly: [`Robot.java`](../robot/src/robot/Robot.java) uses it to create `robot.drive`.

#### Adding your own constants
Add static fields for servo positions, motor targets, PID coefficients, etc.:
```java
// eg.
public static double CLAW_CLOSED = 0.2;
public static double CLAW_OPEN = 0.8;
public static int ARM_UP = 1200;
```

---

## Drive

[`Drive`](src/pedro/Drive.java) is a [subsystem](../framework/README.md#subsystems) that owns Pedro's `Follower`, and is available as `robot.drive`. It updates the follower once per loop, so nothing else should call `follower.update()`.

| Method | Returns | Does |
|--------|---------|------|
| `follow(path)` | behavior | Follows a prebuilt `PathChain` (e.g. from your Auto's `Paths` class); succeeds on arrival |
| `driveTo(pose)` | behavior | Drives in a straight line to `pose` from **wherever the robot is when it starts** |
| `drive(forward, strafe, turn)` | | Driver control; ignored while a path is running. `TeleOpBase` calls it for you |
| `pose()` | `Pose` | Where the robot is |
| `setStartingPose(pose)` | | Tell the localizer where the robot starts (during init) |
| `pathBuilder()` | `PathBuilder` | Build paths for your `Paths` class |

```java
sequence(
        robot.drive.follow(paths.toGoal),
        robot.drive.driveTo(new Pose(72, 72, Math.toRadians(90))).withTimeout(3000)
)
```

If a path behavior is halted part-way (a timeout, a released button, a selector moving on), the robot stops following the path.
