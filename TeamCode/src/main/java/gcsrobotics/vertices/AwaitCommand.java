package gcsrobotics.vertices;

public class AwaitCommand implements Command {
    private final Condition condition;

    private final boolean useTimeout;

    private double startTime;
    private double timeoutDuration;

    public AwaitCommand(Condition condition) {
        this.condition = condition;

        useTimeout = false;
    }

    public AwaitCommand(Condition condition, double timeoutDurationMillis) {
        this.condition = condition;

        useTimeout = true;
        this.timeoutDuration = timeoutDurationMillis;
    }

    public void init() {
        startTime = System.nanoTime();
    }

    public void loop() {
    }

    public boolean isFinished() {
        return condition.getValue() || (useTimeout && System.nanoTime() - startTime > timeoutDuration * 1e6);
    }
}
