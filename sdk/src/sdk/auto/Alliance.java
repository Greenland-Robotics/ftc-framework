package sdk.auto;

/** Which side you're on this match. The Auto menu picks it during init: {@code alliance()} in AutoBase. */
public enum Alliance {
    RED, BLUE;

    public Alliance other() {
        return this == RED ? BLUE : RED;
    }

    /** {@code ifRed} on the red alliance, {@code ifBlue} on blue: {@code alliance.pick(LEFT, RIGHT)}. */
    public <T> T pick(T ifRed, T ifBlue) {
        return this == RED ? ifRed : ifBlue;
    }
}
