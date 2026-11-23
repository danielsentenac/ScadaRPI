import com.genlogic.*; import java.nio.file.*;
public class Check { public static void main(String[] a) throws Exception {
  GlgObject.Init(); GlgObject d = GlgObject.LoadWidget(a[0], GlgObject.FILE); d.SetupHierarchy();
  for (String k : Files.readAllLines(Paths.get(a[1]))) { if (k.contains("sub")) continue;
    boolean ok;
    if (k.contains("Col")) ok = d.SetGTag(k, 0., 1., 0., true);
    else if (k.contains("Val")) ok = d.SetDTag(k, 1.0, true);
    else ok = d.SetDResource(k + "/Visibility", 1.);
    System.out.println((ok ? "ok   " : "FAIL ") + k); }
  System.exit(0); } }
