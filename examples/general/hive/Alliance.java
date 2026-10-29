package hive;

/** The two alliances. Red's hive is on the left half of the field, Blue's on the right. */
public enum Alliance {
    RED(-1), BLUE(1);

    /** -1 for the left half of the field, +1 for the right. */
    final int side;

    Alliance(int side) {
        this.side = side;
    }

    String color() {
        return this == RED ? "red" : "blue";
    }
}
