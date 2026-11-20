import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;

public class Probe {
    public static void main(String[] a) throws Exception {
        Context pi4j = Pi4J.newAutoContext();
        System.out.println("BOARD: " + pi4j.boardInfo().getBoardModel() + " / OS " + pi4j.boardInfo().getOperatingSystem() + " / Java " + pi4j.boardInfo().getJavaInfo());
        System.out.println("PLATFORM: " + pi4j.platform());
        pi4j.providers().all().forEach((k, v) -> System.out.println("PROVIDER: " + k + " -> " + v.getClass().getName()));
        if (a.length > 0) {
            int line = Integer.parseInt(a[0]);
            try {
                DigitalInput in = pi4j.create(DigitalInput.newConfigBuilder(pi4j).id("probe-in").address(line).provider("gpiod-digital-input").build());
                System.out.println("GPIO line " + line + " reads " + in.state());
            } catch (Throwable t) { System.out.println("GPIO INPUT FAILED: " + t); t.printStackTrace(System.out); }
        }
        pi4j.shutdown();
    }
}
