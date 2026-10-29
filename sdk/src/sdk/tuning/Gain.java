package sdk.tuning;

import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** One number a part is tuned by (kP, gravity...), readable and changeable while the robot runs. */
public final class Gain {
    private final String name;
    private final DoubleSupplier get;
    private final DoubleConsumer set;

    public Gain(String name, DoubleSupplier get, DoubleConsumer set) {
        this.name = Objects.requireNonNull(name, "name");
        this.get = Objects.requireNonNull(get, "get");
        this.set = Objects.requireNonNull(set, "set");
    }

    public String name() {
        return name;
    }

    public double get() {
        return get.getAsDouble();
    }

    public void set(double value) {
        set.accept(value);
    }
}
