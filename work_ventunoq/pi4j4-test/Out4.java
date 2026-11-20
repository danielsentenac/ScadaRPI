import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;

public class Out4 {
    public static void main(String[] a) throws Exception {
        Context pi4j = Pi4J.newAutoContext();
        int line = Integer.parseInt(a[0]);
        DigitalOutputConfigBuilder b = DigitalOutput.newConfigBuilder(pi4j);
        b.bus(2); b.bcm(line);
        DigitalOutput out = pi4j.create(b.build());
        for (int i = 0; i < 3; i++) {
            out.low();  Thread.sleep(300); System.out.println("set LOW  -> state " + out.state());
            out.high(); Thread.sleep(300); System.out.println("set HIGH -> state " + out.state());
        }
        out.low(); System.out.println("left LOW");
        pi4j.shutdown();
    }
}
