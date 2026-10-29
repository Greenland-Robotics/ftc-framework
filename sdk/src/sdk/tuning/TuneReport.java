package sdk.tuning;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import util.Log;

/** What a tuning run did and found, line by line, for the Driver Station and the log. */
public final class TuneReport {
    private final String name;
    private final List<String> lines = new ArrayList<>();
    private boolean finished;
    private String failure;

    public TuneReport(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** Adds a line (String.format style), and logs it. */
    public void log(String format, Object... args) {
        String line = String.format(Locale.ROOT, format, args);
        lines.add(line);
        Log.info("Tune " + name, "%s", line);
    }

    public void finish() {
        finished = true;
    }

    public void fail(String why) {
        failure = why;
        finished = true;
        log("Stopped: %s", why);
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean failed() {
        return failure != null;
    }

    public String failure() {
        return failure;
    }

    public List<String> lines() {
        return Collections.unmodifiableList(lines);
    }

    /** The last {@code count} lines, for a screen that fits only a few. */
    public List<String> lastLines(int count) {
        return lines.subList(Math.max(0, lines.size() - count), lines.size());
    }

    public void clear() {
        lines.clear();
        finished = false;
        failure = null;
    }

    /** A gain, rounded to 3 significant digits and written out in full: 0.00412, 0.12, 6000. */
    public static String number(double value) {
        if (value == 0 || Double.isNaN(value) || Double.isInfinite(value)) return value == 0 ? "0" : String.valueOf(value);
        return new BigDecimal(value).round(new MathContext(3)).stripTrailingZeros().toPlainString();
    }
}
