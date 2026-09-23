package gcsrobotics.vertices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SeriesCommand implements Command {
    private final List<Command> commands = new ArrayList<>();

    private int index = 0;
    private boolean finished = false;

    public SeriesCommand(Command... inputCommands) {
        commands.addAll(Arrays.asList(inputCommands));
    }

    public void addCommands(Command... inputCommands) {
        commands.addAll(Arrays.asList(inputCommands));
    }

    public void init() {
        index = 0;

        finished = false;

        if (!commands.isEmpty()) {
            commands.get(0).init();
        }
    }

    public void loop() {
        if (commands.isEmpty()) {
            finished = true;
            return;
        }

        commands.get(index).loop();

        if (commands.get(index).isFinished()) {
            if (index == commands.size() - 1) {
                finished = true;
            }
            else {
                commands.get(++index).init();
            }
        }
    }

    public boolean isFinished() {
        return finished;
    }
}
