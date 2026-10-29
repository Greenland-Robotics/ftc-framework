package hive;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import behavior.runtime.Clock;
import subsystem.Subsystem;

/**
 * A flywheel that launches balls straight ahead, and a feeder that pushes the next held ball into
 * it. At full speed a ball lands {@value Field#SHOT_RANGE} inches in front of the robot.
 */
public final class Shooter implements Subsystem {
    private static final long FEED_MS = 150;

    private final Body body;
    private final Clock clock;

    Shooter(Body body, Clock clock) {
        this.body = body;
        this.clock = clock;
    }

    public boolean atSpeed() {
        return body.flywheelSpeed >= 0.97;
    }

    /** Spins the flywheel up and keeps it spinning until halted. */
    public Node spin() {
        return new Spin();
    }

    /** Pushes the next ball into the flywheel. */
    public Node feed() {
        return new Feed();
    }

    private final class Spin extends Action {
        @Override
        protected void start() {
            body.flywheelPower = 1;
        }

        @Override
        protected Status update() {
            return Status.RUNNING;
        }

        @Override
        protected void end(boolean interrupted) {
            body.flywheelPower = 0;
        }
    }

    private final class Feed extends Action {
        private long startedAt;

        @Override
        protected void start() {
            startedAt = clock.nowMillis();
        }

        @Override
        protected Status update() {
            if (clock.nowMillis() - startedAt < FEED_MS) return Status.RUNNING;
            body.launch = true;
            return Status.SUCCESS;
        }
    }
}
