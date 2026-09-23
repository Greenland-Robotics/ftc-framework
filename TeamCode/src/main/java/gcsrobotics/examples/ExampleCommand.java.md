```java  
package gcsrobotics.commands;

import gcsrobotics.control.OpModeBase;
import gcsrobotics.vertices.Command;

public class ExampleCommand implements Command {

    // When accessing things like motors, use robot.<thing> to use it
    // example: robot.motor.setPower(1);
    private OpModeBase robot = OpModeBase.INSTANCE;

    public void init(){
        // Code to run at the start of command running
    }

    public void loop(){
        // Code to run while the command is running
    }

    public boolean isFinished(){
        return true; // replace `true` with the condition for when the command should end
    }
}
```