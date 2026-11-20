import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;

public class Probe4 {
    public static void main(String[] a) throws Exception {
        Context pi4j = Pi4J.newAutoContext();
        System.out.println("BOARD: " + pi4j.boardInfo().getBoardModel());
        pi4j.providers().all().forEach((k, v) -> System.out.println("PROVIDER: " + k));
        for (String s : a) {
            int line = Integer.parseInt(s);
            DigitalInputConfigBuilder b = DigitalInput.newConfigBuilder(pi4j);
            b.bus(2);
            b.bcm(line);
            DigitalInput in = pi4j.create(b.build());
            System.out.println("gpiochip2 line " + line + " reads " + in.state());
        }
        pi4j.shutdown();
    }
}
