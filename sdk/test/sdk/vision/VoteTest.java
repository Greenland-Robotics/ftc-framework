package sdk.vision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VoteTest {
    @Test
    void unknownUntilEnoughFramesAgree() {
        Vote vote = new Vote(5, 3);
        vote.add(Randomization.LEFT);
        vote.add(Randomization.LEFT);
        assertEquals(Randomization.UNKNOWN, vote.result());
        vote.add(Randomization.LEFT);
        assertEquals(Randomization.LEFT, vote.result());
    }

    @Test
    void oneBadFrameDoesNotChangeTheAnswer() {
        Vote vote = new Vote(5, 3);
        for (int i = 0; i < 4; i++) vote.add(Randomization.RIGHT);
        vote.add(Randomization.CENTER);
        vote.add(null);
        assertEquals(Randomization.RIGHT, vote.result());
    }

    @Test
    void oldFramesDropOut() {
        Vote vote = new Vote(3, 2);
        vote.add(Randomization.LEFT);
        vote.add(Randomization.LEFT);
        vote.add(Randomization.CENTER);
        vote.add(Randomization.CENTER);
        assertEquals(Randomization.CENTER, vote.result());
    }

    @Test
    void orElseFillsInUnknown() {
        assertEquals(Randomization.CENTER, Randomization.UNKNOWN.orElse(Randomization.CENTER));
        assertEquals(Randomization.LEFT, Randomization.LEFT.orElse(Randomization.CENTER));
    }
}
