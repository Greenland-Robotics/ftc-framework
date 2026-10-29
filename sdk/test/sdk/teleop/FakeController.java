package sdk.teleop;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** A controller a test sets by hand. */
final class FakeController extends Controller {
    final Set<Button> down = EnumSet.noneOf(Button.class);
    final Map<Axis, Double> axes = new EnumMap<>(Axis.class);

    @Override
    protected boolean isDown(Button button) {
        return down.contains(button);
    }

    @Override
    protected double raw(Axis axis) {
        return axes.getOrDefault(axis, 0.0);
    }
}
