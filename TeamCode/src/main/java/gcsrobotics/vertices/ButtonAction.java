package gcsrobotics.vertices;

public class ButtonAction {
    public boolean lastPressed = false;

    private final Command command;
    private final CommandRunner commandRunner;

    public ButtonAction(Command command, CommandRunner commandRunner) {
        this.command = command;
        this.commandRunner = commandRunner;
    }

    public void update(boolean input) {
        if (input && !lastPressed) {
            lastPressed = true;
            commandRunner.run(command);
        }
        else if (!input) {
            lastPressed = false;
        }
    }
}