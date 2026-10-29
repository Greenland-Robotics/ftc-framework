Put your own subsystems here, in the `robot.subsystems` package.

Most mechanisms don't need one: an [sdk part](../../../../sdk/README.md#parts) (`Lift`, `Arm`, `Roller`, `ServoPart`, `Flywheel`)
registered in [`Robot.java`](../Robot.java) does the job. Write a subsystem here to:
- **wrap parts in the game's words**, e.g. a `Scorer` with a lift and a claw that offers `scoreBasket()`;
- **build a mechanism from scratch** when no part fits, like [`examples/ftc/Intake.java`](../../../../examples/ftc/Intake.java).

See [Adding your own](../../../../sdk/README.md#adding-your-own), including when to promote one into the sdk, and
[`examples/ftc/seasons/`](../../../../examples/ftc/seasons/) for real ones.
