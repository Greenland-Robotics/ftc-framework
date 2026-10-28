package hive;

import static util.MathUtil.wrapAngle;

import static behavior.Behaviors.condition;
import static behavior.Behaviors.parallel;
import static behavior.Behaviors.race;
import static behavior.Behaviors.reactiveSelector;
import static behavior.Behaviors.reactiveSequence;
import static behavior.Behaviors.sequence;
import static behavior.Behaviors.waitUntil;

import behavior.Node;
import behavior.decorator.Timeout;

/**
 * The robot's behavior tree for the hive game. The Auto runs all of it by itself; the TeleOp
 * hands the wheels, intake and shooter to the driver, but its buttons start the same building
 * blocks the Auto uses ({@link #shootOne}, {@link #lineUpShot}).
 *
 * Each alliance scores by making its own hive (a seesaw) pivot as many times as it can, so the
 * target is always the cup at the end of our hive that is up.
 *
 * <pre>
 * match
 * ├── Autonomous period (30 s)   fill up → (line up → shoot) until empty, over and over
 * └── TeleOp period (2:00)       three lanes at once, one per mechanism:
 *     ├── Wheels    aim button held? → line up a shot     otherwise → drive with the sticks
 *     ├── Intake    intake button held? → run the rollers  otherwise → off
 *     └── Shooter   wait for the shoot button → shoot one ball
 * </pre>
 *
 * Nothing here knows it is a simulation: give it the real robot's subsystems and a gamepad
 * {@link Driver}, and the same tree runs on the robot.
 */
public final class HiveRoutines {
    private HiveRoutines() {}

    /** The whole match: the Auto (if {@code withAuto}), then the TeleOp. */
    public static Node match(Robot robot, Driver driver, Field field, boolean withAuto) {
        double teleopEnd = field.matchSeconds();
        Node teleopPeriod = race(
                teleop(robot, driver, field),
                waitUntil(() -> field.time() >= teleopEnd).named("2:00 on the clock")
        ).named("TeleOp period");
        if (!withAuto) return teleopPeriod;

        return sequence(
                race(
                        autonomous(robot, field),
                        waitUntil(() -> field.time() >= Field.AUTO_SECONDS).named("30 s on the clock")
                ).named("Autonomous period"),
                teleopPeriod
        ).named("Match");
    }

    // ---- Building blocks shared by the Auto and the TeleOp ----

    /** Spins the flywheel up, then feeds one ball into it. Fails if the robot holds no balls. */
    public static Node shootOne(Robot robot) {
        return sequence(
                condition(robot.intake::hasBall).named("Holding a ball?"),
                race(
                        robot.shooter.spin(),
                        sequence(
                                waitUntil(robot.shooter::atSpeed).named("Flywheel up to speed?"),
                                robot.shooter.feed()
                        )
                ).named("Spin up and feed")
        ).named("Shoot one ball");
    }

    /** Drives to the nearest spot in range of the up end of our hive, facing it. */
    public static Node lineUpShot(Robot robot, Field field) {
        return robot.drive.driveTo(() -> shootingSpot(robot, field)).named("Line up a shot at our hive's up end");
    }

    /** Drives over the nearest ball with the intake running, until it is inside. */
    public static Node collectBall(Robot robot, Field field) {
        Node chase = race(
                robot.intake.collect(),
                robot.drive.driveTo(() -> towardNearestBall(robot, field)).named("Drive over the nearest ball")
        );
        // Give up on a ball that keeps getting away; the clock is the game's, so pausing pauses it
        return reactiveSequence(
                condition(() -> field.ballsLeft() > 0).named("Any balls on the field?"),
                new Timeout(chase, 5000, field.clock())
        ).named("Collect a ball");
    }

    // ---- Autonomous: the whole tree, no driver ----

    public static Node autonomous(Robot robot, Field field) {
        return sequence(
                sequence(
                        condition(() -> !robot.intake.isFull()).named("Room for another ball?"),
                        collectBall(robot, field)
                ).repeatForever().alwaysSucceed().named("Fill up"),
                condition(robot.intake::hasBall).named("Got any balls?"),
                // Line up before every shot: when the hive pivots, the other end becomes the target
                sequence(lineUpShot(robot, field), shootOne(robot))
                        .repeatForever().alwaysSucceed().named("Shoot until empty")
        ).repeatForever().named("Autonomous: collect, line up, score, repeat");
    }

    // ---- TeleOp: the driver picks which building blocks run ----

    public static Node teleop(Robot robot, Driver driver, Field field) {
        return parallel(
                reactiveSelector(
                        reactiveSequence(
                                condition(driver::aim).named("Aim button held?"),
                                lineUpShot(robot, field)
                        ),
                        robot.drive.driveWith(driver).named("Drive with the sticks")
                ).repeatForever().named("Wheels"),

                reactiveSelector(
                        reactiveSequence(
                                condition(driver::intake).named("Intake button held?"),
                                robot.intake.run().named("Run the rollers")
                        ),
                        waitUntil(() -> false).named("Intake off")
                ).named("Intake"),

                sequence(
                        waitUntil(driver::shoot).named("Wait for the shoot button"),
                        shootOne(robot).alwaysSucceed()
                ).repeatForever().named("Shooter")
        ).named("TeleOp: the driver's controls, all at once");
    }

    // ---- Where to go ----

    /**
     * Where to shoot from: {@link Field#SHOT_RANGE} from the cup at the up end of our hive
     * (measured from the robot's front), facing it. Of all such spots, the closest one to the
     * robot that is on the field and clear of the hives. Null if there is none.
     */
    static Pose shootingSpot(Robot robot, Field field) {
        Hive hive = field.hive(robot.alliance);
        Pose here = robot.drive.pose();
        int end = hive.endToShoot(here.x, here.y);
        double cupX = hive.cupX(end);
        double cupY = hive.cupY(end);
        double distance = Field.SHOT_RANGE + Body.SIZE / 2;
        double toRobot = Math.atan2(here.y - cupY, here.x - cupX);
        // Start from the direction of the robot and swing both ways around the cup
        for (int step = 0; step <= 36; step++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                double angle = toRobot + sign * step * Math.toRadians(5);
                double x = cupX + Math.cos(angle) * distance;
                double y = cupY + Math.sin(angle) * distance;
                if (field.isClear(x, y, 12)) return new Pose(x, y, wrapAngle(angle + Math.PI));
            }
        }
        return null;
    }

    /** A pose just past the nearest ball, facing it, so the intake sweeps over it. */
    static Pose towardNearestBall(Robot robot, Field field) {
        Ball ball = field.nearestBall(robot.body);
        if (ball == null) return null;
        Pose here = robot.drive.pose();
        double angle = Math.atan2(ball.y - here.y, ball.x - here.x);
        double reach = Body.SIZE / 2;   // The robot's center this far from the ball puts the ball in the rollers
        return new Pose(ball.x - Math.cos(angle) * reach, ball.y - Math.sin(angle) * reach, angle);
    }
}
