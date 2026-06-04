/*
 * controlroom.js — polls jchv and pushes values into the SVG/HTML panels.
 *
 * Protocol (see JChvServlet7.java doPost / handleJsonRequest, lines ~1935-2050):
 *   POST /jchv/jchv  Content-Type: application/json
 *   request body  : {"channels":["A","B",...]}
 *   response body : {"values":["v1","v2",...,"<gps>"]}     -- one extra GPS appended
 *   missing value : "---"
 *
 * Bindings derived from work_controlroom/com/gluonapplication/DataSet*Safety.java
 * and DataSetSensorO2.java. Rendering rules mirror ViewData.java + SidePopupViewData.java
 * (O2 / Rack / OKFailure cases) + DataTypes.java (color tables) + LaserTopology.java
 * (BFS propagation).
 *
 * Element refs are stored on each binding object after init (rather than as DOM ids)
 * so that an fx:id can appear on more than one SVG circle (e.g. TUBENYag exists on
 * both the NE and CB panels) and both instances update.
 */

(function () {
  "use strict";

  // ---------------------------------------------------------------------------
  // Endpoint
  // ---------------------------------------------------------------------------
  const JCHV_URL = (window.JCHV_URL || "/jchv/jchv");
  const POLL_MS = 1000;

  // ---------------------------------------------------------------------------
  // Status colour tables (mirror DataTypes.java)
  // ---------------------------------------------------------------------------
  const VALVE_COLOR = {
    "0": "#f1da21", "1": "#ff9933", "2": "#33ff57",
    "3": "#ff3339", "4": "#ff3339", "5": "#ff3d39", "6": "#ff4739", "7": "#ff5139",
    "255": "#7c7c7c", "---": "#7c7c7c", "...": "#7c7c7c"
  };
  const VALVE2_COLOR = {
    "0": "#f1da21", "1": "#33ff57", "2": "#ff9933",
    "255": "#7c7c7c", "---": "#7c7c7c", "...": "#7c7c7c"
  };
  const LASER_COLOR = {              // yag / localctrl / sqzfast / propagated yag
    "0": "#4e514e", "1": "#ff1f1f",
    "255": "#000000", "---": "#000000", "...": "#000000"
  };
  const GREEN_COLOR = {              // green dots / shutters / sqz-lock / propagated green
    "0": "#4e514e", "1": "#21ff27",
    "255": "#000000", "---": "#000000", "...": "#000000"
  };
  const CO2_COLOR = {                // CO2 dots / shutters
    "0": "#4e514e", "1": "#efff21",
    "255": "#000000", "---": "#000000", "...": "#000000"
  };
  const PCAL_COLOR = {               // PCAL (1047 Hz) dots — DataTypes.PCAL_STATUS_COLOR
    "0": "#4e514e", "1": "#ff7e21",
    "255": "#000000", "---": "#000000", "...": "#000000"
  };
  const O2SENSOR_COLOR = {           // CIRCLE_O2SENSOR_STATUS_COLOR (post bit-decode)
    "0": "lime", "1": "yellow", "2": "orange", "3": "red",
    "4": "brown", "5": "pink", "6": "lightgrey",
    "255": "lightgrey", "---": "lightgrey", "...": "lightgrey"
  };
  const RACK_STRING = {
    "0": "OK", "-1": "COM ERROR", "-2": "COM ERROR",
    "255": "---", "---": "---", "...": "..."
  };
  // RACK_2_STATUS_COLOR — the variant SidePopupViewData uses for the O2 controller labels.
  // "lime" was too flashy against the dark panel; use a softer Material-style green.
  const RACK_BG = {
    "0": "#4caf50", "-1": "orange", "-2": "red",
    "255": "lightgrey", "---": "lightgrey", "...": "lightgrey"
  };
  const OKFAIL_STRING = {
    "0": "OK", "1": "FAILURE",
    "255": "---", "---": "---", "...": "..."
  };
  // OKFAILURE_STATUS_COLOR_2 — the variant used for the alarm labels.
  const OKFAIL_BG = {
    "0": "#4caf50", "1": "red",
    "255": "lightgrey", "---": "lightgrey", "...": "lightgrey"
  };

  // ===========================================================================
  // LaserTopology — port of work_controlroom/com/gluonapplication/LaserTopology.java
  // ===========================================================================
  const TOWER = {
    IB: "IB", MC: "MC", PR: "PR", BS: "BS", NI: "NI", WI: "WI", SR: "SR", DET: "DET",
    TUBEN: "TUBEN", TUBEW: "TUBEW", NE: "NE", WE: "WE",
    SQZDET1: "SQZDET1", SQZDET2: "SQZDET2", SQZ0N: "SQZ0N",
    SQZTUBE: "SQZTUBE", SQZ300N: "SQZ300N"
  };

  // Edges: [tower-a, tower-b, [gating-valve-channels]]
  // Zero-valve edges (IB-PR via CryoLink, SR-DET via CryoLink, all SQZ edges) are
  // unconditionally passable per LaserTopology.java comments.
  const EDGES = [
    [TOWER.IB,      TOWER.MC,      ["VAC_VALVECENTRAL_VLIST"]],
    [TOWER.IB,      TOWER.PR,      []],
    [TOWER.PR,      TOWER.BS,      ["VAC_VALVECENTRAL_VPSST"]],
    [TOWER.BS,      TOWER.NI,      ["VAC_VALVECENTRAL_VNSST"]],
    [TOWER.BS,      TOWER.WI,      ["VAC_VALVECENTRAL_VWSST"]],
    [TOWER.BS,      TOWER.SR,      ["VAC_VALVECENTRAL_VSSST"]],
    [TOWER.SR,      TOWER.DET,     []],
    [TOWER.NI,      TOWER.TUBEN,   ["VAC_CRYONI_VCRYOST", "VAC_VALVEBIGNI_ST"]],
    [TOWER.WI,      TOWER.TUBEW,   ["VAC_CRYOWI_VCRYOST", "VAC_VALVEBIGWI_ST"]],
    [TOWER.TUBEN,   TOWER.NE,      ["VAC_CRYONE_VCRYOST", "VAC_VALVEBIGNE_ST"]],
    [TOWER.TUBEW,   TOWER.WE,      ["VAC_CRYOWE_VCRYOST", "VAC_VALVEBIGWE_ST"]],
    [TOWER.SQZDET1, TOWER.SQZDET2, []],
    [TOWER.SQZDET2, TOWER.SQZ0N,   []],
    [TOWER.SQZ0N,   TOWER.SQZTUBE, []],
    [TOWER.SQZTUBE, TOWER.SQZ300N, []],
  ];

  const GRAPH = {};
  EDGES.forEach(([a, b, valves]) => {
    (GRAPH[a] = GRAPH[a] || []).push({n: b, v: valves});
    (GRAPH[b] = GRAPH[b] || []).push({n: a, v: valves});
  });

  /**
   * Two-pass BFS reachability (mirror LaserTopology.reach).
   * sources       : {tower -> "ON"/"OFF"/"UNKNOWN"}
   * valveLookup   : (channel) -> "0"/"1"/"?" (closed/open/unknown)
   * returns       : {tower -> "ON"/"OFF"/"UNKNOWN"}
   */
  function reach(sources, valveLookup) {
    const strict = bfs(sources, valveLookup, true);
    const loose  = bfs(sources, valveLookup, false);
    const out = {};
    Object.keys(GRAPH).forEach(t => {
      if (strict.has(t))      out[t] = "ON";
      else if (loose.has(t))  out[t] = "UNKNOWN";
      else                    out[t] = "OFF";
    });
    return out;
  }

  function bfs(sources, valveLookup, strict) {
    const visited = new Set();
    const queue = [];
    Object.entries(sources).forEach(([tower, state]) => {
      const ok = strict ? (state === "ON") : (state !== "OFF");
      if (ok && GRAPH[tower] && !visited.has(tower)) {
        visited.add(tower);
        queue.push(tower);
      }
    });
    while (queue.length) {
      const cur = queue.shift();
      for (const edge of GRAPH[cur] || []) {
        if (visited.has(edge.n)) continue;
        if (canTraverse(edge.v, valveLookup, strict)) {
          visited.add(edge.n);
          queue.push(edge.n);
        }
      }
    }
    return visited;
  }

  function canTraverse(valveChannels, valveLookup, strict) {
    for (const ch of valveChannels) {
      const s = valveLookup(ch);
      if (s === "1") continue;       // open: passes either mode
      if (s === "0") return false;   // closed: never passes
      if (strict) return false;      // strict: unknown rejected
      // loose: unknown tolerated
    }
    return true;
  }

  // Source-state computations (mirror LaserTopology.compute*SourceState).
  function computeYagSource(cv) {
    const channels = ["INJ_EIB_POUT_PD_MAX", "BsX_QF_DC_MAX", "BsX_QN_DC_MAX"];
    let sum = 0, known = 0, anyUnknown = false;
    for (const ch of channels) {
      const f = asFloat(cv[ch]);
      if (f == null) { anyUnknown = true; continue; }
      sum += f; known++;
    }
    if (known === 0 || anyUnknown) return "UNKNOWN";
    return sum >= 0.1 ? "ON" : "OFF";
  }
  function computeGreenSource(cv, pdChannel, relChannel) {
    const pd  = asFloat(cv[pdChannel]);
    const rel = asFloat(cv[relChannel]);
    if (pd == null || rel == null) return "UNKNOWN";
    return (pd >= 1.0 && rel < 0.5) ? "ON" : "OFF";
  }
  // CO2 source: any channel sharing the fxId above CO2_ON_THRESHOLD => ON.
  // Threshold from LaserTopology.java (CO2_ON_THRESHOLD = 20.0).
  const CO2_ON_THRESHOLD = 20.0;
  function computeCo2Source(cv, channels) {
    let anyOn = false, anyKnown = false;
    for (const ch of channels) {
      const f = asFloat(cv[ch]);
      if (f == null) continue;
      anyKnown = true;
      if (f > CO2_ON_THRESHOLD) anyOn = true;
    }
    if (!anyKnown) return "UNKNOWN";
    return anyOn ? "ON" : "OFF";
  }
  function computeSqzGreenSource(cv) {
    const v = asFloat(cv["SQZ_SHG_Lock_Status_MAX"]);
    if (v == null) return "UNKNOWN";
    return v < 5.0 ? "ON" : "OFF";
  }
  function computeSqzYagSource(cv) {
    const v = asFloat(cv["EQB1_FAST_SHUTTER_MONI_MAX"]);
    if (v == null) return "UNKNOWN";
    return v <= 1.0 ? "ON" : "OFF";
  }

  function applyPropagation(cv) {
    const yagSources = {
      [TOWER.IB]:      computeYagSource(cv),
      [TOWER.SQZDET1]: computeSqzYagSource(cv),
    };
    const greenSources = {
      [TOWER.WE]:      computeGreenSource(cv, "ALS_WEB_PD_GREEN_MONI_CALI_MEAN", "ALS_WEB_REL1"),
      [TOWER.NE]:      computeGreenSource(cv, "ALS_NEB_PD_GREEN_MONI_CALI_MEAN", "ALS_NEB_REL1"),
      [TOWER.SQZDET1]: computeSqzGreenSource(cv),
    };
    const valveLookup = (ch) => {
      const raw = cv[ch];
      if (raw == null) return "?";
      const s = String(raw);
      if (s.includes("NOTEXIST") || s.includes("TIMOUT")) return "?";
      const cl = s.replace(/ /g, "").replace(/,/g, ".");
      if (cl === "0" || cl === "1") return cl;
      return "?";
    };
    return {
      yag:   reach(yagSources,   valveLookup),
      green: reach(greenSources, valveLookup),
      sourceYag:    yagSources[TOWER.IB],
      sourceWE:     greenSources[TOWER.WE],
      sourceNE:     greenSources[TOWER.NE],
      sourceSqzG:   greenSources[TOWER.SQZDET1],
      sourceSqzYag: yagSources[TOWER.SQZDET1],
    };
  }

  // ===========================================================================
  // SVG bindings
  // ===========================================================================
  // Locator forms (resolved into b._el on init):
  //   {type:"g",      prefix:"translate(X Y)"}   first <g> whose transform starts with that
  //   {type:"circle", cx, cy}                    first <circle cx="X" cy="Y" ...>
  const SVG_BINDINGS = [
    // ----- NE: direct-channel elements -----
    { fxId:"StatusValveBigNE",  kind:"valve",         channels:["VAC_VALVEBIGNE_ST"],
      locator:{type:"g", prefix:"translate(133 169)"} },
    { fxId:"StatusValveCryoNE", kind:"valve",         channels:["VAC_CRYONE_VCRYOST"],
      locator:{type:"g", prefix:"translate(132 110)"} },
    { fxId:"NELocalCtrl",       kind:"localctrl",     channels:["SAT_NE_F0_DC_ENBL","SAT_NE_F7_DC_ON"],
      locator:{type:"g", prefix:"translate(186 114)"} },
    { fxId:"NEGreenShutter",    kind:"shutter-green", channels:["ALS_NEB_REL1"],
      locator:{type:"g", prefix:"translate(211 71)"} },
    { fxId:"NESourceGreen",     kind:"source-green",
      channels:["ALS_NEB_PD_GREEN_MONI_CALI_MEAN","ALS_NEB_REL1"],
      locator:{type:"circle", cx:247, cy:132} },
    // PCAL (1047 Hz): tower circle mirrors the source — same channel on both.
    { fxId:"NESourcePCAL",      kind:"pcal",          channels:["PCAL_NE_laser_on_20kHz_50Hz_MAX"],
      locator:{type:"circle", cx:218, cy:32} },
    { fxId:"NEPCAL",            kind:"pcal",          channels:["PCAL_NE_laser_on_20kHz_50Hz_MAX"],
      locator:{type:"circle", cx:190, cy:71} },

    // ----- WE: direct-channel elements -----
    { fxId:"StatusValveBigWE",  kind:"valve",         channels:["VAC_VALVEBIGWE_ST"],
      locator:{type:"g", prefix:"translate(198 124)"} },
    { fxId:"StatusValveCryoWE", kind:"valve",         channels:["VAC_CRYOWE_VCRYOST"],
      locator:{type:"g", prefix:"translate(136 123)"} },
    { fxId:"WELocalCtrl",       kind:"localctrl",     channels:["SAT_WE_F0_DC_ENBL","SAT_WE_F7_DC_ON"],
      locator:{type:"g", prefix:"translate(140 180)"} },
    { fxId:"WEGreenShutter",    kind:"shutter-green", channels:["ALS_WEB_REL1"],
      locator:{type:"g", prefix:"translate(155 26)"} },
    { fxId:"WESourceGreen",     kind:"source-green",
      channels:["ALS_WEB_PD_GREEN_MONI_CALI_MEAN","ALS_WEB_REL1"],
      locator:{type:"circle", cx:192, cy:86} },
    // PCAL (1047 Hz): tower circle mirrors the source — same channel on both.
    { fxId:"WESourcePCAL",      kind:"pcal",          channels:["PCAL_WE_laser_on_20kHz_50Hz_MAX"],
      locator:{type:"circle", cx:51,  cy:115} },
    { fxId:"WEPCAL",            kind:"pcal",          channels:["PCAL_WE_laser_on_20kHz_50Hz_MAX"],
      locator:{type:"circle", cx:142, cy:135} },

    // ----- CB: valves (single-channel) -----
    { fxId:"StatusValveSqz300N",  kind:"valve", channels:["VAC_SQZ300N_VPST"],     locator:{type:"g", prefix:"translate(432 16)"}  },
    { fxId:"StatusValveSqz0N",    kind:"valve", channels:["VAC_SQZ0N_VPST"],       locator:{type:"g", prefix:"translate(432 122)"} },
    { fxId:"StatusValveSqzDet2",  kind:"valve", channels:["VAC_SQZDET2_VPST"],     locator:{type:"g", prefix:"translate(432 164)"} },
    { fxId:"StatusValveSqzDet1",  kind:"valve", channels:["VAC_SQZDET1_VPST"],     locator:{type:"g", prefix:"translate(432 207)"} },
    { fxId:"StatusValveBigNI",    kind:"valve", channels:["VAC_VALVEBIGNI_ST"],    locator:{type:"g", prefix:"translate(256 126)"} },
    { fxId:"StatusValveBigWI",    kind:"valve", channels:["VAC_VALVEBIGWI_ST"],    locator:{type:"g", prefix:"translate(115 263)"} },
    { fxId:"StatusValveCentralLI", kind:"valve", channels:["VAC_VALVECENTRAL_VLIST"], locator:{type:"g", prefix:"translate(223 440)"} },
    { fxId:"StatusValveCryoNI",   kind:"valve", channels:["VAC_CRYONI_VCRYOST"],   locator:{type:"g", prefix:"translate(256 166)"} },
    { fxId:"StatusValveCryoWI",   kind:"valve", channels:["VAC_CRYOWI_VCRYOST"],   locator:{type:"g", prefix:"translate(156 264)"} },
    { fxId:"StatusValveCentralNS", kind:"valve", channels:["VAC_VALVECENTRAL_VNSST"], locator:{type:"g", prefix:"translate(257 233)"} },
    { fxId:"StatusValveCentralWS", kind:"valve", channels:["VAC_VALVECENTRAL_VWSST"], locator:{type:"g", prefix:"translate(223 265)"} },
    { fxId:"StatusValveCentralPS", kind:"valve", channels:["VAC_VALVECENTRAL_VPSST"], locator:{type:"g", prefix:"translate(257 298)"} },
    { fxId:"StatusValveCentralSS", kind:"valve", channels:["VAC_VALVECENTRAL_VSSST"], locator:{type:"g", prefix:"translate(290 265)"} },

    // ----- CB: cryolink valves (color map 2) -----
    { fxId:"StatusValveCryoLinkDETVs1", kind:"valve2", channels:["VAC_CRYOLINKDET_Vs1"], locator:{type:"g", prefix:"translate(358 265)"} },
    { fxId:"StatusValveCryoLinkIBVs2",  kind:"valve2", channels:["VAC_CRYOLINKIB_Vs2"],  locator:{type:"g", prefix:"translate(257 365)"} },
    { fxId:"StatusValveCryoLinkIBVs1",  kind:"valve2", channels:["VAC_CRYOLINKIB_Vs1"],  locator:{type:"g", prefix:"translate(257 405)"} },
    { fxId:"StatusValveCryoLinkDETVs2", kind:"valve2", channels:["VAC_CRYOLINKDET_Vs2"], locator:{type:"g", prefix:"translate(398 265)"} },

    // ----- CB: LocalCtrl rectangles -----
    { fxId:"NILocalCtrl",  kind:"localctrl", channels:["SAT_NI_F0_DC_ENBL","SAT_NI_F7_DC_ON"], locator:{type:"g", prefix:"translate(302 245)"} },
    { fxId:"WILocalCtrl",  kind:"localctrl", channels:["SAT_WI_F0_DC_ENBL","SAT_WI_F7_DC_ON"], locator:{type:"g", prefix:"translate(236 308)"} },
    { fxId:"BSLocalCtrl",  kind:"localctrl", channels:["SAT_BS_F0_DC_ENBL","SAT_BS_F7_DC_ON"], locator:{type:"g", prefix:"translate(302 308)"} },
    { fxId:"PRLocalCtrl",  kind:"localctrl", channels:["SAT_PR_F0_DC_ENBL","SAT_PR_F7_DC_ON"], locator:{type:"g", prefix:"translate(302 377)"} },
    { fxId:"IBLocalCtrl",  kind:"localctrl", channels:["SAT_IB_F0_DC_ENBL"],                   locator:{type:"g", prefix:"translate(302 484)"} },
    { fxId:"MCLocalCtrl",  kind:"localctrl", channels:["SAT_MC_F0_DC_ENBL"],                   locator:{type:"g", prefix:"translate(145 484)"} },
    { fxId:"SRLocalCtrl",  kind:"localctrl", channels:["SAT_SR_F0_DC_ENBL","SAT_SR_F7_DC_ON"], locator:{type:"g", prefix:"translate(369 308)"} },
    { fxId:"DETLocalCtrl", kind:"localctrl", channels:["SAT_OB_F0_DC_ENBL"],                   locator:{type:"g", prefix:"translate(477 308)"} },

    // ----- CB: CO2 shutters -----
    { fxId:"NICO2Shutter", kind:"shutter-co2",
      channels:["TCS_CO2_REL5","TCS_CO2_REL6","TCS_CO2_REL7"],
      locator:{type:"g", prefix:"translate(182 140)"} },
    { fxId:"WICO2Shutter", kind:"shutter-co2",
      channels:["TCS_CO2_REL1","TCS_CO2_REL2","TCS_CO2_REL3"],
      locator:{type:"g", prefix:"translate(171 190)"} },

    // ----- CB: CO2 source circles (4 instances of 2 channel pairs) -----
    { fxId:"WISourceCO2", kind:"source-co2",
      channels:["TCS_WI_CO2_CH_PWRLAS_MEAN","TCS_WI_CO2_PWRLAS_MEAN"],
      locator:{type:"circle", cx:217, cy:251} },
    { fxId:"WICO2",       kind:"source-co2",
      channels:["TCS_WI_CO2_CH_PWRLAS_MEAN","TCS_WI_CO2_PWRLAS_MEAN"],
      locator:{type:"circle", cx:240, cy:283} },
    { fxId:"NISourceCO2", kind:"source-co2",
      channels:["TCS_NI_CO2_CH_PWRLAS_MEAN","TCS_NI_CO2_PWRLAS_MEAN"],
      locator:{type:"circle", cx:228, cy:201} },
    { fxId:"NICO2",       kind:"source-co2",
      channels:["TCS_NI_CO2_CH_PWRLAS_MEAN","TCS_NI_CO2_PWRLAS_MEAN"],
      locator:{type:"circle", cx:306, cy:218} },

    // ----- CB: Source Yag -----
    { fxId:"SourceYag", kind:"source-yag",
      channels:["INJ_EIB_POUT_PD_MAX","BsX_QF_DC_MAX","BsX_QN_DC_MAX"],
      locator:{type:"circle", cx:387, cy:473} },

    // ----- CB: SQZ branch (sources) -----
    { fxId:"SQZSourceGreen", kind:"sqz-lock",         channels:["SQZ_SHG_Lock_Status_MAX"],
      locator:{type:"circle", cx:560, cy:261} },
    { fxId:"SQZYagShutter",  kind:"shutter-sqz-fast", channels:["EQB1_FAST_SHUTTER_MONI_MAX"],
      locator:{type:"g", prefix:"translate(453 199)"} },

    // ----- NE panel: propagated chamber dots -----
    { fxId:"NEYag",        kind:"yag-prop",   tower:TOWER.NE,    locator:{type:"circle", cx:144, cy:70}  },
    { fxId:"TUBENYag",     kind:"yag-prop",   tower:TOWER.TUBEN, locator:{type:"circle", cx:155, cy:230} },
    { fxId:"NEGreen",      kind:"green-prop", tower:TOWER.NE,    locator:{type:"circle", cx:144, cy:118} },
    { fxId:"TUBENGreen",   kind:"green-prop", tower:TOWER.TUBEN, locator:{type:"circle", cx:181, cy:230} },
    { fxId:"NEMiniYag",    kind:"yag-prop",   tower:TOWER.NE,    locator:{type:"circle", cx:153, cy:43}  },
    { fxId:"NEMiniGreen",  kind:"green-prop", tower:TOWER.NE,    locator:{type:"circle", cx:176, cy:43}  },

    // ----- WE panel: propagated chamber dots -----
    { fxId:"WEYag",        kind:"yag-prop",   tower:TOWER.WE,    locator:{type:"circle", cx:99,  cy:136} },
    { fxId:"TUBEWYag",     kind:"yag-prop",   tower:TOWER.TUBEW, locator:{type:"circle", cx:261, cy:147} },
    { fxId:"WEGreen",      kind:"green-prop", tower:TOWER.WE,    locator:{type:"circle", cx:99,  cy:182} },
    { fxId:"TUBEWGreen",   kind:"green-prop", tower:TOWER.TUBEW, locator:{type:"circle", cx:261, cy:170} },
    { fxId:"WEMiniYag",    kind:"yag-prop",   tower:TOWER.WE,    locator:{type:"circle", cx:45,  cy:166} },
    { fxId:"WEMiniGreen",  kind:"green-prop", tower:TOWER.WE,    locator:{type:"circle", cx:69,  cy:167} },

    // ----- CB panel: propagated chamber dots (Yag, r=6) -----
    { fxId:"IBYag",      kind:"yag-prop", tower:TOWER.IB,    locator:{type:"circle", cx:276, cy:457} },
    { fxId:"PRYag",      kind:"yag-prop", tower:TOWER.PR,    locator:{type:"circle", cx:276, cy:350} },
    { fxId:"BSYag",      kind:"yag-prop", tower:TOWER.BS,    locator:{type:"circle", cx:276, cy:284} },
    { fxId:"NIYag",      kind:"yag-prop", tower:TOWER.NI,    locator:{type:"circle", cx:276, cy:218} },
    { fxId:"SRYag",      kind:"yag-prop", tower:TOWER.SR,    locator:{type:"circle", cx:344, cy:284} },
    { fxId:"WIYag",      kind:"yag-prop", tower:TOWER.WI,    locator:{type:"circle", cx:209, cy:283} },
    { fxId:"DETYag",     kind:"yag-prop", tower:TOWER.DET,   locator:{type:"circle", cx:451, cy:284} },
    { fxId:"MCYag",      kind:"yag-prop", tower:TOWER.MC,    locator:{type:"circle", cx:119, cy:460} },
    { fxId:"TUBENYag",   kind:"yag-prop", tower:TOWER.TUBEN, locator:{type:"circle", cx:284, cy:143} },
    { fxId:"TUBEWYag",   kind:"yag-prop", tower:TOWER.TUBEW, locator:{type:"circle", cx:129, cy:290} },
    { fxId:"IBMiniYag",  kind:"yag-prop", tower:TOWER.IB,    locator:{type:"circle", cx:327, cy:482} },
    { fxId:"DETMiniYag", kind:"yag-prop", tower:TOWER.DET,   locator:{type:"circle", cx:500, cy:307} },
    { fxId:"PRMiniYag",  kind:"yag-prop", tower:TOWER.PR,    locator:{type:"circle", cx:243, cy:352} },

    // ----- CB panel: propagated chamber dots (Green, r=6) -----
    { fxId:"IBGreen",      kind:"green-prop", tower:TOWER.IB,    locator:{type:"circle", cx:276, cy:489} },
    { fxId:"PRGreen",      kind:"green-prop", tower:TOWER.PR,    locator:{type:"circle", cx:275, cy:382} },
    { fxId:"BSGreen",      kind:"green-prop", tower:TOWER.BS,    locator:{type:"circle", cx:275, cy:317} },
    { fxId:"NIGreen",      kind:"green-prop", tower:TOWER.NI,    locator:{type:"circle", cx:275, cy:251} },
    { fxId:"WIGreen",      kind:"green-prop", tower:TOWER.WI,    locator:{type:"circle", cx:208, cy:316} },
    { fxId:"TUBEWGreen",   kind:"green-prop", tower:TOWER.TUBEW, locator:{type:"circle", cx:129, cy:305} },
    { fxId:"TUBENGreen",   kind:"green-prop", tower:TOWER.TUBEN, locator:{type:"circle", cx:300, cy:143} },
    { fxId:"SRGreen",      kind:"green-prop", tower:TOWER.SR,    locator:{type:"circle", cx:344, cy:317} },
    { fxId:"DETGreen",     kind:"green-prop", tower:TOWER.DET,   locator:{type:"circle", cx:451, cy:316} },
    { fxId:"DETMiniGreen", kind:"green-prop", tower:TOWER.DET,   locator:{type:"circle", cx:516, cy:307} },
    { fxId:"PRMiniGreen",  kind:"green-prop", tower:TOWER.PR,    locator:{type:"circle", cx:259, cy:353} },
    { fxId:"IBMiniGreen",  kind:"green-prop", tower:TOWER.IB,    locator:{type:"circle", cx:342, cy:482} },

    // ----- CB panel: SQZ branch propagated dots (Yag, r=4) -----
    { fxId:"SQZDET1Yag", kind:"yag-prop", tower:TOWER.SQZDET1, locator:{type:"circle", cx:460, cy:266} },
    { fxId:"SQZDET2Yag", kind:"yag-prop", tower:TOWER.SQZDET2, locator:{type:"circle", cx:460, cy:224} },
    { fxId:"SQZ0NYag",   kind:"yag-prop", tower:TOWER.SQZ0N,   locator:{type:"circle", cx:460, cy:181} },
    { fxId:"SQZ300NYag", kind:"yag-prop", tower:TOWER.SQZ300N, locator:{type:"circle", cx:459, cy:34}  },
    { fxId:"SQZTUBEYag", kind:"yag-prop", tower:TOWER.SQZTUBE, locator:{type:"circle", cx:460, cy:140} },

    // ----- CB panel: SQZ branch propagated dots (Green, r=4) -----
    { fxId:"SQZDET1Green", kind:"green-prop", tower:TOWER.SQZDET1, locator:{type:"circle", cx:475, cy:266} },
    { fxId:"SQZDET2Green", kind:"green-prop", tower:TOWER.SQZDET2, locator:{type:"circle", cx:475, cy:224} },
    { fxId:"SQZ0NGreen",   kind:"green-prop", tower:TOWER.SQZ0N,   locator:{type:"circle", cx:475, cy:181} },
    { fxId:"SQZ300NGreen", kind:"green-prop", tower:TOWER.SQZ300N, locator:{type:"circle", cx:474, cy:34}  },
    { fxId:"SQZTUBEGreen", kind:"green-prop", tower:TOWER.SQZTUBE, locator:{type:"circle", cx:474, cy:140} },
  ];

  // ---------------------------------------------------------------------------
  // O2 panel bindings (HTML, already has fx:id as the element id)
  // ---------------------------------------------------------------------------
  const O2_CONTROLLERS = [
    {id:"ControllerCB", channel:"VAC_CB_O2_RackStatus"},
    {id:"ControllerNE", channel:"VAC_NE_O2_RackStatus"},
    {id:"ControllerWE", channel:"VAC_WE_O2_RackStatus"},
  ];
  const O2_ALARMS = [
    {id:"AlarmCB", channel:"VAC_CB_O2_ALARM"},
    {id:"AlarmNE", channel:"VAC_NE_O2_ALARM"},
    {id:"AlarmWE", channel:"VAC_WE_O2_ALARM"},
  ];
  const O2_READINGS = [
    // CB
    ["CBZoneA60",    "VAC_CB_O2_ZONE_A_H60",    "VAC_CB_O2_ZONE_A_H60_ST"],
    ["CBZoneA120",   "VAC_CB_O2_ZONE_A_H120",   "VAC_CB_O2_ZONE_A_H120_ST"],
    ["CBZoneB120",   "VAC_CB_O2_ZONE_B_H120",   "VAC_CB_O2_ZONE_B_H120_ST"],
    ["CBZoneB170",   "VAC_CB_O2_ZONE_B_H170",   "VAC_CB_O2_ZONE_B_H170_ST"],
    ["CBZoneC60",    "VAC_CB_O2_ZONE_C_H60",    "VAC_CB_O2_ZONE_C_H60_ST"],
    ["CBZoneC120",   "VAC_CB_O2_ZONE_C_H120",   "VAC_CB_O2_ZONE_C_H120_ST"],
    ["CBZoneD120",   "VAC_CB_O2_ZONE_D_H120",   "VAC_CB_O2_ZONE_D_H120_ST"],
    ["CBZoneD170",   "VAC_CB_O2_ZONE_D_H170",   "VAC_CB_O2_ZONE_D_H170_ST"],
    ["CBZoneE60",    "VAC_CB_O2_ZONE_E_H60",    "VAC_CB_O2_ZONE_E_H60_ST"],
    ["CBZoneE120",   "VAC_CB_O2_ZONE_E_H120",   "VAC_CB_O2_ZONE_E_H120_ST"],
    ["CBZoneF60",    "VAC_CB_O2_ZONE_F_H60",    "VAC_CB_O2_ZONE_F_H60_ST"],
    ["CBZoneF120",   "VAC_CB_O2_ZONE_F_H120",   "VAC_CB_O2_ZONE_F_H120_ST"],
    ["CBZoneG60",    "VAC_CB_O2_ZONE_G_H60",    "VAC_CB_O2_ZONE_G_H60_ST"],
    ["CBZoneG120",   "VAC_CB_O2_ZONE_G_H120",   "VAC_CB_O2_ZONE_G_H120_ST"],
    ["CBCleanRoom60",  "VAC_CB_O2_CLEANROOM_DOWN", "VAC_CB_O2_CLEANROOM_DOWN_ST"],
    ["CBCleanRoom120", "VAC_CB_O2_CLEANROOM_UP",   "VAC_CB_O2_CLEANROOM_UP_ST"],
    // NE
    ["NETowerDx60",    "VAC_NE_O2_TOWER_DX_DOWN", "VAC_NE_O2_TOWER_DX_DOWN_ST"],
    ["NETowerDx120",   "VAC_NE_O2_TOWER_DX_UP",   "VAC_NE_O2_TOWER_DX_UP_ST"],
    ["NEBaseRoom60",   "VAC_NE_O2_BASEROOM_DOWN", "VAC_NE_O2_BASEROOM_DOWN_ST"],
    ["NEBaseRoom120",  "VAC_NE_O2_BASEROOM_UP",   "VAC_NE_O2_BASEROOM_UP_ST"],
    ["NECleanRoom60",  "VAC_NE_O2_CLEANROOM_DOWN","VAC_NE_O2_CLEANROOM_DOWN_ST"],
    ["NECleanRoom120", "VAC_NE_O2_CLEANROOM_UP",  "VAC_NE_O2_CLEANROOM_UP_ST"],
    ["NETowerSx60",    "VAC_NE_O2_TOWER_SX_DOWN", "VAC_NE_O2_TOWER_SX_DOWN_ST"],
    ["NETowerSx120",   "VAC_NE_O2_TOWER_SX_UP",   "VAC_NE_O2_TOWER_SX_UP_ST"],
    ["NETunnel60",     "VAC_NE_O2_TUNNEL_DOWN",   "VAC_NE_O2_TUNNEL_DOWN_ST"],
    ["NETunnel120",    "VAC_NE_O2_TUNNEL_UP",     "VAC_NE_O2_TUNNEL_UP_ST"],
    ["NETunnelDoor60", "VAC_NE_O2_TUNNELDOOR_DOWN","VAC_NE_O2_TUNNELDOOR_DOWN_ST"],
    ["NETunnelDoor120","VAC_NE_O2_TUNNELDOOR_UP", "VAC_NE_O2_TUNNELDOOR_UP_ST"],
    // WE
    ["WETowerDx60",    "VAC_WE_O2_TOWER_DX_DOWN", "VAC_WE_O2_TOWER_DX_DOWN_ST"],
    ["WETowerDx120",   "VAC_WE_O2_TOWER_DX_UP",   "VAC_WE_O2_TOWER_DX_UP_ST"],
    ["WEBaseRoom60",   "VAC_WE_O2_BASEROOM_DOWN", "VAC_WE_O2_BASEROOM_DOWN_ST"],
    ["WEBaseRoom120",  "VAC_WE_O2_BASEROOM_UP",   "VAC_WE_O2_BASEROOM_UP_ST"],
    ["WECleanRoom60",  "VAC_WE_O2_CLEANROOM_DOWN","VAC_WE_O2_CLEANROOM_DOWN_ST"],
    ["WECleanRoom120", "VAC_WE_O2_CLEANROOM_UP",  "VAC_WE_O2_CLEANROOM_UP_ST"],
    ["WETowerSx60",    "VAC_WE_O2_TOWER_SX_DOWN", "VAC_WE_O2_TOWER_SX_DOWN_ST"],
    ["WETowerSx120",   "VAC_WE_O2_TOWER_SX_UP",   "VAC_WE_O2_TOWER_SX_UP_ST"],
    ["WETunnel60",     "VAC_WE_O2_TUNNEL_DOWN",   "VAC_WE_O2_TUNNEL_DOWN_ST"],
    ["WETunnel120",    "VAC_WE_O2_TUNNEL_UP",     "VAC_WE_O2_TUNNEL_UP_ST"],
    ["WETunnelDoor60", "VAC_WE_O2_TUNNELDOOR_DOWN","VAC_WE_O2_TUNNELDOOR_DOWN_ST"],
    ["WETunnelDoor120","VAC_WE_O2_TUNNELDOOR_UP", "VAC_WE_O2_TUNNELDOOR_UP_ST"],
  ];

  // ---------------------------------------------------------------------------
  // Polled channel set (deduped); also includes channels needed only by the BFS.
  // ---------------------------------------------------------------------------
  function collectChannels() {
    const set = new Set();
    SVG_BINDINGS.forEach(b => (b.channels || []).forEach(c => set.add(c)));
    O2_CONTROLLERS.forEach(b => set.add(b.channel));
    O2_ALARMS.forEach(b => set.add(b.channel));
    O2_READINGS.forEach(r => { set.add(r[1]); set.add(r[2]); });
    // Propagation source channels (some are also direct-binding channels — Set dedupes):
    ["INJ_EIB_POUT_PD_MAX","BsX_QF_DC_MAX","BsX_QN_DC_MAX",
     "ALS_WEB_PD_GREEN_MONI_CALI_MEAN","ALS_WEB_REL1",
     "ALS_NEB_PD_GREEN_MONI_CALI_MEAN","ALS_NEB_REL1",
     "SQZ_SHG_Lock_Status_MAX","EQB1_FAST_SHUTTER_MONI_MAX"].forEach(c => set.add(c));
    // Valve channels referenced by edges (already in direct bindings, but make explicit):
    EDGES.forEach(([,,vs]) => vs.forEach(v => set.add(v)));
    return Array.from(set);
  }

  // Per-kind stroke colour: matches the FXML convention where each laser-type
  // circle keeps a fixed-colour outline (red for Yag, green for Green, yellow
  // for CO2) so the operator can identify the laser type even when the fill is
  // grey/black (OFF / no-data). The stroke is set once at init; only the fill
  // changes per poll.
  const KIND_STROKE = {
    "yag-prop":     "#ff1f1f",
    "green-prop":   "#21ff27",
    "source-yag":   "#ff1f1f",
    "source-green": "#21ff27",
    "source-co2":   "#efff21",
    "sqz-lock":     "#21ff27",
    "pcal":         "#ff7e21",
  };

  function resolveBindings() {
    SVG_BINDINGS.forEach(b => {
      let el = null;
      if (b.locator.type === "g") {
        el = document.querySelector(`g[transform^="${b.locator.prefix}"]`);
      } else if (b.locator.type === "circle") {
        el = document.querySelector(`circle[cx="${b.locator.cx}"][cy="${b.locator.cy}"]`);
      }
      if (el) {
        b._el = el;
        const strokeColor = KIND_STROKE[b.kind];
        if (strokeColor) el.setAttribute("stroke", strokeColor);
      }
      else console.warn("controlroom.js: locator did not match for", b.fxId, b.locator);
    });
  }

  // ---------------------------------------------------------------------------
  // Value helpers — mirror ViewData.java conventions.
  // ---------------------------------------------------------------------------
  function clean(v)     { return v == null ? "---" : String(v).replace(/ /g, "").replace(/,/g, "."); }
  function isUnknown(v) { if (v == null) return true; const s = String(v); return s.includes("NOTEXIST") || s.includes("TIMOUT") || s === "---" || s === "..."; }
  function asInt(v)     { if (isUnknown(v)) return null; const n = parseInt(clean(v), 10); return Number.isNaN(n) ? null : n; }
  function asFloat(v)   { if (isUnknown(v)) return null; const n = parseFloat(clean(v)); return Number.isNaN(n) ? null : n; }

  // Reduce a numeric channel value to its integer-string key (e.g. "1.000000" -> "1",
  // "-1.0" -> "-1"). Falls back to the cleaned string if not a whole number.
  function statusKey(raw) {
    if (raw == null) return "---";
    if (isUnknown(raw)) return "---";
    const f = parseFloat(clean(raw));
    if (Number.isNaN(f)) return clean(raw);
    return Number.isInteger(f) ? String(f) : clean(raw);
  }

  // OR aggregation across an array of channel values:
  // any !=0 -> "1"; all 0 -> "0"; otherwise "---" (unknown).
  function orState(values) {
    let anyOn = false, anyKnown = false;
    values.forEach(v => {
      const n = asInt(v);
      if (n == null) return;
      anyKnown = true;
      if (n !== 0) anyOn = true;
    });
    return anyOn ? "1" : (anyKnown ? "0" : "---");
  }

  // SidePopupViewData.java:522 — decode the 16-bit O2 sensor status word.
  // Priority (LATER override EARLIER in the Java cascade, so check in REVERSE):
  //   bit 8  -> "6" Off
  //   bits 10|11|14|15 -> "5" Reset
  //   bit 0  -> "4" brown
  //   bit 4  -> "3" red    (alarm 3)
  //   bit 3  -> "2" orange (alarm 2)
  //   bit 2  -> "1" yellow (alarm 1)
  //   bit 7  -> "0" lime   (OK)
  function decodeO2Status(raw) {
    if (isUnknown(raw)) return "---";
    const n = parseInt(clean(raw), 10);
    if (Number.isNaN(n)) return "---";
    if ((n >> 8) & 1) return "6";
    if (((n >> 10) & 1) || ((n >> 11) & 1) || ((n >> 14) & 1) || ((n >> 15) & 1)) return "5";
    if ((n >> 0) & 1) return "4";
    if ((n >> 4) & 1) return "3";
    if ((n >> 3) & 1) return "2";
    if ((n >> 2) & 1) return "1";
    if ((n >> 7) & 1) return "0";
    return "255";
  }

  // ---------------------------------------------------------------------------
  // Render
  // ---------------------------------------------------------------------------
  function setFill(el, color)   { if (el) el.setAttribute("fill", color); }
  function setStroke(el, color) { if (el) el.setAttribute("stroke", color); }

  function triToKey(state) {
    return state === "ON" ? "1" : state === "OFF" ? "0" : "---";
  }

  function renderSvg(b, values, propagation) {
    const el = b._el;
    if (!el) return;
    const raw0 = values[0];

    switch (b.kind) {
      case "valve":
        setFill(el, VALVE_COLOR[statusKey(raw0)] || VALVE_COLOR["---"]);
        break;
      case "valve2":
        setFill(el, VALVE2_COLOR[statusKey(raw0)] || VALVE2_COLOR["---"]);
        break;
      case "localctrl": {
        const rect = el.querySelector("rect");
        setFill(rect, LASER_COLOR[orState(values)]);
        break;
      }
      case "shutter-green": {
        // Single channel (ALS_NEB_REL1 / ALS_WEB_REL1) — interpreted opposite to the
        // CO2 shutters: REL == 0 means the safety release is engaged so the beam
        // passes (OPEN, green); REL != 0 means BLOCKED (CLOSED, grey).
        // This matches DataSetNESafety.java's comment that NESourceGreen is ON iff
        // PD > 1 AND REL == 0 (and the ViewData.java SHUTTER_GREEN handler has a
        // sign bug — `anyOn = false` on the non-zero branch — that masks the
        // shutter state in the live JavaFX app; not replicated here).
        const v = asInt(raw0);
        const st = (v == null) ? "---" : (v === 0 ? "1" : "0");
        setFill(el, GREEN_COLOR[st]);
        setStroke(el, st === "1" || st === "0" ? GREEN_COLOR[st] : "#21ff27");
        break;
      }
      case "shutter-co2": {
        const st = orState(values);
        setFill(el, CO2_COLOR[st]);
        setStroke(el, st === "1" || st === "0" ? CO2_COLOR[st] : "#efff21");
        break;
      }
      case "source-green": {
        // PD >= 1 AND REL < 0.5 (matches LaserTopology.computeGreenSourceState).
        const pd  = asFloat(values[0]);
        const rel = asFloat(values[1]);
        const st  = (pd == null || rel == null) ? "---"
                  : ((pd >= 1.0 && rel < 0.5) ? "1" : "0");
        setFill(el, GREEN_COLOR[st]);
        break;
      }
      case "source-co2": {
        // any channel > CO2_ON_THRESHOLD => ON.
        setFill(el, CO2_COLOR[triToKey(computeCo2Source(channelValueMap, b.channels))]);
        break;
      }
      case "source-yag": {
        // Always pull from propagation so the SourceYag circle agrees with the BFS.
        setFill(el, LASER_COLOR[triToKey(propagation.sourceYag)]);
        break;
      }
      case "pcal": {
        // PCAL (1047 Hz) on/off flag (_MAX aggregate). The channel may arrive as
        // "0"/"1" or as a float ("0.0"/"1.0"), so decide numerically like
        // ViewData's CIRCLE_PCAL_STATUS_COLOR case: non-zero => ON (orange),
        // zero => OFF (grey), non-numeric => no-data (black).
        const v = asFloat(raw0);
        const st = (v == null) ? "---" : (v !== 0 ? "1" : "0");
        setFill(el, PCAL_COLOR[st]);
        break;
      }
      case "sqz-lock": {
        // SHG locked => ON (green) iff value < 5.
        const v = asFloat(raw0);
        const st = (v == null) ? "---" : (v < 5.0 ? "1" : "0");
        setFill(el, GREEN_COLOR[st]);
        break;
      }
      case "shutter-sqz-fast": {
        // Fast shutter OPEN (red, hazard) iff value <= 1; otherwise CLOSED (grey).
        const v = asFloat(raw0);
        const st = (v == null) ? "---" : (v <= 1.0 ? "1" : "0");
        setFill(el, LASER_COLOR[st]);
        setStroke(el, st === "1" || st === "0" ? LASER_COLOR[st] : "#ff1f1f");
        break;
      }
      case "yag-prop":
        setFill(el, LASER_COLOR[triToKey(propagation.yag[b.tower] || "UNKNOWN")]);
        break;
      case "green-prop":
        setFill(el, GREEN_COLOR[triToKey(propagation.green[b.tower] || "UNKNOWN")]);
        break;
    }
  }

  // Channel map exposed to the case "source-co2" branch (needs reads beyond values[]).
  let channelValueMap = {};

  function renderO2(cv) {
    O2_CONTROLLERS.forEach(c => {
      const el = document.getElementById(c.id);
      if (!el) return;
      const k = statusKey(cv[c.channel]);
      const key = (k in RACK_STRING) ? k : "---";
      el.textContent = RACK_STRING[key];
      el.style.backgroundColor = RACK_BG[key];
    });
    O2_ALARMS.forEach(a => {
      const el = document.getElementById(a.id);
      if (!el) return;
      const k = statusKey(cv[a.channel]);
      const key = (k in OKFAIL_STRING) ? k : "---";
      el.textContent = OKFAIL_STRING[key];
      el.style.backgroundColor = OKFAIL_BG[key];
    });
    O2_READINGS.forEach(([id, vch, sch]) => {
      const elVal = document.getElementById(id);
      const elSt  = document.getElementById(id + "Status");
      if (elVal) {
        const f = asFloat(cv[vch]);
        elVal.textContent = (f == null) ? "---" : (f.toFixed(2) + " %O²");
      }
      if (elSt) {
        const key = decodeO2Status(cv[sch]);
        elSt.style.backgroundColor = O2SENSOR_COLOR[key] || O2SENSOR_COLOR["---"];
      }
    });
  }

  function setBanner(message) {
    const b = document.getElementById("connBanner");
    if (!b) return;
    if (message) { b.textContent = message; b.classList.add("show"); }
    else b.classList.remove("show");
  }
  function setGps(value) {
    const g = document.getElementById("gpsLabel");
    if (!g) return;
    g.textContent = "GPS: " + (isUnknown(value) ? "---" : clean(value));
  }

  // ---------------------------------------------------------------------------
  // Polling loop
  // ---------------------------------------------------------------------------
  const channelNames = collectChannels();

  async function pollOnce() {
    try {
      const resp = await fetch(JCHV_URL, {
        method: "POST",
        headers: {"Content-Type": "application/json", "Accept": "application/json"},
        credentials: "same-origin",
        body: JSON.stringify({channels: channelNames}),
      });
      if (!resp.ok) {
        setBanner("jchv HTTP " + resp.status);
        return;
      }
      const data = await resp.json();
      const values = data.values || [];
      const cv = {};
      channelNames.forEach((name, i) => { cv[name] = values[i]; });
      channelValueMap = cv;

      const propagation = applyPropagation(cv);

      // SVG bindings
      SVG_BINDINGS.forEach(b => {
        const vs = (b.channels || []).map(c => cv[c]);
        renderSvg(b, vs, propagation);
      });
      // O2 bindings
      renderO2(cv);
      // GPS (last element in values, appended by jchv)
      setGps(values.length ? values[values.length - 1] : null);

      setBanner(null);
    } catch (err) {
      console.warn("jchv poll failed:", err);
      setBanner("jchv unreachable");
    }
  }

  function start() {
    resolveBindings();
    pollOnce();
    setInterval(pollOnce, POLL_MS);
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
})();
