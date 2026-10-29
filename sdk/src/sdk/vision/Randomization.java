package sdk.vision;

/**
 * The three-way answer almost every season hides on the field at the start of the Auto:
 * FREIGHT FRENZY's barcode, POWERPLAY's signal sleeve, CENTERSTAGE's team prop, DECODE's obelisk
 * motif (21 = GPP, 22 = PGP, 23 = PPG). Map whatever the camera sees onto LEFT, CENTER and RIGHT,
 * and give the Auto a fallback for UNKNOWN.
 */
public enum Randomization {
    LEFT, CENTER, RIGHT, UNKNOWN;

    /** This, or {@code fallback} if the camera never decided. */
    public Randomization orElse(Randomization fallback) {
        return this == UNKNOWN ? fallback : this;
    }
}
