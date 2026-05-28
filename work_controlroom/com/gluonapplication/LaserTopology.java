package com.gluonapplication;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Static description of laser-propagation topology in the Virgo ITF, plus the
 * runtime propagation step.
 *
 * Nodes are tower IDs. Edges are physical light paths between adjacent towers;
 * each edge carries zero or more gating valves and is passable iff ALL its
 * valves are reported OPEN. Edges with zero valves are unconditionally open
 * (e.g. CryoLink* valves, which have optical viewports).
 *
 * SQZ branch (SQZDET1 ↔ SQZDET2 ↔ SQZ0N ↔ SQZTUBE ↔ SQZ300N, a.k.a. SQB1 →
 * SQB2 → FCIN → FCEND) is an isolated subgraph: it is *not* connected to DET,
 * so neither the IB YAG nor the WE/NE GREEN reach it. Both YAG and GREEN are
 * driven by an SQZ-local source at SQB1: GREEN is gated by the SHG lock,
 * YAG by the EQB1 fast shutter. All SQZ edges are zero-valve (every valve on
 * this path has an optical viewport), so each source signal propagates
 * uniformly to every SQZ tower indicator.
 */
public final class LaserTopology {

   /* Tower IDs */
   public static final String IB    = "IB";
   public static final String MC    = "MC";
   public static final String PR    = "PR";
   public static final String BS    = "BS";
   public static final String NI    = "NI";
   public static final String WI    = "WI";
   public static final String SR    = "SR";
   public static final String DET   = "DET";
   public static final String TUBEN = "TUBEN";
   public static final String TUBEW = "TUBEW";
   public static final String NE    = "NE";
   public static final String WE    = "WE";
   public static final String SQZDET1 = "SQZDET1";
   public static final String SQZDET2 = "SQZDET2";
   public static final String SQZ0N   = "SQZ0N";
   public static final String SQZTUBE = "SQZTUBE";
   public static final String SQZ300N = "SQZ300N";

   /* SQZ source channels (single-source-of-truth). Used by both
    * computeSqzGreenSourceState() / computeSqzYagSourceState() and the
    * dedicated CIRCLE_SQZ_LOCK_STATUS_COLOR / SHUTTER_SQZ_FAST_STATUS_COLOR
    * renderers. */
   private static final String CH_SQZ_SHG_LOCK     = "SQZ_SHG_Lock_Status_MAX";
   private static final String CH_SQZ_FAST_SHUTTER = "EQB1_FAST_SHUTTER_MONI_MAX";

   /* Valve channel names — single source of truth */
   private static final String V_CENTRAL_LI = "VAC_VALVECENTRAL_VLIST";
   private static final String V_CENTRAL_PS = "VAC_VALVECENTRAL_VPSST";
   private static final String V_CENTRAL_NS = "VAC_VALVECENTRAL_VNSST";
   private static final String V_CENTRAL_WS = "VAC_VALVECENTRAL_VWSST";
   private static final String V_CENTRAL_SS = "VAC_VALVECENTRAL_VSSST";
   private static final String V_CRYO_NI    = "VAC_CRYONI_VCRYOST";
   private static final String V_CRYO_WI    = "VAC_CRYOWI_VCRYOST";
   private static final String V_CRYO_NE    = "VAC_CRYONE_VCRYOST";
   private static final String V_CRYO_WE    = "VAC_CRYOWE_VCRYOST";
   private static final String V_BIG_NI     = "VAC_VALVEBIGNI_ST";
   private static final String V_BIG_WI     = "VAC_VALVEBIGWI_ST";
   private static final String V_BIG_NE     = "VAC_VALVEBIGNE_ST";
   private static final String V_BIG_WE     = "VAC_VALVEBIGWE_ST";

   public static final class Edge {
      public final String neighbor;
      public final String[] valves;
      Edge(String neighbor, String... valves) {
         this.neighbor = neighbor;
         this.valves = valves;
      }
   }

   public static final Map<String, List<Edge>> GRAPH;
   static {
      Map<String, List<Edge>> g = new HashMap<>();
      addEdge(g, IB,    MC,    V_CENTRAL_LI);
      addEdge(g, IB,    PR);                              /* CryoLinkIBVs1/Vs2 — transparent */
      addEdge(g, PR,    BS,    V_CENTRAL_PS);
      addEdge(g, BS,    NI,    V_CENTRAL_NS);
      addEdge(g, BS,    WI,    V_CENTRAL_WS);
      addEdge(g, BS,    SR,    V_CENTRAL_SS);
      addEdge(g, SR,    DET);                             /* CryoLinkDETVs1/Vs2 — transparent */
      addEdge(g, NI,    TUBEN, V_CRYO_NI, V_BIG_NI);
      addEdge(g, WI,    TUBEW, V_CRYO_WI, V_BIG_WI);
      addEdge(g, TUBEN, NE,    V_CRYO_NE, V_BIG_NE);
      addEdge(g, TUBEW, WE,    V_CRYO_WE, V_BIG_WE);
      /* SQZ branch: isolated subgraph (no DET edge). Every SQZ valve has an
       * optical viewport, so both YAG and GREEN propagate through the whole
       * branch regardless of valve state — modelled with zero-valve edges.
       * Source is at SQB1 (SQZDET1), driven by the SHG-lock + fast-shutter
       * signals; see compute*SqzSourceState() below. */
      addEdge(g, SQZDET1, SQZDET2);
      addEdge(g, SQZDET2, SQZ0N);
      addEdge(g, SQZ0N,   SQZTUBE);
      addEdge(g, SQZTUBE, SQZ300N);

      Map<String, List<Edge>> immutable = new HashMap<>();
      for (Map.Entry<String, List<Edge>> e : g.entrySet()) {
         immutable.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
      }
      GRAPH = Collections.unmodifiableMap(immutable);
   }

   private static void addEdge(Map<String, List<Edge>> g, String a, String b, String... valves) {
      g.computeIfAbsent(a, k -> new ArrayList<>()).add(new Edge(b, valves));
      g.computeIfAbsent(b, k -> new ArrayList<>()).add(new Edge(a, valves));
   }

   /* Source-tower mappings. SQZDET1 (SQB1) is the entry point of the isolated
    * SQZ subgraph for both YAG and GREEN. */
   public static final List<String> YAG_SOURCE_TOWERS   = Collections.unmodifiableList(Arrays.asList(IB, SQZDET1));
   public static final List<String> GREEN_SOURCE_TOWERS = Collections.unmodifiableList(Arrays.asList(WE, NE, SQZDET1));

   /* fx:id of a Yag/Green tower indicator -> its tower. Any indicator NOT in
    * the map is left untouched by propagation (e.g. WE/NE MiniGreen, which are
    * direct sensors). */
   public static final Map<String, String> YAG_INDICATORS;
   public static final Map<String, String> GREEN_INDICATORS;
   static {
      Map<String, String> ym = new LinkedHashMap<>();
      ym.put("IBYag",      IB);
      ym.put("IBMiniYag",  IB);
      ym.put("PRYag",      PR);
      ym.put("PRMiniYag",  PR);
      ym.put("BSYag",      BS);
      ym.put("NIYag",      NI);
      ym.put("WIYag",      WI);
      ym.put("SRYag",      SR);
      ym.put("DETYag",     DET);
      ym.put("DETMiniYag", DET);
      ym.put("MCYag",      MC);
      ym.put("TUBENYag",   TUBEN);
      ym.put("TUBEWYag",   TUBEW);
      ym.put("WEYag",      WE);
      ym.put("WEMiniYag",  WE);
      ym.put("NEYag",      NE);
      ym.put("NEMiniYag",  NE);
      ym.put("SQZDET1Yag", SQZDET1);
      ym.put("SQZDET2Yag", SQZDET2);
      ym.put("SQZ0NYag",   SQZ0N);
      ym.put("SQZTUBEYag", SQZTUBE);
      ym.put("SQZ300NYag", SQZ300N);
      YAG_INDICATORS = Collections.unmodifiableMap(ym);

      Map<String, String> gm = new LinkedHashMap<>();
      gm.put("IBGreen",      IB);
      gm.put("IBMiniGreen",  IB);
      gm.put("PRGreen",      PR);
      gm.put("PRMiniGreen",  PR);
      gm.put("BSGreen",      BS);
      gm.put("NIGreen",      NI);
      gm.put("WIGreen",      WI);
      gm.put("SRGreen",      SR);
      gm.put("DETGreen",     DET);
      gm.put("DETMiniGreen", DET);
      gm.put("TUBENGreen",   TUBEN);
      gm.put("TUBEWGreen",   TUBEW);
      gm.put("WEGreen",      WE);
      gm.put("WEMiniGreen",  WE);
      gm.put("NEGreen",      NE);
      gm.put("NEMiniGreen",  NE);
      gm.put("SQZDET1Green", SQZDET1);
      gm.put("SQZDET2Green", SQZDET2);
      gm.put("SQZ0NGreen",   SQZ0N);
      gm.put("SQZTUBEGreen", SQZTUBE);
      gm.put("SQZ300NGreen", SQZ300N);
      GREEN_INDICATORS = Collections.unmodifiableMap(gm);
   }

   public enum Tri { OFF, ON, UNKNOWN }

   /**
    * BFS reachability from the ON sources. For each tower:
    *   ON      — some ON source has a path of fully-OPEN valves to it,
    *   UNKNOWN — no such path, but some ON or UNKNOWN source has a path
    *             without any CLOSED valve (i.e. only OPEN+unknown valves),
    *   OFF     — every path from every non-OFF source has a CLOSED valve.
    */
   public static Map<String, Tri> reach(Map<String, Tri> sourceStates,
                                        Function<String, String> valveState) {
      Set<String> strictReach = bfs(sourceStates, /*strictSources=*/true,  valveState, /*strictValves=*/true);
      Set<String> looseReach  = bfs(sourceStates, /*strictSources=*/false, valveState, /*strictValves=*/false);

      Map<String, Tri> out = new HashMap<>();
      for (String tower : GRAPH.keySet()) {
         if (strictReach.contains(tower))     out.put(tower, Tri.ON);
         else if (looseReach.contains(tower)) out.put(tower, Tri.UNKNOWN);
         else                                 out.put(tower, Tri.OFF);
      }
      return out;
   }

   private static Set<String> bfs(Map<String, Tri> sourceStates,
                                  boolean strictSources,
                                  Function<String, String> valveState,
                                  boolean strictValves) {
      Set<String> visited = new HashSet<>();
      Deque<String> q = new ArrayDeque<>();
      for (Map.Entry<String, Tri> e : sourceStates.entrySet()) {
         boolean ok = strictSources ? (e.getValue() == Tri.ON) : (e.getValue() != Tri.OFF);
         if (ok && GRAPH.containsKey(e.getKey()) && visited.add(e.getKey())) {
            q.add(e.getKey());
         }
      }
      while (!q.isEmpty()) {
         String cur = q.removeFirst();
         for (Edge edge : GRAPH.getOrDefault(cur, Collections.emptyList())) {
            if (visited.contains(edge.neighbor)) continue;
            if (canTraverse(edge.valves, valveState, strictValves)) {
               visited.add(edge.neighbor);
               q.add(edge.neighbor);
            }
         }
      }
      return visited;
   }

   private static boolean canTraverse(String[] valves,
                                      Function<String, String> valveState,
                                      boolean strictValves) {
      for (String v : valves) {
         String s = valveState.apply(v);
         if ("1".equals(s)) continue;        /* open: passes either mode */
         if ("0".equals(s)) return false;    /* closed: never passes */
         if (strictValves)  return false;    /* strict: unknown is rejected */
         /* loose: unknown is tolerated */
      }
      return true;
   }

   /* ---------- Integration: compute & inject propagation into a DataSet ---------- */

   /**
    * Compute Yag/Green source states from the raw channels in the DataSet,
    * run propagation, and overwrite the svrValueList slots for any Yag/Green
    * tower-indicator entries with "0"/"1"/"---". Called from ViewData after
    * the server fetch, before the per-element render loop.
    */
   public static void applyPropagation(DataSet data) {
      Tri yagSrc       = computeYagSourceState(data);
      Tri greenWeSrc   = computeGreenSourceState(data, "WESourceGreen");
      Tri greenNeSrc   = computeGreenSourceState(data, "NESourceGreen");
      Tri sqzYagSrc    = computeSqzYagSourceState(data);
      Tri sqzGreenSrc  = computeSqzGreenSourceState(data);

      Function<String, String> valveLookup = chan -> {
         for (int i = 0; i < data.list.size(); i++) {
            if (chan.equals(data.list.elementAt(i).svrName)) {
               String raw = data.svrValueList.elementAt(i).replace(" ", "").replace(",", ".");
               if (raw.contains("NOTEXIST") || raw.contains("TIMOUT")) return "?";
               return raw;
            }
         }
         return "?";  /* channel absent from this panel's DataSet */
      };

      Map<String, Tri> yagSources = new HashMap<>();
      yagSources.put(IB,      yagSrc);
      yagSources.put(SQZDET1, sqzYagSrc);
      Map<String, Tri> yagReach = reach(yagSources, valveLookup);

      Map<String, Tri> greenSources = new HashMap<>();
      greenSources.put(WE,      greenWeSrc);
      greenSources.put(NE,      greenNeSrc);
      greenSources.put(SQZDET1, sqzGreenSrc);
      Map<String, Tri> greenReach = reach(greenSources, valveLookup);

      // logIfChanged(yagSrc, greenWeSrc, greenNeSrc, sqzYagSrc, sqzGreenSrc, yagReach, greenReach, valveLookup);

      writeReachIntoSvrValueList(data, YAG_INDICATORS, yagReach);
      writeReachIntoSvrValueList(data, GREEN_INDICATORS, greenReach);
   }

   /* Print one line per state transition (avoids per-frame log spam). */
   private static String lastLogLine = "";
   private static void logIfChanged(Tri yag, Tri gWe, Tri gNe,
                                    Tri sqzYag, Tri sqzGreen,
                                    Map<String, Tri> yagReach,
                                    Map<String, Tri> greenReach,
                                    java.util.function.Function<String, String> valveLookup) {
      StringBuilder sb = new StringBuilder();
      sb.append("[LaserProp] srcYag=").append(yag)
        .append(" srcGreenWE=").append(gWe)
        .append(" srcGreenNE=").append(gNe)
        .append(" srcSqzYag=").append(sqzYag)
        .append(" srcSqzGreen=").append(sqzGreen);
      sb.append(" yag{");
      for (Map.Entry<String, Tri> e : yagReach.entrySet()) {
         sb.append(e.getKey()).append('=').append(triShort(e.getValue())).append(' ');
      }
      sb.append("} green{");
      for (Map.Entry<String, Tri> e : greenReach.entrySet()) {
         sb.append(e.getKey()).append('=').append(triShort(e.getValue())).append(' ');
      }
      sb.append('}');
      String line = sb.toString();
      if (!line.equals(lastLogLine)) {
         lastLogLine = line;
         System.out.println(line);
         /* Also dump the raw channels driving the source decisions. */
         System.out.println("  INJ_EIB_POUT_PD_MAX=" + valveLookup.apply("INJ_EIB_POUT_PD_MAX")
                          + " BsX_QF_DC_MAX=" + valveLookup.apply("BsX_QF_DC_MAX")
                          + " BsX_QN_DC_MAX=" + valveLookup.apply("BsX_QN_DC_MAX"));
         System.out.println("  SQZ_SHG_Lock_Status_MAX=" + valveLookup.apply("SQZ_SHG_Lock_Status_MAX")
                          + " EQB1_FAST_SHUTTER_MONI_MAX=" + valveLookup.apply("EQB1_FAST_SHUTTER_MONI_MAX"));
      }
   }
   private static String triShort(Tri t) {
      return t == Tri.ON ? "1" : t == Tri.OFF ? "0" : "?";
   }

   private static void writeReachIntoSvrValueList(DataSet data,
                                                  Map<String, String> indicators,
                                                  Map<String, Tri> reach) {
      for (int i = 0; i < data.list.size(); i++) {
         DataElement de = data.list.elementAt(i);
         String tower = indicators.get(de.name);
         if (tower == null) continue;
         Tri state = reach.get(tower);
         if (state == null) continue;
         String s;
         switch (state) {
            case ON:  s = "1";   break;
            case OFF: s = "0";   break;
            default:  s = "---"; break;
         }
         data.svrValueList.setElementAt(s, i);
      }
   }

   /** Yag source state — single source of truth for both the SourceYag
    *  circle rendering and the laser propagation BFS. Adjust threshold/sign
    *  HERE only; the ViewData case handler delegates to this method. */
   public static Tri computeYagSourceState(DataSet data) {
      double sum = 0.0;
      int known = 0;
      boolean anyUnknown = false;
      String[] channels = {"INJ_EIB_POUT_PD_MAX", "BsX_QF_DC_MAX", "BsX_QN_DC_MAX"};
      for (String chan : channels) {
         String raw = findRaw(data, chan);
         if (raw == null) { anyUnknown = true; continue; }
         try { sum += Double.parseDouble(raw); known++; }
         catch (NumberFormatException ex) { anyUnknown = true; }
      }
      if (known == 0 || anyUnknown) return Tri.UNKNOWN;
      /* spec is `sum >= 0.1 V => ON`; flip back here when done testing. */
      return sum < 0.1 ? Tri.OFF : Tri.ON;
   }

   /** Convenience: map Tri to the "0"/"1"/"---" string the renderer expects. */
   public static String triToColorKey(Tri t) {
      if (t == Tri.ON)  return "1";
      if (t == Tri.OFF) return "0";
      return "---";
   }

   /** Green source state by fx:id — single source of truth for both the
    *  SourceGreen circle rendering and the laser propagation BFS. Walks every
    *  entry in the DataSet whose `name` matches `fxIdName` and inspects the
    *  shared channels: one is the calibrated photodiode (svrName contains
    *  "PD_GREEN_MONI_CALI"), the other is the release flag. ON iff PD > 1 AND
    *  REL < 0.5; OFF otherwise; UNKNOWN if either channel is missing/garbled. */
   public static Tri computeGreenSourceState(DataSet data, String fxIdName) {
      Double pdValue = null;
      Double relValue = null;
      boolean anyUnknown = false;
      for (int k = 0; k < data.list.size(); k++) {
         DataElement de = data.list.elementAt(k);
         if (!de.name.equals(fxIdName)) continue;
         String raw = data.svrValueList.elementAt(k).replace(" ", "").replace(",", ".");
         if (raw.contains("NOTEXIST") || raw.contains("TIMOUT")) { anyUnknown = true; continue; }
         try {
            double v = Double.parseDouble(raw);
            if (de.svrName.contains("PD_GREEN_MONI_CALI")) pdValue = v;
            else relValue = v;
         }
         catch (NumberFormatException ex) { anyUnknown = true; }
      }
      if (pdValue == null || relValue == null || anyUnknown) return Tri.UNKNOWN;
      return (pdValue >= 1.0 && relValue < 0.5) ? Tri.ON : Tri.OFF;
   }

   /** CO2 source state by fx:id — single source of truth. Walks every entry
    *  whose `name` matches `fxIdName` and treats the source as ON if any of
    *  the shared channels reads above the OFF-baseline threshold. Both
    *  CH_PWRLAS (mW) and PWRLAS (V) drop to ~0 when the laser is off, so
    *  one threshold serves both. */
   public static Tri computeCo2SourceState(DataSet data, String fxIdName) {
      boolean anyOn = false;
      boolean anyKnown = false;
      boolean anyUnknown = false;
      for (int k = 0; k < data.list.size(); k++) {
         DataElement de = data.list.elementAt(k);
         if (!de.name.equals(fxIdName)) continue;
         String raw = data.svrValueList.elementAt(k).replace(" ", "").replace(",", ".");
         if (raw.contains("NOTEXIST") || raw.contains("TIMOUT")) { anyUnknown = true; continue; }
         try {
            double v = Double.parseDouble(raw);
            anyKnown = true;
            if (v > CO2_ON_THRESHOLD) anyOn = true;
         }
         catch (NumberFormatException ex) { anyUnknown = true; }
      }
      if (!anyKnown) return Tri.UNKNOWN;
      return anyOn ? Tri.ON : Tri.OFF;
   }

   /** Test-time threshold for CO2 source detection. Original spec used 0.1 V /
    *  ~10 mW; raised here for the current bench setup. Adjust HERE only — both
    *  the SourceCO2 circle and the propagation BFS read this value. */
   private static final double CO2_ON_THRESHOLD = 20.0;

   /** SQZ Green source state at SQB1: ON iff the SHG is locked (lock < 5).
    *  Matches CIRCLE_SQZ_LOCK_STATUS_COLOR. The fast shutter does not gate
    *  the GREEN propagation in this chain — see computeSqzYagSourceState. */
   public static Tri computeSqzGreenSourceState(DataSet data) {
      String lockRaw = findRaw(data, CH_SQZ_SHG_LOCK);
      if (lockRaw == null) return Tri.UNKNOWN;
      try {
         return Double.parseDouble(lockRaw) < 5.0 ? Tri.ON : Tri.OFF;
      }
      catch (NumberFormatException ex) { return Tri.UNKNOWN; }
   }

   /** SQZ Yag source state at SQB1: ON iff the EQB1 fast shutter is OPEN
    *  (shutter <= 1). Matches SHUTTER_SQZ_FAST_STATUS_COLOR. */
   public static Tri computeSqzYagSourceState(DataSet data) {
      String shutterRaw = findRaw(data, CH_SQZ_FAST_SHUTTER);
      if (shutterRaw == null) return Tri.UNKNOWN;
      try {
         return Double.parseDouble(shutterRaw) <= 1.0 ? Tri.ON : Tri.OFF;
      }
      catch (NumberFormatException ex) { return Tri.UNKNOWN; }
   }

   private static String findRaw(DataSet data, String svrName) {
      for (int i = 0; i < data.list.size(); i++) {
         if (svrName.equals(data.list.elementAt(i).svrName)) {
            String raw = data.svrValueList.elementAt(i).replace(" ", "").replace(",", ".");
            if (raw.contains("NOTEXIST") || raw.contains("TIMOUT")) return null;
            return raw;
         }
      }
      return null;
   }

   private LaserTopology() {}
}
