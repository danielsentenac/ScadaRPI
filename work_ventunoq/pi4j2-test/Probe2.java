import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.library.gpiod.internal.GpioChipOpener;

public class Probe2 {
    public static void main(String[] a) throws Exception {
        Context pi4j = Pi4J.newAutoContext();
        System.out.println("CHIP: " + GpioChipOpener.open("f100000.pinctrl"));
        for (String s : a) {
            int line = Integer.parseInt(s);
            DigitalInput in = pi4j.create(DigitalInput.newConfigBuilder(pi4j).id("in" + line).address(line).provider("gpiod-digital-input").build());
            System.out.println("GPIO line " + line + " reads " + in.state());
        }
        pi4j.shutdown();
    }
}
