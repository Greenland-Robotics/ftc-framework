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
        // robot.drive.setStartingPose(new Pose(0, 0, Math.toRadians(0)));
    }

    @Override
    protected Node routine() {
        return sequence(
                // robot.drive.follow(...), robot.intake.collect(), ...
        );
    }

    @Override
    protected void runLoop() {
        telemetry.addData("Pose", robot.drive.pose());
    }
}
