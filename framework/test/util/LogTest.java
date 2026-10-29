package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogTest {
    private final List<String> lines = new ArrayList<>();
    private Log.Level levelBefore;

    @BeforeEach
    void captureLines() {
        levelBefore = Log.level();
        Log.setSink((level, tag, message) -> lines.add(level + " " + tag + ": " + message));
    }

    @AfterEach
    void restore() {
        Log.setSink(Log.STANDARD_OUT);
        Log.setLevel(levelBefore);
    }

    @Test
    void writesOnlyAtOrAboveTheLevel() {
        Log.setLevel(Log.Level.INFO);
        Log.debug("Arm", "hidden");
        Log.info("Arm", "shown");
        Log.error("Arm", "also shown");
        assertEquals(Arrays.asList("INFO Arm: shown", "ERROR Arm: also shown"), lines);
    }

    @Test
    void debugLevelShowsEverything() {
        Log.setLevel(Log.Level.DEBUG);
        Log.debug("Intake", "ball %d in, %.1f s", 4, 1.25);
        assertEquals(Arrays.asList("DEBUG Intake: ball 4 in, 1.3 s"), lines);
    }

    @Test
    void offWritesNothing() {
        Log.setLevel(Log.Level.OFF);
        Log.error("Drive", "hidden");
        assertTrue(lines.isEmpty());
        assertFalse(Log.isEnabled(Log.Level.ERROR));
    }

    @Test
    void levelFromText() {
        Log.setLevel("debug");
        assertEquals(Log.Level.DEBUG, Log.level());
        Log.setLevel("nonsense");   // Unknown: unchanged
        assertEquals(Log.Level.DEBUG, Log.level());
    }

    @Test
    void textWithoutArgumentsIsNotFormatted() {
        Log.setLevel(Log.Level.INFO);
        Log.info("Shooter", "100% speed");
        assertEquals(Arrays.asList("INFO Shooter: 100% speed"), lines);
    }
}
