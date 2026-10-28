package hive;

import static util.MathUtil.clamp;
import static util.MathUtil.distance;
import static util.MathUtil.toFieldFrame;
import static util.MathUtil.wrapAngle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import behavior.runtime.Clock;

/**
 * The simulated game: a 144 × 144 inch field with wiffle balls and wiffle softballs, a
 * {@link Hive} (a seesaw with a cup at each end) for each alliance, and the robots. Every time an
 * alliance's hive pivots, that alliance scores.
 *
 * Time only moves when {@link #step()} is called, {@value #DT} s per call: once per tick of the
 * tree, like one loop of an OpMode. Slowing the ticks down slows the whole game down.
 */
public final class Field {
    public static final double SIZE = 144;
    static final double DT = 0.02;

    /** How far each hive is from the side wall. */
    static final double HIVE_INSET = 44;

    /** How far in front of the robot a ball lands when the flywheel is at full speed, in inches. */
    static final double SHOT_RANGE = 30;

    static final double AUTO_SECONDS = 30;
    static final double TELEOP_SECONDS = 120;

    private static final double MAX_SPEED = 50;                     // inches per second
    private static final double MAX_TURN = Math.toRadians(200);     // radians per second
    private static final double ROBOT_RADIUS = 11;                  // For bumping into things
    private static final int WIFFLE_BALLS = 36;
    private static final int SOFTBALLS = 12;

    final boolean withAuto;
    final List<Ball> loose = new ArrayList<>();
    final List<Ball> flying = new ArrayList<>();
    final List<Body> bodies = new ArrayList<>();
    final Hive[] hives = {
            new Hive(Alliance.RED, HIVE_INSET, SIZE / 2),
            new Hive(Alliance.BLUE, SIZE - HIVE_INSET, SIZE / 2)
    };
    private long steps;
    private final Random random = new Random(2026);

    public Field(boolean withAuto) {
        this.withAuto = withAuto;
        // The same layout on both halves, mirrored, so neither alliance has an advantage
        int placed = 0;
        while (placed < (WIFFLE_BALLS + SOFTBALLS) / 2) {
            double x = 5 + random.nextDouble() * 64;
            double y = 5 + random.nextDouble() * 134;
            Hive red = hives[0];
            if (Math.abs(x - red.x) < Hive.WIDTH / 2 + 6 && Math.abs(y - red.y) < Hive.LENGTH / 2 + 6) continue;
            if (distance(x, y, 16, 20) < 16) continue;                  // Not on the starting spots
            if (distance(x, y, 16, 124) < 16) continue;
            boolean tooClose = false;
            for (Ball other : loose) tooClose |= distance(other.x, other.y, x, y) < 6;
            if (tooClose) continue;
            boolean softball = placed < SOFTBALLS / 2;
            loose.add(new Ball(softball, x, y));
            loose.add(new Ball(softball, SIZE - x, SIZE - y));
            placed++;
        }
    }

    /** Puts a robot on the field, holding one preloaded ball. */
    public Robot addRobot(Alliance alliance, double x, double y, double heading, double speedScale) {
        Body body = new Body(alliance, x, y, heading, speedScale);
        body.held.add(new Ball(false, x, y));
        bodies.add(body);
        return new Robot(body, clock());
    }

    /** Seconds since the match started. */
    public double time() {
        return steps * DT;
    }

    public double matchSeconds() {
        return (withAuto ? AUTO_SECONDS : 0) + TELEOP_SECONDS;
    }

    public boolean inAuto() {
        return withAuto && time() < AUTO_SECONDS;
    }

    /** The game's own clock, for anything in the tree that waits. */
    public Clock clock() {
        return () -> Math.round(steps * DT * 1000);
    }

    public Hive hive(Alliance alliance) {
        return hives[alliance.ordinal()];
    }

    public int score(Alliance alliance) {
        return hive(alliance).pivots * Hive.PIVOT_POINTS;
    }

    /** Whether a robot centered at (x, y) is at least {@code margin} from the walls and the hives. */
    boolean isClear(double x, double y, double margin) {
        if (x < margin || y < margin || x > SIZE - margin || y > SIZE - margin) return false;
        for (Hive hive : hives) {
            if (hive.pushOut(x, y, margin) != null) return false;
        }
        return true;
    }

    public int ballsLeft() {
        return loose.size();
    }

    /**
     * The ball a robot should go for next: the closest one, avoiding balls right next to another
     * robot or behind a hive (it drives straight there). Null if there are none.
     */
    Ball nearestBall(Body robot) {
        Ball best = null;
        double bestCost = Double.MAX_VALUE;
        for (Ball ball : loose) {
            double cost = distance(robot.x, robot.y, ball.x, ball.y);
            for (Body other : bodies) {
                if (other != robot && distance(other.x, other.y, ball.x, ball.y) < 20) cost += 60;
            }
            if (hiveInTheWay(robot.x, robot.y, ball.x, ball.y)) cost += 80;
            if (cost < bestCost) {
                bestCost = cost;
                best = ball;
            }
        }
        return best;
    }

    /** Whether a robot driving straight from one point to another would run into a hive. */
    private boolean hiveInTheWay(double fromX, double fromY, double toX, double toY) {
        double length = distance(fromX, fromY, toX, toY);
        int samples = (int) Math.ceil(length / 4);
        for (int i = 1; i < samples; i++) {
            double x = fromX + (toX - fromX) * i / samples;
            double y = fromY + (toY - fromY) * i / samples;
            for (Hive hive : hives) {
                if (hive.pushOut(x, y, ROBOT_RADIUS - 2) != null) return true;
            }
        }
        return false;
    }

    // ---- Simulation ----

    /** Moves the game forward one loop. */
    public void step() {
        steps++;
        for (Body body : bodies) moveRobot(body);
        for (int i = 0; i < bodies.size(); i++) {
            for (int j = i + 1; j < bodies.size(); j++) separate(bodies.get(i), bodies.get(j));
        }
        for (Body body : bodies) {
            runIntake(body);
            runShooter(body);
        }
        moveBalls();
        flyBalls();
        updateHives();
    }

    private void moveRobot(Body body) {
        double speed = MAX_SPEED * body.speedScale;
        // Mecanum wheels strafe a little slower than they drive
        double[] target = toFieldFrame(body.forward * speed, body.strafe * speed * 0.85, body.heading);
        double targetVx = target[0];
        double targetVy = target[1];
        double targetOmega = body.turn * MAX_TURN * body.speedScale;

        // Motors take a moment to speed up and slow down
        double blend = Math.min(1, DT * 10);
        body.vx += (targetVx - body.vx) * blend;
        body.vy += (targetVy - body.vy) * blend;
        body.omega += (targetOmega - body.omega) * blend;
        body.x += body.vx * DT;
        body.y += body.vy * DT;
        body.heading = wrapAngle(body.heading + body.omega * DT);

        // Walls: keep the whole (turned) square inside the field
        double half = Body.SIZE / 2 * (Math.abs(Math.cos(body.heading)) + Math.abs(Math.sin(body.heading)));
        body.x = clamp(body.x, half, SIZE - half);
        body.y = clamp(body.y, half, SIZE - half);

        // The hives
        for (Hive hive : hives) {
            double[] push = hive.pushOut(body.x, body.y, ROBOT_RADIUS);
            if (push != null) {
                body.x += push[0];
                body.y += push[1];
            }
        }
    }

    private static void separate(Body a, Body b) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double distance = Math.hypot(dx, dy);
        double overlap = 2 * ROBOT_RADIUS - distance;
        if (overlap <= 0 || distance == 0) return;
        a.x -= dx / distance * overlap / 2;
        a.y -= dy / distance * overlap / 2;
        b.x += dx / distance * overlap / 2;
        b.y += dy / distance * overlap / 2;
    }

    private void runIntake(Body body) {
        if (body.intakePower <= 0) return;
        for (Iterator<Ball> it = loose.iterator(); it.hasNext() && body.held.size() < Body.CAPACITY; ) {
            Ball ball = it.next();
            double[] seen = body.toRobot(ball.x, ball.y);
            double ahead = seen[0];
            double left = seen[1];
            // The rollers reach a few inches past the front bumper, across most of its width
            if (ahead > Body.SIZE / 2 - 2 && ahead < Body.SIZE / 2 + ball.radius() + 3 && Math.abs(left) < 7) {
                it.remove();
                body.held.add(ball);
            }
        }
    }

    private void runShooter(Body body) {
        double rate = body.flywheelPower > body.flywheelSpeed ? 2.5 : 0.8;   // Spins up faster than it coasts down
        double change = (body.flywheelPower - body.flywheelSpeed) * Math.min(1, rate * DT * 4);
        body.flywheelSpeed = clamp(body.flywheelSpeed + change, 0, 1);

        if (!body.launch) return;
        body.launch = false;
        if (body.held.isEmpty()) return;
        Ball ball = body.held.remove(0);
        double[] front = body.toField(Body.SIZE / 2, 0);
        // A little spread, so shots from far away or from a bad angle sometimes miss
        double direction = body.heading + random.nextGaussian() * Math.toRadians(1.5);
        double range = SHOT_RANGE * body.flywheelSpeed * (1 + random.nextGaussian() * 0.03);
        ball.fromX = ball.x = front[0];
        ball.fromY = ball.y = front[1];
        ball.toX = front[0] + Math.cos(direction) * range;
        ball.toY = front[1] + Math.sin(direction) * range;
        ball.flight = 0;
        ball.flightTime = 0.35 + range / 60;
        flying.add(ball);
    }

    private void moveBalls() {
        double friction = Math.exp(-2.5 * DT);
        for (Ball ball : loose) {
            ball.vx *= friction;
            ball.vy *= friction;
            ball.x += ball.vx * DT;
            ball.y += ball.vy * DT;
            double r = ball.radius();
            if (ball.x < r || ball.x > SIZE - r) {
                ball.x = clamp(ball.x, r, SIZE - r);
                ball.vx *= -0.5;
            }
            if (ball.y < r || ball.y > SIZE - r) {
                ball.y = clamp(ball.y, r, SIZE - r);
                ball.vy *= -0.5;
            }
            for (Hive hive : hives) {
                double[] push = hive.pushOut(ball.x, ball.y, r);
                if (push == null) continue;
                ball.x += push[0];
                ball.y += push[1];
                double length = Math.hypot(push[0], push[1]);
                double along = (ball.vx * push[0] + ball.vy * push[1]) / length;
                if (along < 0) {
                    ball.vx -= 1.5 * along * push[0] / length;
                    ball.vy -= 1.5 * along * push[1] / length;
                }
            }
            for (Body body : bodies) pushBall(body, ball);
        }
        // Balls bump each other apart
        for (int i = 0; i < loose.size(); i++) {
            for (int j = i + 1; j < loose.size(); j++) {
                Ball a = loose.get(i);
                Ball b = loose.get(j);
                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double distance = Math.hypot(dx, dy);
                double overlap = a.radius() + b.radius() - distance;
                if (overlap <= 0 || distance == 0) continue;
                double nx = dx / distance;
                double ny = dy / distance;
                a.x -= nx * overlap / 2;
                a.y -= ny * overlap / 2;
                b.x += nx * overlap / 2;
                b.y += ny * overlap / 2;
                double closing = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny;
                if (closing > 0) {
                    a.vx -= closing * nx;
                    a.vy -= closing * ny;
                    b.vx += closing * nx;
                    b.vy += closing * ny;
                }
            }
        }
    }

    /** A robot shoves a ball it drives into. */
    private static void pushBall(Body body, Ball ball) {
        double[] seen = body.toRobot(ball.x, ball.y);
        double ahead = seen[0];
        double left = seen[1];
        double reach = Body.SIZE / 2 + ball.radius();
        if (Math.abs(ahead) >= reach || Math.abs(left) >= reach) return;
        // Out through the nearest side, moving with the robot
        if (reach - Math.abs(ahead) < reach - Math.abs(left)) {
            ahead = Math.copySign(reach, ahead);
        } else {
            left = Math.copySign(reach, left);
        }
        double[] outside = body.toField(ahead, left);
        ball.x = outside[0];
        ball.y = outside[1];
        ball.vx = body.vx * 1.2 + (ball.x - body.x) * 0.5;
        ball.vy = body.vy * 1.2 + (ball.y - body.y) * 0.5;
    }

    private void flyBalls() {
        for (Iterator<Ball> it = flying.iterator(); it.hasNext(); ) {
            Ball ball = it.next();
            ball.flight = Math.min(1, ball.flight + DT / ball.flightTime);
            ball.x = ball.fromX + (ball.toX - ball.fromX) * ball.flight;
            ball.y = ball.fromY + (ball.toY - ball.fromY) * ball.flight;
            if (ball.flight < 1) continue;
            it.remove();
            land(ball);
        }
    }

    private void land(Ball ball) {
        for (Hive hive : hives) {
            int end = hive.cupAt(ball.x, ball.y);
            if (end >= 0) {
                hive.add(ball, end);
                return;
            }
        }
        // Missed: it bounces on and rolls on in the direction it was flying
        double dx = ball.toX - ball.fromX;
        double dy = ball.toY - ball.fromY;
        double length = Math.max(1e-6, Math.hypot(dx, dy));
        ball.vx = dx / length * 25;
        ball.vy = dy / length * 25;
        ball.x = clamp(ball.x, ball.radius(), SIZE - ball.radius());
        ball.y = clamp(ball.y, ball.radius(), SIZE - ball.radius());
        loose.add(ball);
    }

    private void updateHives() {
        for (Hive hive : hives) {
            List<Ball> spilled = hive.settle(DT);
            if (spilled.isEmpty()) continue;   // Didn't pivot: a pivot always tips out the 20+ units that pulled it down
            // Shows up in the visualizer's Printed panel
            System.out.println(hive.alliance + "'s hive pivoted to its " + (hive.down == Hive.TOP ? "top" : "bottom")
                    + " end: +" + Hive.PIVOT_POINTS + " for " + hive.alliance + ", and " + spilled.size()
                    + " balls fall out");
            // They tumble out of the cup onto the floor around that end of the hive
            double cupY = hive.cupY(hive.down);
            double outward = hive.down == Hive.TOP ? 1 : -1;
            for (Ball ball : spilled) {
                double angle = random.nextDouble() * Math.PI * 2;
                ball.x = hive.x + Math.cos(angle) * 4;
                ball.y = cupY + Math.sin(angle) * 4;
                double speed = 15 + random.nextDouble() * 25;
                ball.vx = Math.cos(angle) * speed;
                ball.vy = Math.sin(angle) * speed + outward * 10;
                loose.add(ball);
            }
        }
    }
}
