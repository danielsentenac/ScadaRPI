# VirgoCleanRoomDeltaP

Arduino PLC module for Virgo clean-room differential pressure sensing with one Würth Elektronik WSEN-PDUS V2 sensor on one Arduino Leonardo ETH.

## Architecture

- One Leonardo ETH per pressure sensor
- One local I2C link between Leonardo ETH and WSEN-PDUS
- One Ethernet connection per node
- One Modbus TCP slave/server per node
- SCADA or supervisory software reads each node on demand

## Mandatory configuration before deployment

- Set `MAC_ADDRESS`, `STATIC_IP`, `DNS_SERVER`, `GATEWAY_IP`, and `SUBNET_MASK`.
- Set `NODE_ID`.
- Set `LOCAL_SENSOR_TYPE` to the exact ordered WSEN-PDUS variant mounted on the node.

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
