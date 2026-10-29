package robot.subsystems;

import static behavior.Behaviors.fail;
import static behavior.Behaviors.ifElse;
import static behavior.Behaviors.sequence;

import java.util.function.Supplier;

import behavior.Node;
import sdk.auto.Alliance;
import sdk.ftc.Hardware;
import sdk.hardware.PieceColor;
import sdk.parts.Lift;
import sdk.parts.Roller;
import subsystem.Subsystem;

/**
 * The extendo (a horizontal slide, so an sdk {@link Lift}) with a roller intake on the end and a
 * color sensor inside. Its job in game words: "get me a sample we're allowed to score".
 */
public class SampleIntake implements Subsystem {
    public static final double IN = 0;
    public static final double OUT = 1100;

    public final Lift extendo;
    public final Roller roller;
    private final Supplier<PieceColor> color;
    private final Supplier<Alliance> alliance;
    private PieceColor seen = PieceColor.NONE;

    public SampleIntake(Hardware hw, Supplier<Alliance> alliance) {
        this.alliance = alliance;
        extendo = new Lift(hw.motor("extendo")).limits(IN, OUT).tolerance(30);
        color = hw.color("intakeColor");
        roller = new Roller(hw.crServo("intake")).sensor(() -> seen != PieceColor.NONE);
    }

    /** A yellow sample, or one in our alliance's color. */
    public boolean holdsOurs() {
        PieceColor ours = alliance.get() == Alliance.RED ? PieceColor.RED : PieceColor.BLUE;
        return seen == PieceColor.YELLOW || seen == ours;
    }

    /**
     * Reaches out, spins in until something is inside, then: ours? pull it in. Theirs? spit it
     * out and fail, so a {@code .retry(3)} around this tries again.
     */
    public Node grab() {
        return sequence(
                extendo.goTo(OUT),
                roller.collect().withTimeout(3000),
                ifElse(this::holdsOurs,
                        extendo.goTo(IN),
                        sequence(roller.eject(400), fail()))
        ).named("Grab a sample we can score");
    }

    @Override
    public void initPeriodic() {
        extendo.initPeriodic();
    }

    @Override
    public void periodic() {
        seen = color.get();
        extendo.periodic();
        roller.periodic();
    }
}
