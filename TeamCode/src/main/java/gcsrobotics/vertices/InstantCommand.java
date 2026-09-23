package gcsrobotics.vertices;

public class InstantCommand implements Command {
    private final Runnable toRun;
    private boolean initialLoop = true;

    public InstantCommand(Runnable toRun) {
        this.toRun = toRun;
    }

    public void init() {}

    public void loop() {
        if(initialLoop) toRun.run(); initialLoop = false;
    }

    public boolean isFinished() {
        return true;
    }
}
