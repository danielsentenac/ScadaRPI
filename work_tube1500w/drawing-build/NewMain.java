import com.genlogic.*; import java.util.*;
public class NewMain {
  static GlgObject d, arr; static List<GlgObject> o = new ArrayList<GlgObject>();
  static List<Object[]> fits = new ArrayList<Object[]>(), places = new ArrayList<Object[]>();
  static GlgObject named(String n) { for (GlgObject c : o) if (n.equals(c.GetSResource("Name"))) return c; throw new RuntimeException("no " + n); }
  static void fit(GlgObject x, double x1, double y1, double x2, double y2) { fits.add(new Object[]{x, x1, y1, x2, y2}); }
  static void place(GlgObject x, double px, double py) { places.add(new Object[]{x, px, py}); }
  static void retag(GlgObject c, String from, String to) {
    GlgObject tags = c.CreateTagList(false); if (tags == null) return;
    for (int i = 0; i < tags.GetSize(); i++) { GlgObject t = (GlgObject) tags.GetElement(i);
      t.SetSResource("TagSource", t.GetSResource("TagSource").replace(from, to)); t.SetSResource("TagName", t.GetSResource("TagSource")); } }
  static GlgObject pipeT;
  static void pipe(double x1, double y1, double x2, double y2) { GlgObject p = pipeT.CopyObject(); arr.AddObjectAt(p, 1); fit(p, x1, y1, x2, y2); }
  static void vpipe(double x, double y1, double y2) { pipe(x - 5, y1, x + 5, y2); }
  static void hpipe(double y, double x1, double x2) { pipe(x1, y - 5, x2, y + 5); }
  static GlgObject textT;
  static GlgObject text(String s, double x, double y) { GlgObject t = textT.CopyObject(); t.SetSResource("String", s); arr.AddObjectToBottom(t); place(t, x, y); return t; }
  static GlgObject boxT;
  static void box(double x1, double y1, double x2, double y2) { GlgObject b = boxT.CopyObject(); arr.AddObjectToBottom(b); fit(b, x1, y1, x2, y2); }
  static GlgObject circT;
  static void circle(double cx, double cy, double r, String label) { GlgObject c = circT.CopyObject(); retag(c, "Ga4Col", "PassiveCol"); c.SetSResource("Name", "");
    arr.AddObjectToBottom(c); fit(c, cx - r, cy - r, cx + r, cy + r); text(label, cx - 4.8 * label.length() - 2, cy - 10); }
  static GlgObject poly(double[][] pts) { GlgPolygon p = new GlgPolygon(pts.length, null);
    for (int i = 0; i < pts.length; i++) ((GlgObject) p.GetElement(i)).SetGResource(null, pts[i][0], pts[i][1], 0.);
    p.SetDResource("FillType", (double) GlgObject.FILL_EDGE); p.SetDResource("OpenType", (double) GlgObject.CLOSED); p.SetGResource("FillColor", 0.62, 0.73, 0.93); p.SetGResource("EdgeColor", 0.2, 0.2, 0.2);
    arr.AddObjectToBottom(p); return p; }
  // manual valve: bow-tie plus a T handle; vertical = on a vertical pipe (handle to the right), else handle on top
  static void manualValve(double cx, double cy, boolean vertical, String label) {
    double w = 17, h = 17;
    if (vertical) { fit(poly(new double[][]{{-1,1},{1,1},{0,0}}), cx - w, cy - h, cx + w, cy); fit(poly(new double[][]{{-1,-1},{1,-1},{0,0}}), cx - w, cy, cx + w, cy + h);
      fit(poly(new double[][]{{0,0},{1,0},{1,1},{0,1}}), cx, cy - 1.5, cx + 26, cy + 1.5); fit(poly(new double[][]{{0,0},{1,0},{1,1},{0,1}}), cx + 26, cy - 11, cx + 29, cy + 11);
      text(label, cx + 36, cy - 10); }
    else { fit(poly(new double[][]{{-1,1},{-1,-1},{0,0}}), cx - w, cy - h, cx, cy + h); fit(poly(new double[][]{{1,1},{1,-1},{0,0}}), cx, cy - h, cx + w, cy + h);
      fit(poly(new double[][]{{0,0},{1,0},{1,1},{0,1}}), cx - 1.5, cy - 26, cx + 1.5, cy); fit(poly(new double[][]{{0,0},{1,0},{1,1},{0,1}}), cx - 11, cy - 29, cx + 11, cy - 26);
      text(label, cx - 17, cy + 21); } }
  static void sicemBlock(int ch, double x, double y) {
    text("CH" + ch + " SET:", x, y + 3); text("CH" + ch + " T:", x + 16, y + 32);
    GlgObject sp = gauge("Setpoint7Val", "Setpoint" + ch + "Val"); fit(sp, x + 82, y, x + 186, y + 26);
    GlgObject t = gauge("Temp7Val", "Temp" + ch + "Val"); fit(t, x + 82, y + 29, x + 186, y + 55); }
  static GlgObject gauge(String from, String name) { GlgObject g = named(from).CopyObject(); g.SetSResource("Name", name); retag(g, from, name); arr.AddObjectToBottom(g); return g; }

  public static void main(String[] a) throws Exception {
    GlgObject.Init(); d = GlgObject.LoadWidget(a[0], GlgObject.FILE); arr = d.GetResourceObject("Array");
    for (int i = 0; i < arr.GetSize(); i++) o.add((GlgObject) arr.GetElement(i));
    pipeT = o.get(1).CopyObject(); textT = o.get(10).CopyObject(); boxT = o.get(92).CopyObject();
    circT = ((GlgObject) named("Ga4").GetElement(0)).CopyObject();
    // ---- remove: old pipes, V23, Ga2 (XGS), bypass indicator, per-column SET/T labels (replaced)
    int[] del = {1,4,5,6,7,8,9,31,33,36,38,39,104,106, 16,25,40};
    for (int i : del) arr.DeleteObject(o.get(i));
    int[] setLab = {44,49,53,57,61,65,69}, tLab = {45,46,50,54,58,62,66};     // channels 7..1
    int[] setVal = {42,48,52,56,60,64,68}, tVal = {43,47,51,55,59,63,67};
    int[] tmLab = {79,77,75,73,71}, tmVal = {78,76,74,72,70};                 // T5m..T1m
    for (int i : tLab) arr.DeleteObject(o.get(i));
    // ---- tube
    double AX = 152;                                  // tube axis
    fit(o.get(2), 350, 61, 1250, 243);
    for (int k = 0; k < 5; k++) { double x = 420 + 165 * k; place(o.get(tmLab[k]), x, 85); fit(o.get(tmVal[k]), x + 42, 80, x + 146, 108); }
    for (int k = 0; k < 7; k++) { double x = 435 + 110 * k;
      o.get(setLab[k]).SetSResource("String", "CH" + (7 - k)); place(o.get(setLab[k]), x + 36, 122);
      fit(o.get(setVal[k]), x, 145, x + 104, 173); fit(o.get(tVal[k]), x, 177, x + 104, 205); }
    text("SET:", 392, 150); text("T:", 408, 182);
    // ---- left: ION - conductance 1 - NEG - Ga4 - VM3
    circle(40, AX, 35, "ION"); hpipe(AX, 75, 92);
    box(92, AX - 20, 182, AX + 20); text("COND. 1", 104, AX + 26); hpipe(AX, 110, 164);
    hpipe(AX, 182, 197); circle(232, AX, 35, "NEG"); hpipe(AX, 267, 298);
    vpipe(283, 106, AX); fit(named("Ga4"), 250, 60, 315, 106); place(o.get(18), 208, 72); fit(named("Ga4Val"), 78, 22, 246, 55);
    manualValve(315, AX, false, "VM3"); hpipe(AX, 332, 352);
    // ---- right: VM2 - Ga5 / RGA - G21
    hpipe(AX, 1248, 1263); manualValve(1280, AX, false, "VM2"); hpipe(AX, 1297, 1505);
    vpipe(1360, 106, AX); fit(named("ROTGa5"), 1328, 60, 1393, 106); place(o.get(108), 1286, 72); fit(named("ROTGa5Val"), 1262, 22, 1430, 55);
    vpipe(1360, AX, 182); fit(named("RGAGa3"), 1328, 182, 1393, 231); place(o.get(17), 1286, 196);
    vpipe(1500, 106, AX + 4); fit(named("G21"), 1468, 60, 1533, 106); place(o.get(15), 1540, 72); fit(named("G21Val"), 1432, 22, 1600, 55);
    // ---- V21 branch (turbo P21, V22, G22, dry pump P22)
    double X1 = 640;
    vpipe(X1, 241, 262); fit(named("V21"), X1 - 29, 260, X1 + 29, 298); place(o.get(10), X1 + 36, 270);
    vpipe(X1, 296, 322); fit(named("TurboP21"), X1 - 56, 320, X1 + 56, 412);
    fit(named("TurboP21SpeedVal"), X1 + 66, 338, X1 + 188, 366); fit(named("TurboP21PowerVal"), X1 + 66, 368, X1 + 188, 396);
    vpipe(X1, 410, 452); hpipe(440, X1 - 44, X1); fit(named("V23"), X1 - 100, 421, X1 - 42, 458); place(o.get(12), X1 - 85, 462);
    // V23 is a manual valve with limit switches: live symbol plus the T handle of the manual valves
    fit(poly(new double[][]{{0,0},{1,0},{1,1},{0,1}}), X1 - 72.5, 411, X1 - 69.5, 439); fit(poly(new double[][]{{0,0},{1,0},{1,1},{0,1}}), X1 - 82, 408, X1 - 60, 411);
    fit(named("V22"), X1 - 29, 450, X1 + 29, 488); place(o.get(13), X1 + 36, 460);
    vpipe(X1, 486, 542); hpipe(511, X1, X1 + 66); fit(named("G22"), X1 + 65, 487, X1 + 131, 535); place(o.get(14), X1 + 137, 501);
    fit(named("G22Val"), X1 + 65, 542, X1 + 233, 575); fit(named("DryP22"), X1 - 56, 540, X1 + 56, 632);
    place(o.get(90), X1 + 66, 598); place(named("BypassG22Val"), X1 + 150, 597);
    // ---- Sicem channels 8 (heater around VM3) and 9 (heater around V21)
    sicemBlock(8, 150, 202); sicemBlock(9, X1 + 92, 264);
    // ---- V1 branch (H2O vapour line): V1 - VM1 - conductance 2 - G4 / H2O+G5 - PT - VS - PS
    double X2 = 1140;
    vpipe(X2, 241, 262); fit(named("V1"), X2 - 29, 260, X2 + 29, 298); place(o.get(11), X2 - 55, 270);
    vpipe(X2, 296, 312); manualValve(X2, 327, true, "VM1"); vpipe(X2, 343, 358);
    box(X2 - 30, 356, X2 + 30, 426); text("COND. 2", X2 + 36, 381); vpipe(X2, 368, 414);
    vpipe(X2, 426, 464); hpipe(450, X2 - 62, X2);
    GlgObject g4 = gauge("Ga4", "G4"); fit(g4, X2 - 126, 427, X2 - 61, 473); text("G4", X2 - 103, 476);
    GlgObject g4v = gauge("Ga4Val", "G4Val"); fit(g4v, X2 - 300, 434, X2 - 132, 467);
    hpipe(450, X2, 1292);
    box(1290, 422, 1420, 482); text("H2O", 1338, 442);
    vpipe(1355, 482, 506); GlgObject g5 = gauge("G22", "G5"); fit(g5, 1322, 505, 1388, 553); text("G5", 1393, 518);
    GlgObject g5v = gauge("Ga4Val", "G5Val"); fit(g5v, 1262, 556, 1430, 589);
    GlgObject pt = named("TurboP21").CopyObject(); pt.SetSResource("Name", ""); retag(pt, "TurboP21Col", "PassiveCol");
    ((GlgObject) pt.GetElement(7)).SetSResource("String", "PT"); System.out.println("pt visibility was " + pt.GetDResource("Visibility")); pt.SetDResource("Visibility", 1.); arr.AddObjectToBottom(pt); fit(pt, X2 - 56, 462, X2 + 56, 554);
    vpipe(X2, 552, 568); manualValve(X2, 582, true, "VS"); vpipe(X2, 598, 616);
    GlgObject ps = named("DryP22").CopyObject(); ps.SetSResource("Name", ""); retag(ps, "DryP22Col", "PassiveCol");
    ((GlgObject) ps.GetElement(3)).SetSResource("String", "PS"); System.out.println("ps visibility was " + ps.GetDResource("Visibility")); ps.SetDResource("Visibility", 1.); arr.AddObjectToBottom(ps); fit(ps, X2 - 48, 614, X2 + 48, 693);
    // ---- ramp box to the lower left, Sicem box to the right under the RGA group
    
    fit(o.get(88), 1285, 250, 1597, 420);
    place(o.get(87), 1345, 263); place(named("SICEMSTVal"), 1510, 263);
    place(named("CH1_ON"), 1297, 305); place(named("CH1_OFF"), 1415, 305); place(named("CH1STVal"), 1543, 313);
    place(named("CH2_ON"), 1297, 360); place(named("CH2_OFF"), 1415, 360); place(named("CH2STVal"), 1543, 368);

    // ---- button opening the AI assistant window (copy of the TEMPERATURES button)
    GlgObject ai = named("TCMuxTemp").CopyObject(); ai.SetSResource("Name", "Assistant");
    ai.SetSResource("Label/String", "AI ASSISTANT"); arr.AddObjectToBottom(ai);
    d.SetupHierarchy();
    for (Object[] f : fits) { GlgCube c = new GlgCube(); c.p1 = new GlgPoint((Double) f[1], (Double) f[2], 0.); c.p2 = new GlgPoint((Double) f[3], (Double) f[4], 0.);
      ((GlgObject) f[0]).FitObject(GlgObject.SCREEN_COORD, c, false); }
    for (Object[] p : places) { GlgObject x = (GlgObject) p[0]; GlgCube b = x.GetBox();
      x.MoveObjectBy(GlgObject.SCREEN_COORD, (Double) p[1] - b.p1.x, (Double) p[2] - b.p1.y, 0.); }
    for (int i = 92; i <= 103; i++) o.get(i).MoveObjectBy(GlgObject.SCREEN_COORD, 15 - 647, 262 - 371, 0.);
    named("TCMuxTemp").MoveObjectBy(GlgObject.SCREEN_COORD, 8., 0., 0.); named("TubeStatusDetails").MoveObjectBy(GlgObject.SCREEN_COORD, 8., 0., 0.);
    { GlgCube b = ai.GetBox(); ai.MoveObjectBy(GlgObject.SCREEN_COORD, 195 - b.p1.x, 0., 0.); }
    d.SetGTag("PassiveCol", 0.62, 0.73, 0.93, true);
    d.Update(); d.ResetHierarchy();
    System.out.println("saved=" + d.SaveObject(a[1])); System.exit(0);
  }
}
