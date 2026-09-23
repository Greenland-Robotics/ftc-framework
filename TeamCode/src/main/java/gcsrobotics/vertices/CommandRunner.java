package gcsrobotics.vertices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class CommandRunner {
    private final List<Command> commands = new ArrayList<>();

    public CommandRunner(Command... commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    public CommandRunner() {
    }

    public void start() {
        for (Command command : commands) {
            command.init();
        }
    }

    public void update() {
        int i = 0;

        while (i < commands.size()) {
            Command command = commands.get(i);

            command.loop();

            if (command.isFinished()) {
                commands.remove(i);
            }
            else {
                i++;
            }
        }
    }

    public void run(Command command) {
        command.init();
        commands.add(command);
    }

    public boolean isFinished() {
        return commands.isEmpty();
    }
}