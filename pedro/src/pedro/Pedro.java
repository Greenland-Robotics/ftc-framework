package pedro;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.hardware.HardwareMap;

/// Holds the Follower for the running OpMode so commands such as FollowPath can reach it.
public final class Pedro {
    private static Follower follower;

    private Pedro() {}

    /// Builds the Follower from Constants and makes it the current one. Called once per OpMode by OpModeBase.
    public static Follower createFollower(HardwareMap hardwareMap) {
        follower = Constants.createFollower(hardwareMap);
        return follower;
    }

    /// @return The Follower of the running OpMode
    public static Follower follower() {
        if (follower == null) {
            throw new IllegalStateException("No Follower yet: call Pedro.createFollower(hardwareMap) first");
        }
        return follower;
    }
}
