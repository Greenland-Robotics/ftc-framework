package gcsrobotics.vertices;

public class SwitchCommand implements Command {
    private final Condition condition;
    private final Command action;

    private Command fallback;

    private boolean eval;
    private boolean hasFallback = false;

    public SwitchCommand(Condition condition, Command action) {
        this.condition = condition;
        this.action = action;
    }

    public SwitchCommand(
        Condition condition,
        Command action,
        Command fallback
    ) {
        this.condition = condition;
        this.action = action;

        this.fallback = fallback;
        this.hasFallback = true;
    }

    public void init() {
        eval = condition.getValue();

        if (eval) {
            action.init();
        }
        else if (hasFallback){
            fallback.init();
        }
    }

    public void loop() {
        if (eval) {
            action.loop();
        }
        else if (hasFallback) {
            fallback.loop();
        }
    }

    public boolean isFinished() {
        if (eval) {
            return action.isFinished();
        }
        else if (hasFallback) {
            return fallback.isFinished();
        }
        else{
            return true;
        }
    }
}
