package hive;

import behavior.Action;
import behavior.Node;
import behavior.Status;
import subsystem.Subsystem;

/** Rollers at the front that pull in balls, up to {@value Body#CAPACITY} at a time. */
public final class Intake implements Subsystem {
    private final Body body;

    Intake(Body body) {
        this.body = body;
    }

    public int ballCount() {
        return body.held.size();
    }

    public boolean hasBall() {
        return !body.held.isEmpty();
    }

    public boolean isFull() {
        return body.held.size() >= Body.CAPACITY;
    }

    /** Spins the rollers in until halted (e.g. when the driver lets go of the button). */
    public Node run() {
        return new Spin();
    }

    /** Spins the rollers in until one more ball is inside, then stops. */
    public Node collect() {
        return new Collect();
    }

    private final class Spin extends Action {
        @Override
        protected void start() {
            body.intakePower = 1;
        }

        @Override
        protected Status update() {
            return Status.RUNNING;
        }

        @Override
        protected void end(boolean interrupted) {
            body.intakePower = 0;
        }
    }

    private final class Collect extends Action {
        private int before;

        @Override
        protected void start() {
            before = ballCount();
            body.intakePower = 1;
        }

        @Override
        protected Status update() {
            return ballCount() > before || isFull() ? Status.SUCCESS : Status.RUNNING;
        }

        @Override
        protected void end(boolean interrupted) {
            body.intakePower = 0;
        }
    }
}
