#!/bin/bash
# Trial run only: asks the Arduino hardware tool what enabling the "automation" HAT (header I2C bus i2c0) would do.
# Nothing is changed. Run with: sudo bash ~/pi4j4-test/try-enable-i2c.sh
# The output is saved in ~/pi4j4-test/try-enable-i2c.log
LOG=/home/arduino/pi4j4-test/try-enable-i2c.log
{
  echo "=== $(date) ==="
  echo "--- hw show"
  arduino-linux-config --log-level debug hw show
  echo "--- hw list"
  arduino-linux-config --log-level debug hw list
  echo "--- hw enable automation --dry-run"
  arduino-linux-config --log-level debug hw enable automation --dry-run
  echo "exit code: $?"
} 2>&1 | tee "$LOG"
chown arduino:arduino "$LOG"
