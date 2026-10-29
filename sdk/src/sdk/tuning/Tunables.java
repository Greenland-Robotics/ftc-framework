package sdk.tuning;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Finds the tunable parts on a robot by looking through its public fields: {@code robot.lift}
 * is "lift", and a part inside a season subsystem, {@code robot.scorer.lift}, is "scorer.lift".
 * So every Lift, Arm and Flywheel shows up in the Tune OpMode without any setup.
 */
public final class Tunables {
    private Tunables() {}

    public static Map<String, Tunable> find(Object robot) {
        Map<String, Tunable> found = new LinkedHashMap<>();
        collect(robot, "", 0, found);
        return found;
    }

    private static void collect(Object owner, String prefix, int depth, Map<String, Tunable> found) {
        if (owner == null || depth > 2) return;
        for (Field field : owner.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            Object value;
            try {
                value = field.get(owner);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (value == null) continue;
            String name = prefix + field.getName();
            if (value instanceof Tunable) {
                found.put(name, (Tunable) value);
            } else if (value instanceof subsystem.Subsystem) {
                collect(value, name + ".", depth + 1, found);
            }
        }
    }
}
