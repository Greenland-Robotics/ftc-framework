package robot.opmode;

import static behavior.Behaviors.*;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;

import behavior.Node;
import robot.control.AutoBase;

@Autonomous(name = "OpMode name here")
@Disabled
public class BoilerplateAuto extends AutoBase {
    @Override
    protected void initialize() {
        // Add your own menu lines: start = menu().choice("Start", Start.values());
    }

    @Override
    protected Node routine() {
        // Runs at start, after the menu: poses are written for blue and flipped for red
        // robot.drive.setStartingPose(pose(56, 8, 90));
        return sequence(
                // robot.drive.driveTo(pose(56, 36, 90)), robot.intake.collect(), ...
        );
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Pose", robot.drive.pose());
    }
}
