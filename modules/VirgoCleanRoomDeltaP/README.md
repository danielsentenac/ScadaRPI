# VirgoCleanRoomDeltaP

Arduino PLC module for Virgo clean-room differential pressure sensing with one Würth Elektronik WSEN-PDUS V2 sensor on one Arduino Leonardo ETH or one Arduino Leonardo with a W5x00 Ethernet module.

## Architecture

- One Leonardo ETH, or one Leonardo with W5x00 Ethernet, per pressure sensor
- One local I2C link between the Leonardo board and WSEN-PDUS
- One Ethernet connection per node
- One Modbus TCP slave/server per node
- SCADA or supervisory software reads each node on demand

## Mandatory configuration before deployment

- Set `ETHERNET_BOARD_TYPE`.
- For `ETHERNET_BOARD_LEONARDO_ETH`, set `MAC_ADDRESS` from the sticker on the board.
- For `ETHERNET_BOARD_W5X00_MODULE`, set a unique locally administered `MAC_ADDRESS`; there is normally no sticker/factory MAC to retrieve.
- Set `STATIC_IP`, `DNS_SERVER`, `GATEWAY_IP`, and `SUBNET_MASK`.
- Set `NODE_ID`.
- Set `LOCAL_SENSOR_TYPE` to the exact ordered WSEN-PDUS variant mounted on the node.

## Ethernet hardware selection

Default build:

```cpp
#define ETHERNET_BOARD_TYPE ETHERNET_BOARD_LEONARDO_ETH
```

External W5x00 module build:

```cpp
#define ETHERNET_BOARD_TYPE ETHERNET_BOARD_W5X00_MODULE
```

The default W5x00 pin settings are:

```cpp
static const uint8_t W5X00_ETH_CS_PIN = 10;
static const uint8_t W5X00_ETH_RST_PIN = 11;
static const uint8_t W5X00_SD_CS_PIN = 4;
```

On Leonardo, SPI must be connected through the ICSP header. Digital pins 10, 11, and 4 above are only chip-select/reset GPIOs, not the SPI MOSI/MISO/SCK pins.

## Local register map

The node publishes these holding registers starting at address `0`.

| Address | Meaning |
| --- | --- |
| 0-1 | Pressure in kPa as IEEE754 float |
| 2-3 | Temperature in degC as IEEE754 float |
| 4 | Raw pressure word |
| 5 | Raw temperature word |
| 6 | Status flags |
| 7 | Node ID |
| 8 | Sensor type enum |
| 9 | Local sample age in seconds |
| 10 | Register-map version |
| 11 | Reset command (`1` reboots the board) |

## Status flags

- `0x0001`: measurement valid
- `0x0002`: WSEN-PDUS I2C read error
- `0x0004`: invalid sensor-type configuration
- `0x0008`: stale value
