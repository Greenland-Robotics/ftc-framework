package gcsrobotics.vertices;

public class SleepCommand implements Command {
    private double startTime;
    private final double targetTime;

    public SleepCommand(double targetTime) {
        this.targetTime = targetTime;
    }

    public void init() {
        startTime = System.nanoTime();
    }

    public void loop() {
    }

    public boolean isFinished() {
        return System.nanoTime() - startTime > targetTime * 1e6;
    }
}
