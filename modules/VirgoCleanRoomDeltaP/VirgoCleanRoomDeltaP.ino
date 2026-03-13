/*
  Virgo clean-room differential pressure node

  Hardware:
  - WSEN-PDUS V2 differential pressure sensor on I2C
  - Arduino Leonardo ETH on Ethernet

  Network role:
  - One node per sensor
  - Modbus TCP slave/server
  - SCADA or supervisory software reads sensor values on demand

  Notes:
  - The intended deployment is one Arduino Leonardo ETH per WSEN-PDUS sensor.
  - The fixed WSEN-PDUS I2C address (0x78) is not a problem in that topology.
  - Update LOCAL_SENSOR_TYPE to match the exact mounted WSEN-PDUS variant.
*/
#include <Wire.h>
#include <ModbusTCPSlave.h>

// -------------------------------------------------------------------------------------------------
// Site configuration
// -------------------------------------------------------------------------------------------------


static const bool USE_DHCP = true;
static const uint8_t DHCP_ATTEMPTS = 5;
static const unsigned long DHCP_RETRY_DELAY_MS = 1000UL;

byte MAC_ADDRESS[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5D, 0x5A };
IPAddress STATIC_IP(192, 168, 224, 190);
IPAddress DNS_SERVER(192, 168, 224, 1);
IPAddress GATEWAY_IP(192, 168, 224, 1);
IPAddress SUBNET_MASK(255, 255, 255, 0);

static const uint16_t MODBUS_PORT = 502;
static const uint8_t NODE_ID = 1;

static const bool SERIAL_DIAGNOSTICS = true;
static const unsigned long SERIAL_BAUD = 115200UL;
static const unsigned long LOCAL_SAMPLE_INTERVAL_MS = 500UL;

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

  if (USE_DHCP)
  {
    logMessage(F("Trying DHCP"));
    for (uint8_t attempt = 0; attempt < DHCP_ATTEMPTS && !networkReady; ++attempt)
    {
      networkReady = Ethernet.begin(MAC_ADDRESS) != 0;
      if (!networkReady)
      {
        logMessage(F("DHCP attempt failed"));
        delay(DHCP_RETRY_DELAY_MS);
      }
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

  modbus.begin();
  modbus.setHoldingRegisters(holdingRegisters, NB_HOLDING_REGISTERS);
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
  }

  Wire.begin();

  initialiseRegisters();
  startModbusServer();

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
    logSnapshot(localSnapshot);
  }

  holdingRegisters[REG_SAMPLE_AGE_S] =
    (uint16_t)((millis() - localSnapshot.updatedAtMs) / 1000UL);

  if (holdingRegisters[REG_RESET_CMD] == 0x0001U)
  {
    resetBoard();
  }
}
