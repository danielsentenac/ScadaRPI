/*
  Virgo clean-room differential pressure node

  Hardware:
  - WSEN-PDUS V2 differential pressure sensor on I2C
  - Arduino Leonardo ETH, Arduino Ethernet, or Arduino Uno/Leonardo + W5x00 Ethernet module

  Network role:
  - One node per sensor
  - Modbus TCP slave/server
  - SCADA or supervisory software reads sensor values on demand

  Notes:
  - The intended deployment is one Ethernet-capable Arduino per WSEN-PDUS sensor.
  - The fixed WSEN-PDUS I2C address (0x78) is not a problem in that topology.
  - Update LOCAL_SENSOR_TYPE to match the exact mounted WSEN-PDUS variant.
*/
#include <Wire.h>
#include <SPI.h>
#include <ModbusTCPSlave.h>

// -------------------------------------------------------------------------------------------------
// Site configuration
// -------------------------------------------------------------------------------------------------

#define ETHERNET_BOARD_LEONARDO_ETH 1
#define ETHERNET_BOARD_W5X00_MODULE 2
#define ETHERNET_BOARD_ARDUINO_ETHERNET 3
#define ETHERNET_BOARD_UNO_W5X00 4
#define ETHERNET_BOARD_ROBOTDYN_LEONARDO_W5500 5

// Select the Ethernet hardware fitted on this node.
// - ETHERNET_BOARD_LEONARDO_ETH: use the MAC address printed on the Arduino Leonardo ETH sticker.
// - ETHERNET_BOARD_ARDUINO_ETHERNET: old ATmega328P board with built-in W5100 and EXT PROG.
// - ETHERNET_BOARD_UNO_W5X00: Arduino Uno with a W5100/W5500 module or shield.
// - ETHERNET_BOARD_W5X00_MODULE: use a locally administered MAC address configured below.
// - ETHERNET_BOARD_ROBOTDYN_LEONARDO_W5500: RobotDyn Leonardo+Ethernet W5500+microSD.

//#define ETHERNET_BOARD_TYPE ETHERNET_BOARD_LEONARDO_ETH
//#define ETHERNET_BOARD_TYPE ETHERNET_BOARD_W5X00_MODULE
//#define ETHERNET_BOARD_TYPE ETHERNET_BOARD_ARDUINO_ETHERNET
//#define ETHERNET_BOARD_TYPE ETHERNET_BOARD_UNO_W5X00
#define ETHERNET_BOARD_TYPE ETHERNET_BOARD_ROBOTDYN_LEONARDO_W5500
static const bool USE_DHCP = true;
static const uint8_t DHCP_ATTEMPTS = 5;
static const unsigned long DHCP_RETRY_DELAY_MS = 1000UL;

//byte MAC_ADDRESS[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5D, 0x5A };   // DELTAP1 MAC use with ETHERNET_BOARD_LEONARDO_ETH (Arduino Leonardo ETH)
//byte MAC_ADDRESS[] = { 0x02, 0x56, 0x43, 0x52, 0x44, 0x01 };   // DELTAP2 MAC use with ETHERNET_BOARD_W5X00_MODULE (Arduino Leonardo)
//byte MAC_ADDRESS[] = { 0x02, 0x56, 0x43, 0x52, 0x44, 0x02 };   // DELTAP3 MAC use with ETHERNET_BOARD_W5X00_MODULE (Arduino Leonardo)
//byte MAC_ADDRESS[] = { 0x90, 0xA2, 0xDA, 0x10, 0x2C, 0x1D };   // DELTAP4 MAC use with ETHERNET_BOARD_UNO_W5X00 (Arduino Ethernet)
byte MAC_ADDRESS[] = { 0x02, 0x56, 0x43, 0x52, 0x44, 0x03 };   // DELTAP5 MAC use with ETHERNET_BOARD_W5X00_MODULE (Arduino Leonardo)

IPAddress STATIC_IP(192, 168, 224, 182);
IPAddress DNS_SERVER(192, 168, 224, 1);
IPAddress GATEWAY_IP(192, 168, 224, 1);
IPAddress SUBNET_MASK(255, 255, 255, 0);

#if ETHERNET_BOARD_TYPE == ETHERNET_BOARD_W5X00_MODULE || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ARDUINO_ETHERNET || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_UNO_W5X00 || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ROBOTDYN_LEONARDO_W5500
static const uint8_t W5X00_ETH_CS_PIN = 10;
static const uint8_t W5X00_SD_CS_PIN = 4;

#if ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ROBOTDYN_LEONARDO_W5500
#define W5X00_HAS_RESET_PIN 1
#endif

#ifndef W5X00_HAS_RESET_PIN
#define W5X00_HAS_RESET_PIN 0
#endif

#if W5X00_HAS_RESET_PIN
static const uint8_t W5X00_ETH_RST_PIN = 11;
#endif

// Modern Ethernet library (2.x) and the bundled ModbusTCP both use <Ethernet.h> which
// provides Ethernet.init(). Override to 0 only if linking against an older library.
#ifndef W5X00_USE_ETHERNET_INIT
#define W5X00_USE_ETHERNET_INIT 1
#endif
#endif

static const uint16_t MODBUS_PORT = 502;
static const uint8_t NODE_ID = 1;

static const bool SERIAL_DIAGNOSTICS = true;
static const unsigned long SERIAL_BAUD = 9600UL;
static const unsigned long LOCAL_SAMPLE_INTERVAL_MS = 500UL;
static const unsigned long SERIAL_SNAPSHOT_INTERVAL_MS = 5000UL;
static const uint32_t W5X00_SPI_PROBE_HZ = 1000000UL;

// -------------------------------------------------------------------------------------------------
// WSEN-PDUS sensor configuration
// -------------------------------------------------------------------------------------------------

static const uint8_t PDUS_I2C_ADDRESS = 0x78;

enum PDUSSensorType
{
  PDUS_RANGE_NEG_0_1_TO_POS_0_1_KPA = 0, // 2513130810001
  PDUS_RANGE_NEG_1_TO_POS_1_KPA     = 1, // 2513130810101
  PDUS_RANGE_NEG_10_TO_POS_10_KPA   = 2, // 2513130810201
  PDUS_RANGE_0_TO_100_KPA           = 3, // 2513130810301
  PDUS_RANGE_NEG_100_TO_POS_100_KPA = 4  // 2513130810401
};

// Clean-room applications usually use one of the low differential ranges.
// This must match the exact mounted sensor variant before deployment.
static const PDUSSensorType LOCAL_SENSOR_TYPE = PDUS_RANGE_NEG_1_TO_POS_1_KPA;

static const uint16_t PDUS_PRESSURE_RAW_MIN = 3277U;
static const uint16_t PDUS_TEMPERATURE_RAW_MIN = 8192U;
static const uint16_t MAP_VERSION = 1U;

// -------------------------------------------------------------------------------------------------
// Register map
// -------------------------------------------------------------------------------------------------

enum RegisterMap
{
  REG_PRESSURE_LO = 0,
  REG_PRESSURE_HI = 1,
  REG_TEMPERATURE_LO = 2,
  REG_TEMPERATURE_HI = 3,
  REG_RAW_PRESSURE = 4,
  REG_RAW_TEMPERATURE = 5,
  REG_STATUS = 6,
  REG_NODE_ID = 7,
  REG_SENSOR_TYPE = 8,
  REG_SAMPLE_AGE_S = 9,
  REG_MAP_VERSION = 10,
  REG_RESET_CMD = 11,
  NB_HOLDING_REGISTERS = 12
};

enum StatusFlag
{
  STATUS_OK = 0x0001,
  STATUS_I2C_ERROR = 0x0002,
  STATUS_CONFIG_ERROR = 0x0004,
  STATUS_STALE = 0x0008
};

typedef union
{
  float fvalue;
  uint16_t value[2];
} FloatUint16;

struct SensorSnapshot
{
  float pressureKPa;
  float temperatureDegC;
  uint16_t rawPressure;
  uint16_t rawTemperature;
  uint16_t status;
  unsigned long updatedAtMs;
};

ModbusTCPSlave modbus(MODBUS_PORT);
uint16_t holdingRegisters[NB_HOLDING_REGISTERS];
SensorSnapshot localSnapshot;
unsigned long lastLocalSampleMs = 0UL;
unsigned long lastSnapshotLogMs = 0UL;

void(*resetBoard)(void) = 0;

// -------------------------------------------------------------------------------------------------
// Helpers
// -------------------------------------------------------------------------------------------------

void logMessage(const __FlashStringHelper *message)
{
  if (SERIAL_DIAGNOSTICS)
  {
    Serial.println(message);
  }
}

void logBoardConfiguration()
{
  if (!SERIAL_DIAGNOSTICS)
  {
    return;
  }

  Serial.print(F("Ethernet board mode: "));
#if ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ARDUINO_ETHERNET
  Serial.println(F("Arduino Ethernet"));
#elif ETHERNET_BOARD_TYPE == ETHERNET_BOARD_LEONARDO_ETH
  Serial.println(F("Arduino Leonardo ETH"));
#elif ETHERNET_BOARD_TYPE == ETHERNET_BOARD_UNO_W5X00
  Serial.println(F("Arduino Uno + W5x00 module"));
#elif ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ROBOTDYN_LEONARDO_W5500
  Serial.println(F("RobotDyn Leonardo + W5500"));
#elif ETHERNET_BOARD_TYPE == ETHERNET_BOARD_W5X00_MODULE
  Serial.println(F("W5x00 Ethernet module"));
#else
  Serial.println(F("unknown"));
#endif

  Serial.print(F("Arduino target: "));
#if defined(ARDUINO_AVR_ETHERNET)
  Serial.println(F("arduino:avr:ethernet"));
#elif defined(ARDUINO_AVR_UNO)
  Serial.println(F("arduino:avr:uno"));
#elif defined(ARDUINO_AVR_LEONARDO)
  Serial.println(F("arduino:avr:leonardo"));
#elif defined(ARDUINO_AVR_LEONARDO_ETH)
  Serial.println(F("arduino:avr:leonardoeth"));
#else
  Serial.println(F("not detected"));
#endif
}

void logEthernetStatus()
{
  if (!SERIAL_DIAGNOSTICS)
  {
    return;
  }

  Serial.print(F("Ethernet hardware: "));
  switch (Ethernet.hardwareStatus())
  {
    case EthernetNoHardware: Serial.println(F("none detected (SPI/wiring?)")); break;
    case EthernetW5100:      Serial.println(F("W5100")); break;
    case EthernetW5200:      Serial.println(F("W5200")); break;
    case EthernetW5500:      Serial.println(F("W5500")); break;
    default:                 Serial.println(F("unknown")); break;
  }

  Serial.print(F("Ethernet link: "));
  switch (Ethernet.linkStatus())
  {
    case LinkON:  Serial.println(F("up")); break;
    case LinkOFF: Serial.println(F("down (cable?)")); break;
    case Unknown:
    default:      Serial.println(F("unknown")); break;
  }
}

void logMacAddress()
{
  if (!SERIAL_DIAGNOSTICS)
  {
    return;
  }

  Serial.print(F("Ethernet MAC: "));
  for (uint8_t i = 0; i < 6; ++i)
  {
    if (MAC_ADDRESS[i] < 0x10)
    {
      Serial.print(F("0"));
    }
    Serial.print(MAC_ADDRESS[i], HEX);
    if (i < 5)
    {
      Serial.print(F(":"));
    }
  }
  Serial.println();
}

void logNetworkAddress(const __FlashStringHelper *label, IPAddress address)
{
  if (!SERIAL_DIAGNOSTICS)
  {
    return;
  }

  Serial.print(label);
  Serial.println(address);
}

#if ETHERNET_BOARD_TYPE == ETHERNET_BOARD_W5X00_MODULE || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ARDUINO_ETHERNET || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_UNO_W5X00 || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ROBOTDYN_LEONARDO_W5500
uint8_t readW5500VersionRegister(uint8_t csPin)
{
  SPI.beginTransaction(SPISettings(W5X00_SPI_PROBE_HZ, MSBFIRST, SPI_MODE0));
  digitalWrite(csPin, LOW);
  SPI.transfer(0x00);
  SPI.transfer(0x39);
  SPI.transfer(0x00);
  uint8_t version = SPI.transfer(0x00);
  digitalWrite(csPin, HIGH);
  SPI.endTransaction();
  return version;
}

void logW5500Probe(uint8_t csPin)
{
  if (!SERIAL_DIAGNOSTICS)
  {
    return;
  }

  pinMode(csPin, OUTPUT);
  digitalWrite(csPin, HIGH);

  Serial.print(F("W5500 VERSIONR probe CS D"));
  Serial.print(csPin);
  Serial.print(F(": 0x"));
  uint8_t version = readW5500VersionRegister(csPin);
  if (version < 0x10)
  {
    Serial.print(F("0"));
  }
  Serial.println(version, HEX);
}

void logW5500ProbeScan()
{
  if (!SERIAL_DIAGNOSTICS)
  {
    return;
  }

  logMessage(F("W5500 SPI probe: expected VERSIONR 0x04"));
  logW5500Probe(W5X00_ETH_CS_PIN);
  logW5500Probe(8);
  logW5500Probe(SS);
}
#endif

void logSnapshot(const SensorSnapshot &snapshot)
{
  if (!SERIAL_DIAGNOSTICS)
  {
    return;
  }

  Serial.print(F("PDUS pressure[kPa]: "));
  Serial.print(snapshot.pressureKPa, 4);
  Serial.print(F(" temp[C]: "));
  Serial.print(snapshot.temperatureDegC, 2);
  Serial.print(F(" rawP: "));
  Serial.print(snapshot.rawPressure);
  Serial.print(F(" rawT: "));
  Serial.print(snapshot.rawTemperature);
  Serial.print(F(" status: 0x"));
  Serial.println(snapshot.status, HEX);
}

void writeFloatRegisterPair(uint16_t startRegister, float value)
{
  FloatUint16 conversion;
  conversion.fvalue = value;
  holdingRegisters[startRegister] = conversion.value[0];
  holdingRegisters[startRegister + 1] = conversion.value[1];
}

bool convertRawPdusValues(PDUSSensorType sensorType,
                          uint16_t rawPressure,
                          uint16_t rawTemperature,
                          float &pressureKPa,
                          float &temperatureDegC)
{
  float temporary = (float)rawTemperature - (float)PDUS_TEMPERATURE_RAW_MIN;
  temperatureDegC = (temporary * 4.272f) / 1000.0f;

  temporary = (float)rawPressure - (float)PDUS_PRESSURE_RAW_MIN;

  switch (sensorType)
  {
    case PDUS_RANGE_NEG_0_1_TO_POS_0_1_KPA:
      pressureKPa = ((temporary * 7.63f) / 1000000.0f) - 0.1f;
      return true;
    case PDUS_RANGE_NEG_1_TO_POS_1_KPA:
      pressureKPa = ((temporary * 7.63f) / 100000.0f) - 1.0f;
      return true;
    case PDUS_RANGE_NEG_10_TO_POS_10_KPA:
      pressureKPa = ((temporary * 7.63f) / 10000.0f) - 10.0f;
      return true;
    case PDUS_RANGE_0_TO_100_KPA:
      pressureKPa = (temporary * 3.815f) / 1000.0f;
      return true;
    case PDUS_RANGE_NEG_100_TO_POS_100_KPA:
      pressureKPa = ((temporary * 4.196f) / 100.0f) - 100.0f;
      return true;
    default:
      pressureKPa = 0.0f;
      temperatureDegC = 0.0f;
      return false;
  }
}

bool readLocalSensor(SensorSnapshot &snapshot)
{
  uint8_t data[4] = { 0, 0, 0, 0 };

  int bytesReceived = Wire.requestFrom((int)PDUS_I2C_ADDRESS, 4);
  if (bytesReceived != 4)
  {
    snapshot.status = STATUS_I2C_ERROR | STATUS_STALE;
    return false;
  }

  for (uint8_t i = 0; i < 4; ++i)
  {
    if (!Wire.available())
    {
      snapshot.status = STATUS_I2C_ERROR | STATUS_STALE;
      return false;
    }
    data[i] = (uint8_t)Wire.read();
  }

  snapshot.rawPressure = (((uint16_t)data[0]) << 8) | data[1];
  snapshot.rawTemperature = (((uint16_t)data[2]) << 8) | data[3];

  if (!convertRawPdusValues(LOCAL_SENSOR_TYPE,
                            snapshot.rawPressure,
                            snapshot.rawTemperature,
                            snapshot.pressureKPa,
                            snapshot.temperatureDegC))
  {
    snapshot.status = STATUS_CONFIG_ERROR | STATUS_STALE;
    return false;
  }

  snapshot.status = STATUS_OK;
  snapshot.updatedAtMs = millis();
  return true;
}

void publishLocalSnapshot()
{
  writeFloatRegisterPair(REG_PRESSURE_LO, localSnapshot.pressureKPa);
  writeFloatRegisterPair(REG_TEMPERATURE_LO, localSnapshot.temperatureDegC);

  holdingRegisters[REG_RAW_PRESSURE] = localSnapshot.rawPressure;
  holdingRegisters[REG_RAW_TEMPERATURE] = localSnapshot.rawTemperature;
  holdingRegisters[REG_STATUS] = localSnapshot.status;
  holdingRegisters[REG_NODE_ID] = NODE_ID;
  holdingRegisters[REG_SENSOR_TYPE] = (uint16_t)LOCAL_SENSOR_TYPE;
  holdingRegisters[REG_SAMPLE_AGE_S] =
    (uint16_t)((millis() - localSnapshot.updatedAtMs) / 1000UL);
  holdingRegisters[REG_MAP_VERSION] = MAP_VERSION;
}

void startModbusServer()
{
  bool networkReady = false;

  logMacAddress();
#if ETHERNET_BOARD_TYPE == ETHERNET_BOARD_W5X00_MODULE || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ARDUINO_ETHERNET || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_UNO_W5X00 || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ROBOTDYN_LEONARDO_W5500
  logW5500ProbeScan();
#endif

  if (USE_DHCP)
  {
    logMessage(F("Trying DHCP"));
    for (uint8_t attempt = 0; attempt < DHCP_ATTEMPTS && !networkReady; ++attempt)
    {
      if (SERIAL_DIAGNOSTICS)
      {
        Serial.print(F("DHCP attempt "));
        Serial.println(attempt + 1);
      }
      networkReady = Ethernet.begin(MAC_ADDRESS) != 0;
      if (!networkReady)
      {
        logMessage(F("DHCP attempt failed"));
        delay(DHCP_RETRY_DELAY_MS);
      }
    }
    if (networkReady)
    {
      logMessage(F("DHCP succeeded"));
    }
    if (!networkReady)
    {
      logMessage(F("DHCP failed, falling back to static IP"));
    }
  }

  if (!networkReady)
  {
    logMessage(F("Starting with static IP"));
    Ethernet.begin(MAC_ADDRESS, STATIC_IP, DNS_SERVER, GATEWAY_IP, SUBNET_MASK);
  }

  delay(1000);

  logEthernetStatus();
#if ETHERNET_BOARD_TYPE == ETHERNET_BOARD_W5X00_MODULE || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ARDUINO_ETHERNET || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_UNO_W5X00 || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ROBOTDYN_LEONARDO_W5500
  if (Ethernet.hardwareStatus() == EthernetNoHardware)
  {
    logW5500ProbeScan();
  }
#endif
  logNetworkAddress(F("IP: "), Ethernet.localIP());
  logNetworkAddress(F("Gateway: "), Ethernet.gatewayIP());
  logNetworkAddress(F("Subnet: "), Ethernet.subnetMask());

  modbus.begin();
  modbus.setHoldingRegisters(holdingRegisters, NB_HOLDING_REGISTERS);
}

void initialiseEthernetHardware()
{
  logMessage(F("Initialising Ethernet hardware"));

#if ETHERNET_BOARD_TYPE == ETHERNET_BOARD_W5X00_MODULE || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ARDUINO_ETHERNET || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_UNO_W5X00 || ETHERNET_BOARD_TYPE == ETHERNET_BOARD_ROBOTDYN_LEONARDO_W5500
  pinMode(SS, OUTPUT);
  digitalWrite(SS, HIGH);

  pinMode(W5X00_ETH_CS_PIN, OUTPUT);
  digitalWrite(W5X00_ETH_CS_PIN, HIGH);

  pinMode(W5X00_SD_CS_PIN, OUTPUT);
  digitalWrite(W5X00_SD_CS_PIN, HIGH);

#if W5X00_HAS_RESET_PIN
  pinMode(W5X00_ETH_RST_PIN, OUTPUT);
  digitalWrite(W5X00_ETH_RST_PIN, HIGH);
  delay(200);
#endif

  SPI.begin();

#if W5X00_USE_ETHERNET_INIT
  Ethernet.init(W5X00_ETH_CS_PIN);
#endif
#endif

  logMessage(F("Ethernet hardware ready"));
}

void initialiseRegisters()
{
  for (uint16_t i = 0; i < NB_HOLDING_REGISTERS; ++i)
  {
    holdingRegisters[i] = 0U;
  }

  localSnapshot.pressureKPa = 0.0f;
  localSnapshot.temperatureDegC = 0.0f;
  localSnapshot.rawPressure = 0U;
  localSnapshot.rawTemperature = 0U;
  localSnapshot.status = STATUS_STALE;
  localSnapshot.updatedAtMs = millis();

  holdingRegisters[REG_NODE_ID] = NODE_ID;
  holdingRegisters[REG_SENSOR_TYPE] = (uint16_t)LOCAL_SENSOR_TYPE;
  holdingRegisters[REG_MAP_VERSION] = MAP_VERSION;
}

void setup()
{
  if (SERIAL_DIAGNOSTICS)
  {
    Serial.begin(SERIAL_BAUD);
    delay(300);
    logMessage(F("VirgoCleanRoomDeltaP boot"));
    logBoardConfiguration();
    logMessage(F("Serial diagnostics ready"));
  }

  logMessage(F("Starting I2C"));
  Wire.begin();
  logMessage(F("I2C ready"));

  initialiseRegisters();
  logMessage(F("Registers ready"));
  initialiseEthernetHardware();
  logMessage(F("Starting Modbus TCP"));
  startModbusServer();
  logMessage(F("Modbus TCP ready"));

  logMessage(F("Reading local sensor"));
  readLocalSensor(localSnapshot);
  publishLocalSnapshot();
  logSnapshot(localSnapshot);

  if (SERIAL_DIAGNOSTICS)
  {
    logMessage(F("Network ready"));
    Serial.print(F("IP: "));
    Serial.println(Ethernet.localIP());
  }
}

void loop()
{
  Ethernet.maintain();
  modbus.update();

  unsigned long now = millis();
  if ((now - lastLocalSampleMs) >= LOCAL_SAMPLE_INTERVAL_MS)
  {
    lastLocalSampleMs = now;
    readLocalSensor(localSnapshot);
    publishLocalSnapshot();
    if ((now - lastSnapshotLogMs) >= SERIAL_SNAPSHOT_INTERVAL_MS)
    {
      lastSnapshotLogMs = now;
      logSnapshot(localSnapshot);
    }
  }

  holdingRegisters[REG_SAMPLE_AGE_S] =
    (uint16_t)((millis() - localSnapshot.updatedAtMs) / 1000UL);

  if (holdingRegisters[REG_RESET_CMD] == 0x0001U)
  {
    resetBoard();
  }
}
