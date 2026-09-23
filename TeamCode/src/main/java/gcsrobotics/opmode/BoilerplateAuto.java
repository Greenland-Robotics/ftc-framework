package gcsrobotics.opmode;

import gcsrobotics.control.AutoBase;
import gcsrobotics.vertices.Command;

public class BoilerplateAuto extends AutoBase {

    // Name your commands based on what that command actually does, eg. `shoot`
    private Command command1, command2 /* etc. */;
    private Paths paths;

    public void buildCommands() {
        // command1 = new SeriesCommand/ParallelCommand etc.
        // command2 = new SeriesCommand/ParallelCommand etc.
        // ....
    }

    public void initialize(){
//        follower.setStartingPose(new Pose(0, 0, Math.toRadians(0)));
//        paths = new Paths(follower);
//        commandRunner = new CommandRunner(new SeriesCommand(
//                command1,
//                command2,
//                ....
//        ));

        // other init code here
    }

    public void runLoop() {
    }

    private static class Paths {
        /// This class should be replaced with the code generated from https://visualizer.pedropathing.com
    }
}
