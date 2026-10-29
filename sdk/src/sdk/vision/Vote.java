package sdk.vision;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

/**
 * Turns a camera's frame-by-frame readings, which flicker, into one steady answer: the most
 * common reading over the last few frames, once enough of them agree. Until then, UNKNOWN.
 */
public final class Vote {
    private final int window;
    private final int needed;
    private final Deque<Randomization> recent = new ArrayDeque<>();

    /** The answer is the reading at least {@code needed} of the last {@code window} frames agree on. */
    public Vote(int window, int needed) {
        if (window < 1 || needed < 1 || needed > window) {
            throw new IllegalArgumentException("Vote needs 1 <= needed <= window");
        }
        this.window = window;
        this.needed = needed;
    }

    /** Adds one frame's reading (UNKNOWN or null if it saw nothing). */
    public void add(Randomization reading) {
        recent.addLast(reading == null ? Randomization.UNKNOWN : reading);
        while (recent.size() > window) recent.removeFirst();
    }

    public Randomization result() {
        Map<Randomization, Integer> counts = new EnumMap<>(Randomization.class);
        Randomization best = Randomization.UNKNOWN;
        int bestCount = 0;
        for (Randomization reading : recent) {
            if (reading == Randomization.UNKNOWN) continue;
            int count = counts.merge(reading, 1, Integer::sum);
            if (count > bestCount) {
                best = reading;
                bestCount = count;
            }
        }
        return bestCount >= needed ? best : Randomization.UNKNOWN;
    }

    public void clear() {
        recent.clear();
    }
}
