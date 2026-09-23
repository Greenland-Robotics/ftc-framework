# pedro

Everything [Pedro Pathing](https://pedropathing.com) related: drivetrain and localizer configuration, the tuning OpModes, and the command that follows paths.

All classes are in the `pedro` package, in [`src/pedro/`](src/pedro/).

| File | What it is | Edit it? |
|------|------------|----------|
| [`Constants.java`](src/pedro/Constants.java) | Motor names/directions, odometry setup and tuned values | **Yes**: first thing to configure for a new robot, then fill in tuned values |
| [`Tuning.java`](src/pedro/Tuning.java) | The **Tuning** OpMode (in the Driver Station's "Pedro Pathing" group) | No |
| [`FollowPath.java`](src/pedro/FollowPath.java) | A `Command` that drives a path | No |
| [`Pedro.java`](src/pedro/Pedro.java) | Hands the running OpMode's `Follower` to commands like `FollowPath` | No |

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
Builds the configured `Follower`. You don't call this directly: `OpModeBase` calls it through `Pedro.createFollower()`.

#### Adding your own constants
Add static fields for servo positions, motor targets, PID coefficients, etc.:
```java
// eg.
public static double CLAW_CLOSED = 0.2;
public static double CLAW_OPEN = 0.8;
public static int ARM_UP = 1200;
```

---

## FollowPath

**Purpose:**
A `Command` that drives the robot along a path and finishes when the robot arrives.

```java
new FollowPath(paths.toGoal)                         // a PathChain, e.g. from your Paths class
new FollowPath(new Pose(0, 0, 0), new Pose(24, 0, 0)) // a straight line between two poses
new FollowPath(start, control, end)                  // a Bézier curve through 3+ poses
```

Create `FollowPath` commands only after the OpMode has started building (inside `buildCommands()`/`initialize()`), because they need the OpMode's `Follower`.
