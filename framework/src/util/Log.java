package util;

import java.util.Locale;

/**
 * Leveled logging for robot code, subsystems and simulations. Plain Java, so it works on the
 * robot and on a laptop. By default it prints to standard out:
 *
 *     Log.debug("Intake", "ball %d in, %d held", id, count);   // DEBUG [Intake] ball 4 in, 2 held
 *
 * Only messages at or above the current level are printed (and only those are formatted, so a
 * debug call costs almost nothing while debug is off). The level starts from the
 * {@code log.level} system property ({@code -Dlog.level=debug}), else INFO; change it with
 * {@link #setLevel}. To send messages somewhere else, e.g. the FTC SDK's RobotLog or telemetry
 * on the robot, give it a {@link Sink}.
 */
public final class Log {
    private Log() {}

    public enum Level { DEBUG, INFO, WARN, ERROR, OFF }

    /** Where messages go once they pass the level check. */
    public interface Sink {
        void write(Level level, String tag, String message);
    }

    /** Prints each message as one line on standard out: {@code DEBUG [tag] message}. */
    public static final Sink STANDARD_OUT = (level, tag, message) ->
            System.out.println(level + " [" + tag + "] " + message);

    private static volatile Level level = parse(System.getProperty("log.level"), Level.INFO);
    private static volatile Sink sink = STANDARD_OUT;

    public static Level level() {
        return level;
    }

    public static void setLevel(Level newLevel) {
        level = newLevel;
    }

    /** Sets the level from text such as "debug" or "WARN"; unknown or null text leaves it as is. */
    public static void setLevel(String name) {
        level = parse(name, level);
    }

    public static void setSink(Sink newSink) {
        sink = newSink;
    }

    /** Whether a message at {@code messageLevel} would be written, e.g. to skip building costly debug text. */
    public static boolean isEnabled(Level messageLevel) {
        return messageLevel != Level.OFF && messageLevel.compareTo(level) >= 0;
    }

    public static void debug(String tag, String format, Object... args) {
        write(Level.DEBUG, tag, format, args);
    }

    public static void info(String tag, String format, Object... args) {
        write(Level.INFO, tag, format, args);
    }

    public static void warn(String tag, String format, Object... args) {
        write(Level.WARN, tag, format, args);
    }

    public static void error(String tag, String format, Object... args) {
        write(Level.ERROR, tag, format, args);
    }

    private static void write(Level messageLevel, String tag, String format, Object... args) {
        if (!isEnabled(messageLevel)) return;
        String message = args.length == 0 ? format : String.format(Locale.ROOT, format, args);
        sink.write(messageLevel, tag, message);
    }

    private static Level parse(String name, Level fallback) {
        if (name == null) return fallback;
        try {
            return Level.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
