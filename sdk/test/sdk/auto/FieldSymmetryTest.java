package sdk.auto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FieldSymmetryTest {
    @Test
    void mirrorFlipsAcrossTheMiddleLine() {
        FieldSymmetry mirror = FieldSymmetry.MIRROR;
        assertEquals(124, mirror.x(20), 1e-9);
        assertEquals(30, mirror.y(30), 1e-9);
        assertEquals(Math.PI, Math.abs(mirror.heading(0)), 1e-9);            // Facing +x becomes facing -x
        assertEquals(Math.PI / 2, mirror.heading(Math.PI / 2), 1e-9);        // Facing up stays up
    }

    @Test
    void rotateTurnsAboutTheCenter() {
        FieldSymmetry rotate = FieldSymmetry.ROTATE;
        assertEquals(128, rotate.x(16), 1e-9);
        assertEquals(124, rotate.y(20), 1e-9);
        assertEquals(-Math.PI / 2, rotate.heading(Math.PI / 2), 1e-9);
    }

    @Test
    void flippingTwiceGivesTheSamePose() {
        for (FieldSymmetry symmetry : FieldSymmetry.values()) {
            assertEquals(37, symmetry.x(symmetry.x(37)), 1e-9);
            assertEquals(81, symmetry.y(symmetry.y(81)), 1e-9);
            assertEquals(0.7, symmetry.heading(symmetry.heading(0.7)), 1e-9);
        }
    }
}
