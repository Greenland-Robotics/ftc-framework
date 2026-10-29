package sdk.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PieceColorTest {
    @Test
    void namesTheSeasonsColors() {
        assertEquals(PieceColor.RED, PieceColor.fromRgb(0.8, 0.1, 0.1));
        assertEquals(PieceColor.BLUE, PieceColor.fromRgb(0.1, 0.2, 0.8));
        assertEquals(PieceColor.YELLOW, PieceColor.fromRgb(0.8, 0.7, 0.1));
        assertEquals(PieceColor.GREEN, PieceColor.fromRgb(0.1, 0.7, 0.3));
        assertEquals(PieceColor.PURPLE, PieceColor.fromRgb(0.5, 0.2, 0.8));
    }

    @Test
    void greyOrDarkIsNothing() {
        assertEquals(PieceColor.NONE, PieceColor.fromRgb(0.4, 0.4, 0.42));
        assertEquals(PieceColor.NONE, PieceColor.fromRgb(0, 0, 0));
    }
}
