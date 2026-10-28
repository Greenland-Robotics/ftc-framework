package hive;

import static behavior.Behaviors.condition;
import static behavior.Behaviors.instant;
import static behavior.Behaviors.parallel;
import static behavior.Behaviors.race;
import static behavior.Behaviors.reactiveSelector;
import static behavior.Behaviors.reactiveSequence;
import static behavior.Behaviors.selector;
import static behavior.Behaviors.sequence;
import static behavior.Behaviors.succeed;
import static behavior.Behaviors.waitUntil;
import static util.MathUtil.wrapAngle;

import java.util.IdentityHashMap;
import java.util.Map;

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
        ShotChoice choice = new ShotChoice(robot, field);
        return robot.drive.driveTo(choice::spot).named("Line up a shot at our hive's up end");
    }

    /**
     * Drives over the nearest ball with the intake running, until it is inside. A ball it can't
     * get within 5 s (stuck behind a hive, jammed in a corner) is skipped for a while, so the next
     * try goes for a different one.
     */
    public static Node collectBall(Robot robot, Field field) {
        BallChoice choice = new BallChoice(robot, field);
        Node chase = race(
                robot.intake.collect(),
                robot.drive.driveTo(choice::approach).named("Drive over the nearest ball")
        );
        return reactiveSequence(
                condition(choice::anyLeft).named("Any balls we haven't given up on?"),
                // The clock is the game's, so pausing the game pauses the timeout too
                selector(
                        new Timeout(chase, 5000, field.clock()),
                        instant(choice::giveUp).named("Give up on that ball for 15 s")
                )
        ).named("Collect a ball");
    }

    // ---- Autonomous: the whole tree, no driver ----

    public static Node autonomous(Robot robot, Field field) {
        return sequence(
                sequence(
                        condition(() -> !robot.intake.isFull()).named("Room for another ball?"),
                        collectBall(robot, field)
                ).repeatForever().alwaysSucceed().named("Fill up"),
                // Every branch here succeeds, so the Auto never ends: with nothing to shoot, it goes looking again
                selector(
                        sequence(
                                condition(robot.intake::hasBall).named("Got any balls?"),
                                // Line up before every shot: when the hive pivots, the other end becomes the target
                                // Can't get there in 4 s (another robot in the way)? Go collect, then try again
                                sequence(
                                        condition(robot.intake::hasBall).named("Any left?"),
                                        new Timeout(lineUpShot(robot, field), 4000, field.clock()),
                                        shootOne(robot)
                                ).repeatForever().alwaysSucceed().named("Shoot until empty")
                        ),
                        succeed().named("Nothing to shoot: look again")
                )
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

    /**
     * The spot a robot is lining up a shot from. It stays put while the robot drives there (worked
     * out again from scratch each time it starts lining up, or if the target end changes), so the
     * robot isn't chasing a spot that moves with it.
     */
    private static final class ShotChoice {
        private final Robot robot;
        private final Field field;
        private Pose spot;
        private int end = -1;
        private double lastAsked = -1;

        ShotChoice(Robot robot, Field field) {
            this.robot = robot;
            this.field = field;
        }

        Pose spot() {
            Hive hive = field.hive(robot.alliance);
            Pose here = robot.drive.pose();
            int target = hive.endToShoot(here.x, here.y);
            boolean startingOver = field.time() - lastAsked > 0.1;   // Not asked last loop: a new line-up
            lastAsked = field.time();
            if (spot == null || startingOver || target != end) {
                spot = shootingSpot(robot, field);
                end = target;
            }
            return spot;
        }
    }

    /** Which ball a robot is going for, and the ones it gave up on and skips for now. */
    private static final class BallChoice {
        private static final double SKIP_SECONDS = 15;

        private final Robot robot;
        private final Field field;
        private final Map<Ball, Double> skipUntil = new IdentityHashMap<>();
        private Ball current;

        BallChoice(Robot robot, Field field) {
            this.robot = robot;
            this.field = field;
        }

        boolean anyLeft() {
            return field.nearestBall(robot.body, this::allowed) != null;
        }

        void giveUp() {
            if (current != null) skipUntil.put(current, field.time() + SKIP_SECONDS);
        }

        private boolean allowed(Ball ball) {
            Double until = skipUntil.get(ball);
            return until == null || field.time() >= until;
        }

        /** Where to drive to pick up the nearest ball we haven't given up on, or null if there is none. */
        Pose approach() {
            current = field.nearestBall(robot.body, this::allowed);
            return current == null ? null : pickUpPose(robot.drive.pose(), current, field);
        }
    }

    /**
     * A pose that puts the ball in the rollers. Straight at it from where the robot is, unless the
     * ball is against a wall or a hive: then square on, facing the wall, so the robot's corners
     * don't keep it out of reach.
     */
    static Pose pickUpPose(Pose here, Ball ball, Field field) {
        double reach = Body.SIZE / 2;   // The robot's center this far from the ball puts the ball in the rollers
        double direct = Math.atan2(ball.y - here.y, ball.x - here.x);
        Pose best = null;
        double bestCost = Double.MAX_VALUE;
        double[] headings = {direct, 0, Math.PI / 2, Math.PI, -Math.PI / 2};
        for (int i = 0; i < headings.length; i++) {
            double heading = headings[i];
            double x = ball.x - Math.cos(heading) * reach;
            double y = ball.y - Math.sin(heading) * reach;
            if (!field.canStand(x, y, heading)) continue;
            // Prefer driving straight at it; otherwise the square-on approach closest to the robot
            double cost = (i == 0 ? 0 : 30) + here.distanceTo(x, y);
            if (cost < bestCost) {
                bestCost = cost;
                best = new Pose(x, y, wrapAngle(heading));
            }
        }
        return best != null ? best : new Pose(ball.x - Math.cos(direct) * reach, ball.y - Math.sin(direct) * reach, direct);
    }
}
