package sdk.hardware;

/**
 * The colors game pieces have come in, season after season: red/blue alliance pieces and yellow
 * neutral ones (INTO THE DEEP, CENTERSTAGE's pixels), green and purple (DECODE's artifacts).
 * {@link #fromRgb} names what a color sensor sees; {@code ftc.Hardware.color(...)} uses it.
 */
public enum PieceColor {
    NONE, RED, YELLOW, GREEN, BLUE, PURPLE;

    /**
     * The closest piece color to an RGB reading (any scale, e.g. a color sensor's normalized
     * 0..1 values), or NONE when it is too dim or too grey to be a piece. The hue bands are a
     * starting point: check them against your sensor and pieces, and adjust here if needed.
     */
    public static PieceColor fromRgb(double red, double green, double blue) {
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        if (max <= 0) return NONE;
        double saturation = (max - min) / max;
        if (saturation < 0.25) return NONE;

        double hue;
        double range = max - min;
        if (max == red) {
            hue = 60 * (((green - blue) / range) % 6);
        } else if (max == green) {
            hue = 60 * ((blue - red) / range + 2);
        } else {
            hue = 60 * ((red - green) / range + 4);
        }
        if (hue < 0) hue += 360;

        if (hue < 25 || hue >= 330) return RED;
        if (hue < 75) return YELLOW;
        if (hue < 170) return GREEN;
        if (hue < 250) return BLUE;
        return PURPLE;
    }
}
