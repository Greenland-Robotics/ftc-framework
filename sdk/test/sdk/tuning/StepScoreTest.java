package sdk.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StepScoreTest {
    @Test
    void measuresOvershootAndSettling() {
        StepScore score = new StepScore(0, 100, 2, 0);
        score.sample(50, 100);
        score.sample(110, 200);   // 10% past
        score.sample(101, 300);   // Inside from here on
        score.sample(100, 500);
        assertFalse(score.isSettled());
        score.sample(100, 700);
        assertTrue(score.isSettled());
        assertEquals(300, score.settleMillis());
        assertEquals(0.1, score.overshoot(), 1e-9);
    }

    @Test
    void worksDownwardsToo() {
        StepScore score = new StepScore(100, 0, 2, 0);
        score.sample(-5, 100);
        assertEquals(0.05, score.overshoot(), 1e-9);
    }

    @Test
    void writesNumbersShortAndPlain() {
        assertEquals("0.00412", TuneReport.number(0.0041234));
        assertEquals("0.12", TuneReport.number(0.12));
        assertEquals("6000", TuneReport.number(6000));
        assertEquals("0.0001", TuneReport.number(0.0001));
        assertEquals("-0.5", TuneReport.number(-0.5));
        assertEquals("0", TuneReport.number(0));
    }
}
