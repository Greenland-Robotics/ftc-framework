package commands;

public interface Command {
    void init();

    void loop();

    boolean isFinished();
}
