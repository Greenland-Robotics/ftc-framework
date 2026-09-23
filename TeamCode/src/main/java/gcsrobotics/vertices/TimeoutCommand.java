package gcsrobotics.vertices;

public class TimeoutCommand implements Command {
    private final Command command;
    private final long timeoutMillis;
    private long startTime;

    public TimeoutCommand(Command command, long timeoutMillis) {
        this.command = command;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public void init() {
        command.init();
        startTime = System.currentTimeMillis();
    }

    @Override
    public void loop() {
        command.loop();
    }

    @Override
    public boolean isFinished() {
        return command.isFinished() || (System.currentTimeMillis() - startTime > timeoutMillis);
    }
}