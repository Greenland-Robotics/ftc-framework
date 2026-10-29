package sdk.auto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutoMenuTest {
    private enum Start { NEAR, FAR }

    private final boolean[] up = {false};
    private final boolean[] down = {false};
    private final boolean[] left = {false};
    private final boolean[] right = {false};
    private final AutoMenu menu = new AutoMenu(() -> up[0], () -> down[0], () -> left[0], () -> right[0]);

    private void press(boolean[] button) {
        button[0] = true;
        menu.update();
        button[0] = false;
        menu.update();
    }

    @Test
    void dpadChoosesALineAndChangesIt() {
        AutoMenu.Choice<Alliance> alliance = menu.choice("Alliance", Alliance.values());
        AutoMenu.Choice<Start> start = menu.choice("Start", Start.values());
        AutoMenu.Number delay = menu.number("Delay", 0, 5, 1);

        press(right);                          // Alliance: RED -> BLUE
        assertEquals(Alliance.BLUE, alliance.get());
        press(down);
        press(left);                           // Start wraps round: NEAR -> FAR
        assertEquals(Start.FAR, start.get());
        press(down);
        press(left);                           // Delay can't go below 0
        assertEquals(0, delay.get());
        press(right);
        press(right);
        assertEquals(2, delay.get());
        press(down);                           // Wraps to the top
        press(right);
        assertEquals(Alliance.RED, alliance.get());
    }

    @Test
    void holdingAButtonCountsOnce() {
        AutoMenu.Number delay = menu.number("Delay", 0, 10, 1);
        right[0] = true;
        for (int i = 0; i < 5; i++) menu.update();
        assertEquals(1, delay.get());
    }

    @Test
    void lockingStopsChanges() {
        AutoMenu.Choice<Alliance> alliance = menu.choice("Alliance", Alliance.values());
        menu.lock();
        press(right);
        assertEquals(Alliance.RED, alliance.get());
        assertTrue(menu.show().get(0).contains("locked"));
    }

    @Test
    void showsTheSelectedLine() {
        menu.choice("Alliance", Alliance.values());
        menu.choice("Start", Start.values()).startAt(Start.FAR);
        assertEquals("> Alliance: ◀ RED ▶", menu.show().get(1));
        assertEquals("  Start: ◀ FAR ▶", menu.show().get(2));
    }
}
