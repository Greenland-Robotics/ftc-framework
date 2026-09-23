package gcsrobotics.vertices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParallelCommand implements Command {
    private final List<Command> commands = new ArrayList<>();

    private boolean finished = false;

    public ParallelCommand(Command... inputCommands) {
        commands.addAll(Arrays.asList(inputCommands));
    }

    public void addCommands(Command... inputCommands) {
        commands.addAll(Arrays.asList(inputCommands));
    }

    public void init() {
        finished = false;

        if (!commands.isEmpty()) {
            for (Command command : commands) {
                command.init();
            }
        }
    }

    public void loop() {
        boolean finished = true;

        for (Command command : commands) {
            command.loop();

            if (!command.isFinished()) {
                finished = false;
            }
        }

        this.finished = finished;
    }

    public boolean isFinished() {
        return finished;
    }
}
