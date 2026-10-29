package sdk.tuning;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Tuned gains kept in a file, one line per gain ({@code scorer.lift.kP=0.0035}). The Tune
 * OpMode saves to it; {@link #apply} puts saved gains on the robot's parts when it starts, in
 * place of the numbers in Robot.java. On the robot the file is
 * {@code /sdcard/FIRST/tuning.properties} (see {@code sdk.ftc.Tuning}).
 */
public final class SavedGains {
    private final File file;

    public SavedGains(File file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public File file() {
        return file;
    }

    /** Sets every saved gain on the matching part; returns the names of the parts that got any. */
    public List<String> apply(Map<String, Tunable> parts) {
        Properties saved = load();
        List<String> applied = new ArrayList<>();
        for (Map.Entry<String, Tunable> part : parts.entrySet()) {
            boolean any = false;
            for (Gain gain : part.getValue().gains()) {
                String value = saved.getProperty(part.getKey() + "." + gain.name());
                if (value == null) continue;
                try {
                    gain.set(Double.parseDouble(value));
                    any = true;
                } catch (NumberFormatException ignored) {
                    // A hand-edited line that isn't a number: keep the gain from Robot.java
                }
            }
            if (any) applied.add(part.getKey());
        }
        return applied;
    }

    /** Saves all of {@code part}'s gains under {@code name}, keeping everything else in the file. */
    public void save(String name, Tunable part) throws IOException {
        Properties saved = load();
        for (Gain gain : part.gains()) {
            saved.setProperty(name + "." + gain.name(), TuneReport.number(gain.get()));
        }
        store(saved);
    }

    /** Removes {@code name}'s saved gains, so Robot.java's numbers are used again. */
    public void forget(String name) throws IOException {
        Properties saved = load();
        for (String key : new TreeSet<>(saved.stringPropertyNames())) {
            if (key.startsWith(name + ".")) saved.remove(key);
        }
        store(saved);
    }

    /** Whether anything is saved for {@code name}. */
    public boolean has(String name) {
        for (String key : load().stringPropertyNames()) {
            if (key.startsWith(name + ".")) return true;
        }
        return false;
    }

    private Properties load() {
        Properties saved = new Properties();
        if (!file.exists()) return saved;
        try (InputStream in = new FileInputStream(file)) {
            saved.load(in);
        } catch (IOException e) {
            util.Log.warn("Tuning", "couldn't read %s: %s", file, e.getMessage());
        }
        return saved;
    }

    private void store(Properties saved) throws IOException {
        File folder = file.getParentFile();
        if (folder != null && !folder.exists() && !folder.mkdirs()) {
            throw new IOException("couldn't create " + folder);
        }
        try (OutputStream out = new FileOutputStream(file)) {
            saved.store(out, "Tuned gains, saved by the Tune OpMode. Used in place of the numbers in Robot.java.");
        }
    }
}
