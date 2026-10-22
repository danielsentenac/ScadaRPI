/*
 * vacsupervisor.js — polls jchv and pushes values into the Vacuum Supervisor SVG.
 *
 * Protocol (same as work_controlroom/web/controlroom.js):
 *   POST /jchv/jchv  Content-Type: application/json
 *   request body  : {"channels":["A","B",...]}
 *   response body : {"values":["v1","v2",...,"<gps>"]}     -- one extra GPS appended
 *   missing value : "---"
 *
 * Bindings are DataSetGlobal.java (VACUUM_SUPERVISOR MainApp); rendering mirrors
 * ViewData.java (LABEL_VALUE_2E/2F_STRING, SVGPATH_VALVE(_2)_STATUS_COLOR,
 * RECTANGLE_FILL_LEVEL) and the DataTypes.java colour tables.
 * Each binding's id is the fx:id that fxml2svg.py copies onto the SVG element.
 */

(function () {
  "use strict";

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

  // ---------------------------------------------------------------------------
  // Bindings (DataSetGlobal.java)
  // ---------------------------------------------------------------------------
  // [fx:id, kind, channel, units | {min,max}]
  const BINDINGS = [
    // Status Virgo SQZ Valves
    ["StatusValveSqz300N", "valve", "VAC_SQZ300N_VPST"],
    ["StatusValveSqz0N",   "valve", "VAC_SQZ0N_VPST"],
    ["StatusValveSqzDet2", "valve", "VAC_SQZDET2_VPST"],
    ["StatusValveSqzDet1", "valve", "VAC_SQZDET1_VPST"],
    // Status Virgo big Valves
    ["StatusValveBigNI", "valve", "VAC_VALVEBIGNI_ST"],
    ["StatusValveBigNE", "valve", "VAC_VALVEBIGNE_ST"],
    ["StatusValveBigWI", "valve", "VAC_VALVEBIGWI_ST"],
    ["StatusValveBigWE", "valve", "VAC_VALVEBIGWE_ST"],
    // Status Virgo Central Valves
    ["StatusValveCentralNS", "valve", "VAC_VALVECENTRAL_VNSST"],
    ["StatusValveCentralSS", "valve", "VAC_VALVECENTRAL_VSSST"],
    ["StatusValveCentralWS", "valve", "VAC_VALVECENTRAL_VWSST"],
    ["StatusValveCentralPS", "valve", "VAC_VALVECENTRAL_VPSST"],
    ["StatusValveCentralLI", "valve", "VAC_VALVECENTRAL_VLIST"],
    // Status CryoTrap Valves
    ["StatusValveCryoNI", "valve", "VAC_CRYONI_VCRYOST"],
    ["StatusValveCryoWI", "valve", "VAC_CRYOWI_VCRYOST"],
    ["StatusValveCryoNE", "valve", "VAC_CRYONE_VCRYOST"],
    ["StatusValveCryoWE", "valve", "VAC_CRYOWE_VCRYOST"],
    // Status CryoLink Valves
    ["StatusValveCryoLinkDETVs1", "valve2", "VAC_CRYOLINKDET_Vs1"],
    ["StatusValveCryoLinkDETVs2", "valve2", "VAC_CRYOLINKDET_Vs2"],
    ["StatusValveCryoLinkIBVs1",  "valve2", "VAC_CRYOLINKIB_Vs1"],
    ["StatusValveCryoLinkIBVs2",  "valve2", "VAC_CRYOLINKIB_Vs2"],
    // Pressures Tube Averages & G51 Det/IB & BS & NE & WE & Remote G3
    ["PressureTubeNorth", "2e", "VAC_AVERAGE_PR_TUBE_NORTH", "mbar"],
    ["PressureTubeWest",  "2e", "VAC_AVERAGE_PR_TUBE_WEST", "mbar"],
    ["PressureDetG51",    "2e", "VAC_TOWERDET_MG1_CH4", "mbar"],
    ["PressureMCG51",     "2e", "VAC_TOWERMC_MG1_CH4", "mbar"],
    ["PressureIBG51",     "2e", "VAC_TOWERIB_MG1_CH4", "mbar"],
    ["PressureBSGd2",     "2e", "VAC_TOWERBS_MG2_CH2", "mbar"],
    ["PressureNEGd2",     "2e", "VAC_TOWERNE_MG2_CH2", "mbar"],
    ["PressureWEGd2",     "2e", "VAC_TOWERWE_MG2_CH2", "mbar"],
    ["PressureRemoteG3",  "2e", "VAC_REMOTESCROLL_MG1_CH3", "mbar"],
    // Levels & Temperature Exhaust Big CryoTraps
    ["LevelPsCryoNI", "2f", "VAC_LN2NI_LT01_PS_LEVEL", "%"],
    ["LevelPsCryoNE", "2f", "VAC_LN2NE_LT01_PS_LEVEL", "%"],
    ["LevelPsCryoWI", "2f", "VAC_LN2WI_LT01_PS_LEVEL", "%"],
    ["LevelPsCryoWE", "2f", "VAC_LN2WE_LT01_PS_LEVEL", "%"],
    ["LevelTexhaustCryoNI", "2f", "VAC_VALVEBIGNI_CRYO_TEMP", "°C"],
    ["LevelTexhaustCryoNE", "2f", "VAC_VALVEBIGNE_CRYO_TEMP", "°C"],
    ["LevelTexhaustCryoWI", "2f", "VAC_VALVEBIGWI_CRYO_TEMP", "°C"],
    ["LevelTexhaustCryoWE", "2f", "VAC_VALVEBIGWE_CRYO_TEMP", "°C"],
    // Status LN2 TANK Valves
    ["StatusValveLN2NE", "valve", "VAC_VALVEBIGNE_LN2TANKVALVE"],
    ["StatusValveLN2NI", "valve", "VAC_VALVEBIGNI_LN2TANKVALVE"],
    ["StatusValveLN2WE", "valve", "VAC_VALVEBIGWE_LN2TANKVALVE"],
    ["StatusValveLN2WI", "valve", "VAC_VALVEBIGNI_LN2TANKVALVE_WI"],
    // Pressure LN2 TANK
    ["PressureLN2NE", "2f", "VAC_NE1_LN2_BAR", "bar"],
    ["PressureLN2CB", "2f", "VAC_CB3_LN2_BAR", "bar"],
    ["PressureLN2WE", "2f", "VAC_WE1_LN2_BAR", "bar"],
    // Level LN2 TANK
    ["LevelLN2NE", "2f", "VAC_NE1_LN2_LEVEL", "m3"],
    ["LevelLN2CB", "2f", "VAC_CB3_LN2_LEVEL", "m3"],
    ["LevelLN2WE", "2f", "VAC_WE1_LN2_LEVEL", "m3"],
    // Status Level LN2 TANK
    ["StatusLevelLN2NE", "level", "VAC_NE1_LN2_LEVEL", {min: 0, max: 6}],
    ["StatusLevelLN2CB", "level", "VAC_CB3_LN2_LEVEL", {min: 0, max: 25}],
    ["StatusLevelLN2WE", "level", "VAC_WE1_LN2_LEVEL", {min: 0, max: 9}],
    // RGA North
    ["RgaNE_2",  "2e", "VAC_RGA1500N_RGAGe4_002", "mbar"],
    ["RgaNE_18", "2e", "VAC_RGA1500N_RGAGe4_018", "mbar"],
    ["RgaNE_28", "2e", "VAC_RGA1500N_RGAGe4_028", "mbar"],
    ["RgaNE_32", "2e", "VAC_RGA1500N_RGAGe4_032", "mbar"],
    ["RgaNE_44", "2e", "VAC_RGA1500N_RGAGe4_044", "mbar"],
    // RGA West
    ["RgaWE_2",  "2e", "VAC_RGA1500W_RGAGe4_002", "mbar"],
    ["RgaWE_18", "2e", "VAC_RGA1500W_RGAGe4_018", "mbar"],
    ["RgaWE_28", "2e", "VAC_RGA1500W_RGAGe4_028", "mbar"],
    ["RgaWE_32", "2e", "VAC_RGA1500W_RGAGe4_032", "mbar"],
    ["RgaWE_44", "2e", "VAC_RGA1500W_RGAGe4_044", "mbar"],
    // RGA BS
    ["RgaBS_2",  "2e", "VAC_TOWERBS_RGAGd4_002", "mbar"],
    ["RgaBS_18", "2e", "VAC_TOWERBS_RGAGd4_018", "mbar"],
    ["RgaBS_28", "2e", "VAC_TOWERBS_RGAGd4_028", "mbar"],
    ["RgaBS_32", "2e", "VAC_TOWERBS_RGAGd4_032", "mbar"],
    ["RgaBS_44", "2e", "VAC_TOWERBS_RGAGd4_044", "mbar"],
  ];

  // ---------------------------------------------------------------------------
  // Value helpers (same conventions as ViewData.java / controlroom.js)
  // ---------------------------------------------------------------------------
  function clean(v)     { return v == null ? "---" : String(v).replace(/ /g, "").replace(/,/g, "."); }
  function isUnknown(v) { if (v == null) return true; const s = String(v); return s.includes("NOTEXIST") || s.includes("TIMOUT") || s === "---" || s === "..."; }
  function asFloat(v)   { if (isUnknown(v)) return null; const n = Number(clean(v)); return Number.isNaN(n) ? null : n; }

  // Reduce a numeric channel value to its integer-string key ("2.0" -> "2").
  function statusKey(raw) {
    if (isUnknown(raw)) return "---";
    const f = parseFloat(clean(raw));
    if (Number.isNaN(f)) return clean(raw);
    return Number.isInteger(f) ? String(f) : clean(raw);
  }

  // Java String.format("%.2e"): two-digit, signed exponent ("1.23e-09").
  function format2e(f) {
    const [m, e] = f.toExponential(2).split("e");
    const n = Math.abs(parseInt(e, 10));
    return m + "e" + (e[0] === "-" ? "-" : "+") + (n < 10 ? "0" + n : n);
  }

  // ---------------------------------------------------------------------------
  // Render
  // ---------------------------------------------------------------------------
  function render(b, raw) {
    const el = b.el;
    switch (b.kind) {
      case "valve":
        el.setAttribute("fill", VALVE_COLOR[statusKey(raw)] || VALVE_COLOR["---"]);
        break;
      case "valve2":
        el.setAttribute("fill", VALVE2_COLOR[statusKey(raw)] || VALVE2_COLOR["---"]);
        break;
      case "2e":
      case "2f": {
        const f = asFloat(raw);
        el.textContent = (f == null) ? "---"
          : (b.kind === "2e" ? format2e(f) : f.toFixed(2)) + " " + b.extra;
        break;
      }
      case "level": {
        // linear-gradient(to top, deepskyblue <p>%, white 0%); grey when no data.
        const grad = document.getElementById(b.id + "_grad");
        if (!grad) break;
        const f = asFloat(raw);
        const stops = grad.querySelectorAll("stop");
        let p, col;
        if (f == null) { p = 1; col = "grey"; }
        else {
          p = Math.trunc(f * 100 / (b.extra.max - b.extra.min)) / 100;
          p = Math.min(1, Math.max(0, p));
          col = "deepskyblue";
        }
        stops[0].setAttribute("stop-color", col);
        stops[1].setAttribute("stop-color", col);
        stops[1].setAttribute("offset", p);
        stops[2].setAttribute("offset", p);
        break;
      }
    }
  }

  function setBanner(message) {
    const b = document.getElementById("connBanner");
    if (!b) return;
    if (message) { b.textContent = message; b.classList.add("show"); }
    else b.classList.remove("show");
  }

  function setGps(value) {
    const g = document.getElementById("GPS");
    if (g) g.textContent = "GPS: " + (isUnknown(value) ? (String(value).includes("TIMOUT") ? "..." : "---") : clean(value));
  }

  // ---------------------------------------------------------------------------
  // Polling loop
  // ---------------------------------------------------------------------------
  let bindings = [];
  let channelNames = [];

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
      bindings.forEach((b, i) => render(b, values[i]));
      setGps(values.length > channelNames.length ? values[values.length - 1] : null);
      setBanner(null);
    } catch (err) {
      console.warn("jchv poll failed:", err);
      setBanner("jchv unreachable");
    }
  }

  function start() {
    bindings = BINDINGS.map(([id, kind, channel, extra]) =>
      ({id, kind, channel, extra, el: document.getElementById(id)}));
    bindings.filter(b => !b.el).forEach(b => console.warn("vacsupervisor: no SVG element #" + b.id));
    bindings = bindings.filter(b => b.el);
    channelNames = bindings.map(b => b.channel);
    pollOnce();
    setInterval(pollOnce, POLL_MS);
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
})();
