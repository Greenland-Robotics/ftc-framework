package hive;

import static util.MathUtil.distance;

import java.util.Locale;

import visualizer.Json;

/**
 * Draws the field for the visualizer page as a list of shapes, in inches with y pointing up.
 * Colors are names the page knows ("red", "ball", ...), so it can match light and dark mode.
 */
final class FieldDrawing {
    private static final double BOARD = 22;   // Height of the scoreboard above the field

    private final Json json = new Json();

    private FieldDrawing() {}

    /** The field, with {@code driver}'s aim shown as a dotted line. */
    static String draw(Field field, Body driver) {
        FieldDrawing d = new FieldDrawing();
        d.json.begin().field("width", Field.SIZE).field("height", Field.SIZE + BOARD).name("shapes").beginArray();
        d.field();
        for (Hive hive : field.hives) d.hive(hive);
        for (Ball ball : field.loose) d.ball(ball.x, ball.y, ball.radius(), ball);
        if (driver != null) d.aim(driver, field.hive(driver.alliance));
        for (Body body : field.bodies) d.robot(body, body == driver);
        for (Ball ball : field.flying) d.flyingBall(ball);
        d.scoreboard(field);
        d.json.endArray().end();
        return d.json.toString();
    }

    private void field() {
        rect(Field.SIZE / 2, Field.SIZE / 2, Field.SIZE, Field.SIZE, 0, "field", "line", 0.6);
        for (int i = 1; i < 6; i++) {
            line("tile", 0.3, null, i * 24, 0, i * 24, Field.SIZE);
            line("tile", 0.3, null, 0, i * 24, Field.SIZE, i * 24);
        }
    }

    private void hive(Hive hive) {
        String color = hive.alliance.color();
        rect(hive.x, hive.y, Hive.WIDTH, Hive.LENGTH, 0, "hive", color, 0.5);
        // The beam: seen from above, the end that is down looks shorter and the end that is up longer
        double shrink = Math.abs(hive.tilt) * 1.5;
        double[] reach = new double[2];
        for (int end = 0; end < 2; end++) {
            boolean goingDown = end == Hive.TOP ? hive.tilt > 0 : hive.tilt < 0;
            reach[end] = Hive.CUP_OFFSET + (goingDown ? -shrink : shrink);
        }
        line("hiveEdge", 1.6, null, hive.x, hive.y + reach[Hive.TOP], hive.x, hive.y - reach[Hive.BOTTOM]);
        poly("hiveEdge", null, hive.x - 4, hive.y - 2.5, hive.x - 4, hive.y + 2.5, hive.x + 2, hive.y);
        for (int end = 0; end < 2; end++) {
            boolean down = hive.down == end;
            double y = end == Hive.TOP ? hive.y + reach[Hive.TOP] : hive.y - reach[Hive.BOTTOM];
            circle(hive.x, y, Hive.CUP_RADIUS, down ? color : color + "Soft", color, down ? 1.2 : 0.6);
            text(hive.x, y, String.valueOf(hive.weight[end]), 4.6, down ? "board" : color, "center", true);
            // Beyond the end: DOWN, or how much more it needs to pivot
            double labelY = end == Hive.TOP ? y + Hive.CUP_RADIUS + 3 : y - Hive.CUP_RADIUS - 3;
            String label = down ? "DOWN" : "+" + hive.unitsToTip(end) + " to pivot";
            text(hive.x, labelY, label, 3, down ? color : "muted", "center", true);
        }
    }

    private void ball(double x, double y, double r, Ball ball) {
        circle(x, y, r, ball.softball ? "softball" : "ball", "ballEdge", 0.25);
    }

    private void flyingBall(Ball ball) {
        // Higher in the middle of its flight: bigger, with its shadow left on the ground
        double height = Math.sin(Math.PI * ball.flight);
        circle(ball.x, ball.y, ball.radius(), "shadow", null, 0);
        ball(ball.x, ball.y + height * 6, ball.radius() * (1 + 0.6 * height), ball);
    }

    /** A dotted line to where a shot would land: green when it would land in the end to pivot. */
    private void aim(Body body, Hive hive) {
        double[] front = body.toField(Body.SIZE / 2, 0);
        double[] landing = body.toField(Body.SIZE / 2 + Field.SHOT_RANGE, 0);
        int end = hive.endToShoot(body.x, body.y);
        boolean onTarget = distance(landing[0], landing[1], hive.cupX(end), hive.cupY(end)) <= Hive.CUP_RADIUS - 1;
        String color = onTarget ? "good" : "muted";
        line(color, 0.5, new double[] {1.2, 1.2}, front[0], front[1], landing[0], landing[1]);
        circle(landing[0], landing[1], 2, null, color, 0.5);
    }

    private void robot(Body body, boolean isDriver) {
        String color = body.alliance.color();
        rect(body.x, body.y, Body.SIZE, Body.SIZE, body.heading, color + "Soft", color, isDriver ? 1.2 : 0.7);

        // Intake rollers across the front: bright while spinning
        double[] a = body.toField(Body.SIZE / 2, 7);
        double[] b = body.toField(Body.SIZE / 2, -7);
        line(body.intakePower > 0 ? "good" : color, 1.6, null, a[0], a[1], b[0], b[1]);

        // Flywheel at the back: fills in as it spins up
        double[] wheel = body.toField(-4.5, 0);
        circle(wheel[0], wheel[1], 3, null, color, 0.5);
        if (body.flywheelSpeed > 0.02) circle(wheel[0], wheel[1], 3 * body.flywheelSpeed, color, null, 0);

        // Balls inside
        for (int i = 0; i < body.held.size(); i++) {
            double[] p = body.toField(3, (i - 1) * 4.6);
            ball(p[0], p[1], body.held.get(i).radius(), body.held.get(i));
        }
        text(body.x, body.y - Body.SIZE / 2 - 3, isDriver ? "YOU" : "AI", 3.2, color, "center", true);
    }

    private void scoreboard(Field field) {
        double mid = Field.SIZE + BOARD / 2;
        rect(Field.SIZE / 2, mid, Field.SIZE, BOARD, 0, "board", null, 0);
        for (Alliance alliance : Alliance.values()) {
            boolean red = alliance == Alliance.RED;
            double x = red ? 4 : Field.SIZE - 4;
            String align = red ? "left" : "right";
            text(x, mid + 3.5, String.valueOf(field.score(alliance)), 10, alliance.color(), align, true);
            int pivots = field.hive(alliance).pivots;
            text(x, mid - 6.5, alliance + " · " + pivots + (pivots == 1 ? " pivot" : " pivots"), 3.4, "muted", align, false);
        }
        double left = Math.max(0, field.matchSeconds() - field.time());
        double periodLeft = field.inAuto() ? Field.AUTO_SECONDS - field.time() : left;
        String period = left == 0 ? "MATCH OVER" : field.inAuto() ? "AUTO" : "TELEOP";
        text(72, mid + 3, period + "  " + clock(periodLeft), 5, "ink", "center", true);
        text(72, mid - 5, field.ballsLeft() + " balls loose · softball = 3 wiffle balls", 3, "muted", "center", false);
    }

    private static String clock(double seconds) {
        int s = (int) Math.ceil(seconds);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    // ---- Shapes the page can draw ----

    private void rect(double x, double y, double w, double h, double angle, String fill, String stroke, double width) {
        json.begin().field("t", "rect").field("x", x).field("y", y).field("w", w).field("h", h)
                .field("a", angle).field("fill", fill).field("stroke", stroke).field("lw", width).end();
    }

    private void circle(double x, double y, double r, String fill, String stroke, double width) {
        json.begin().field("t", "circle").field("x", x).field("y", y).field("r", r)
                .field("fill", fill).field("stroke", stroke).field("lw", width).end();
    }

    private void line(String stroke, double width, double[] dash, double... points) {
        json.begin().field("t", "line").field("stroke", stroke).field("lw", width);
        if (dash != null) {
            json.name("dash").beginArray();
            for (double d : dash) json.value(d);
            json.endArray();
        }
        points(points);
        json.end();
    }

    private void poly(String fill, String stroke, double... points) {
        json.begin().field("t", "poly").field("fill", fill).field("stroke", stroke).field("lw", 0.4);
        points(points);
        json.end();
    }

    private void points(double... points) {
        json.name("pts").beginArray();
        for (double p : points) json.value(p);
        json.endArray();
    }

    private void text(double x, double y, String text, double size, String fill, String align, boolean bold) {
        json.begin().field("t", "text").field("x", x).field("y", y).field("text", text).field("size", size)
                .field("fill", fill).field("align", align).field("bold", bold).end();
    }
}
