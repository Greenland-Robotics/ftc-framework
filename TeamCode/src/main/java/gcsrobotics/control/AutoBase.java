package gcsrobotics.control;

import gcsrobotics.vertices.CommandRunner;

public abstract class AutoBase extends OpModeBase {
    protected CommandRunner commandRunner;
    protected abstract void buildCommands();
    protected abstract void initialize();
    protected abstract void runLoop();

    @Override
    protected void initInternal() {
        initialize();
        buildCommands();
        commandRunner.start();
    }

    @Override
    protected void loopInternal() {
        commandRunner.update();
        runLoop();
    }

}
