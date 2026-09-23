package gcsrobotics.control;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import gcsrobotics.pedroPathing.Constants;
import gcsrobotics.vertices.CommandRunner;

public abstract class OpModeBase extends LinearOpMode {
    public static volatile OpModeBase INSTANCE;
    public Follower follower;
    protected CommandRunner commandRunner;

    protected abstract void initInternal();
    protected abstract void loopInternal();

    @Override
    public void runOpMode() {
        initHardware();
        follower = Constants.createFollower(hardwareMap);
        INSTANCE = this;
        initInternal();

        waitForStart();

        while(opModeIsActive() && !isStopRequested()) {
            telemetry.update();
            loopInternal();
        }
    }

    // declare hardware here
    private void initHardware() {
        // init hardware here
    }

    protected double getX() {return follower.getPose().getX();}
    protected double getY() {return follower.getPose().getY();}
    protected double getHeading() {return follower.getPose().getHeading();}

}
