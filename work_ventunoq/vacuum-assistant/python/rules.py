"""Operating knowledge of the tube 1500 West, from the operators' instruction sheet
("FILE ISTRUZIONI PER TUBO 1500W", received 2026-11-23) and from what they said.

Everything here is checked by the app on the values read from the panel; the model is only
given the result. Edit this file when the instructions change.
"""

# What the operators decided; given to the model with every question.
OPERATING_RULES = (
    "- Heating: all heating channels CH1-CH9 must always have the same setpoint.\n"
    "- Safety mode: if a problem occurs (a valve interlock triggered, pumping failure, valve closed), "
    "the system must be put in safety mode. The operator presses the CH1 OFF and CH2 OFF buttons: this "
    "switches off the heating current in the Sicem controller.\n"
    "- Sicem global alarm: it means a failure of the Sicem controller itself. It is not wired to any interlock. "
    "If it is in alarm, the operator must press CH1 OFF and CH2 OFF.\n"
    "- Normal regime: P22 on; G22 between 0.5 and 5E-4 mbar; V22 open; P21 on at its speed (above 900 Hz in normal mode, nominal 1000 Hz; above 600 Hz in standby mode, nominal 666 Hz); "
    "V21 open; G21 between 1E-5 and 5E-11 mbar.\n"
    "- Ga5 is for high pressure: it cannot measure below 5E-6 mbar; at low pressure use G21.\n"
    "- Protection of the turbo pump P21 against high pressure: a hardware interlock on gauge G22. V21 and V22 "
    "cannot be opened unless G22 is below 1.0 mbar, and they are closed if G22 goes above 5.0 mbar."
)

# Measuring range of each gauge: channel -> (type, lowest, highest, advice outside the range), in mbar.
PIRANI = ("Pirani", 5e-4, 800.0, "")            # 800 to 1100 mbar is not reliable
COLD_CATHODE = ("cold cathode", 5e-11, 5e-5, "")
GAUGE_RANGES = {
    "MG_PR1": PIRANI,         # G22
    "MG_PR2": COLD_CATHODE,   # G21
    "MG_PR3": COLD_CATHODE,   # Ga4
    "MG_PR4": COLD_CATHODE,   # G4 (H2O vapour line)
    "MG_PR5": PIRANI,         # G5 (H2O reservoir)
    # Ga5 is for the high-pressure regime; it cannot measure below 5E-6 mbar, where G21 is used.
    # Range 1 mbar to 5E-6 mbar confirmed by the operators on 2026-11-23.
    "ROT_PR1": ("spinning rotor", 5e-6, 1.0, "; Ga5 cannot measure below 5E-6 mbar, use G21 for low pressure"),
}

# Normal regime ("sistema operativo a regime"):
# (requirement, channels used, test, what to say when met, what to say when not met).
# The comparison is done here and its result written out: the model must not compare numbers.
def _turbo_speed(v):
    """Speed required from the turbo pump, its nominal speed and the mode: the standby mode runs
    the pump at two thirds of the nominal 1000 Hz, which is a correct state of the pump."""
    if v.get("DCU_P21STYST") == 1:
        return 600, 666, "standby mode"
    return 900, 1000, "normal mode"


NORMAL_REGIME = [
    ("P22 on", ["M1_P22ST"], lambda v: v["M1_P22ST"] == 1,
     lambda v: "P22 is on", lambda v: "P22 dry pump is off"),
    ("G22 between 0.5 and 5E-4 mbar", ["MG_PR1"], lambda v: 5e-4 <= v["MG_PR1"] <= 0.5,
     lambda v: "G22 = %.2e mbar is inside its normal range (0.5 to 5E-4 mbar)" % v["MG_PR1"],
     lambda v: "G22 = %.2e mbar is OUTSIDE its normal range (0.5 to 5E-4 mbar)" % v["MG_PR1"]),
    ("V22 open", ["M1_V22ST"], lambda v: v["M1_V22ST"] == 1,
     lambda v: "V22 is open", lambda v: "V22 is not open"),
    ("P21 on at its speed (above 900 Hz in normal mode, above 600 Hz in standby mode)",
     ["DCU_P21ST", "DCU_P21SPEED", "DCU_P21STYST"],
     lambda v: v["DCU_P21ST"] == 1 and v["DCU_P21SPEED"] > _turbo_speed(v)[0],
     lambda v: "P21 is on at %d Hz, correct for %s (above %d Hz, nominal %d Hz)"
               % ((v["DCU_P21SPEED"], _turbo_speed(v)[2]) + _turbo_speed(v)[:2]),
     lambda v: "P21 turbo pump is off" if v["DCU_P21ST"] != 1 else
               "P21 speed = %d Hz is NOT above the %d Hz required in %s (nominal %d Hz)"
               % (v["DCU_P21SPEED"], _turbo_speed(v)[0], _turbo_speed(v)[2], _turbo_speed(v)[1])),
    ("V21 open", ["M1_V21ST"], lambda v: v["M1_V21ST"] == 1,
     lambda v: "V21 is open", lambda v: "V21 is not open"),
    ("G21 between 1E-5 and 5E-11 mbar", ["MG_PR2"], lambda v: 5e-11 <= v["MG_PR2"] <= 1e-5,
     lambda v: "G21 = %.2e mbar is inside its normal range (1E-5 to 5E-11 mbar)" % v["MG_PR2"],
     lambda v: "G21 = %.2e mbar is OUTSIDE its normal range (1E-5 to 5E-11 mbar)" % v["MG_PR2"]),
]

# Hardware interlocks: valve -> (rule in words, [(condition, channels, test)]).
# G22 acts through two MaxiGauge relays with thresholds LOW 1.0 mbar and HIGH 5.0 mbar: V21 and V22
# cannot be opened unless G22 is below 1.0 mbar, and are closed when G22 goes above 5.0 mbar.
# The bypass excludes this G22 safety for V22 (primary pumping). V1 (H2O line) is hard-wired to open
# only with the bypass on and P22 on.
_P22_OFF = ("P22 is off", ["M1_P22ST"], lambda v: v["M1_P22ST"] != 1)
_G22_5 = ("G22 is above 5.0 mbar (valve forced closed)", ["MG_PR1"], lambda v: v["MG_PR1"] > 5.0)
_G22_1 = ("G22 is above 1.0 mbar (opening not allowed)", ["MG_PR1"], lambda v: 1.0 < v["MG_PR1"] <= 5.0)
_AIR = ("compressed air is below 5 bar", ["M1_COMPRESSAIRST"], lambda v: v["M1_COMPRESSAIRST"] != 0)
_TURBO_SLOW = ("P21 speed is below 80% of nominal", ["DCU_P21SPEED", "DCU_P21FSPEED"],
               lambda v: v["DCU_P21SPEED"] < 0.8 * v["DCU_P21FSPEED"])
_NO_BYPASS = ("the bypass is not on", ["M1_BYPASSST"], lambda v: v["M1_BYPASSST"] != 1)
# for V22 the G22 safety counts only while the bypass is not on
_G22_5_NB = (_G22_5[0], ["MG_PR1", "M1_BYPASSST"], lambda v: v["M1_BYPASSST"] != 1 and v["MG_PR1"] > 5.0)
_G22_1_NB = (_G22_1[0], ["MG_PR1", "M1_BYPASSST"], lambda v: v["M1_BYPASSST"] != 1 and 1.0 < v["MG_PR1"] <= 5.0)
_G22_RULE = "it cannot be opened unless G22 is below 1.0 mbar and is closed if G22 goes above 5.0 mbar"
INTERLOCKS = {
    "V22": ("closes if P22 is off or if the compressed air is below 5 bar; %s (the bypass, when on, "
            "switches off this G22 safety for primary pumping)" % _G22_RULE, [_P22_OFF, _G22_5_NB, _G22_1_NB, _AIR]),
    "V21": ("closes if P22 is off, if P21 speed is below 80%% or if the compressed air is below 5 bar; %s "
            "(this hardware interlock on G22 protects the turbo pump P21 from high pressure)" % _G22_RULE, [_P22_OFF, _G22_5, _G22_1, _TURBO_SLOW, _AIR]),
    "V1": ("(H2O line) can be opened only if P22 is on and the bypass is on; it closes if the compressed air "
           "is below 5 bar", [_P22_OFF, _NO_BYPASS, _AIR]),
}


def gauge_note(channel, value):
    """Text added after a gauge reading: its type and range, and a warning outside the range."""
    if channel not in GAUGE_RANGES:
        return ""
    kind, low, high, advice = GAUGE_RANGES[channel]
    if low <= value <= high:
        return " (%s gauge; inside its measuring range, %.0e to %.0e mbar)" % (kind, high, low)
    return " (%s gauge; OUTSIDE its measuring range, %.0e to %.0e mbar: not reliable%s)" % (kind, high, low, advice)


def turbo_note(channel, values):
    """Text added after the turbo pump speed: whether it is correct for the mode (standby or normal)."""
    if channel != "DCU_P21SPEED" or values.get("DCU_P21ST") != 1:
        return ""
    required, nominal, mode = _turbo_speed(values)
    if values[channel] > required:
        return " (CORRECT for %s: nominal %d Hz, must be above %d Hz)" % (mode, nominal, required)
    return " (TOO LOW for %s: nominal %d Hz, must be above %d Hz)" % (mode, nominal, required)


def regime(values, channel_ok):
    """(met, not met, not checkable) against the normal regime, each as ready-made sentences.
    channel_ok(channel) is False when the device of that channel is not communicating."""
    met, not_met, unknown = [], [], []
    for name, channels, test, yes, no in NORMAL_REGIME:
        if not all(channel_ok(c) for c in channels):
            unknown.append(name)
        elif test(values):
            met.append(yes(values))
        else:
            not_met.append(no(values))
    return met, not_met, unknown


def interlock_lines(values, channel_ok, asked=()):
    """One line per valve; if the question names some valves, only those (fewer mix-ups)."""
    lines = []
    for valve, (rule, conds) in INTERLOCKS.items():
        if asked and valve not in asked:
            continue
        now = [text for text, channels, test in conds if all(channel_ok(c) for c in channels) and test(values)]
        verdict = ("So %s can NOT be opened now (and is closed by the interlock if it was open)" % valve if now
                   else "So no interlock prevents %s from being open now" % valve)
        lines.append(" Interlock of %s: it %s. Conditions present now: %s. %s."
                     % (valve, rule, "; ".join(now) if now else "none", verdict))
    return lines
