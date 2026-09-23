package gcsrobotics.vertices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CancelCommand implements Command {
    private final List<Command> commands = new ArrayList<>();
    private boolean commandCanceled = false;
    private boolean disabled = false;

    public CancelCommand(Command... commands){
        this.commands.addAll(Arrays.asList(commands));
    }

    @Override
    public void init() {
        for(Command command : commands){
            command.init();
        }
    }

    @Override
    public void loop() {
        List<Command> toRemove = new ArrayList<>();
        if(!disabled) {
            for(Command command : commands) {
                command.loop();

                if(command.isFinished()){
                    toRemove.add(command);
                }
            }
            commands.removeAll(toRemove);
        }
    }

    ///Permanently halts all commands and marks as finished.
    public void cancel(){
        commands.clear();
        commandCanceled = true;
    }

    ///Temporarily halt the commands from running
    public void disable(){
        disabled = !commandCanceled;
    }

    ///Resume the commands
    public void enable(){
        disabled = commandCanceled;
    }

    ///@return The disabled state of the command
    public boolean isDisabled(){
        return disabled;
    }

    @Override
    public boolean isFinished() {
        return commands.isEmpty() || commandCanceled;
    }
}
