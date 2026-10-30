package behavior.composite;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import behavior.Node;
import behavior.Status;

public class Deadline extends Parallel{

    private final Node deadline;
    private Status deadlineStatus;

    /**
     * Create deadline such that the parallel group runs until deadline is completed
     * @param deadline Node with priority (deadlined node)
     * @param children Nodes without priority (run at the same time)
     */
    public Deadline(Node deadline, Node... children) {
        super(ParallelPolicy.ALL_SUCCEED,
                Stream.concat(Stream.of(deadline),Arrays.stream(children)).toArray(Node[]::new));
        this.deadline = deadline;
        reset();
    }

    @Override
    public Status tick() {
        if (deadlineStatus == Status.RUNNING){
            deadlineStatus = deadline.tick();
        }
        if (deadlineStatus.isDone()){
            halt();
            return deadlineStatus;
        }
        return super.tick();
    }

    @Override
    public void halt() {
        deadline.halt();
        super.halt();
        reset();
    }

    @Override
    protected void reset() {
        deadlineStatus = Status.RUNNING;
        super.reset();
    }
}
