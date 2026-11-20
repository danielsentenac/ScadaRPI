#!/bin/bash
# Gives the VENTUNO Q the Raspberry-OS-style device groups that Pi4J 4 (ffm plugin) insists on.
# Run with: sudo bash ~/pi4j4-test/setup-pi4j-groups.sh     Undo: see the end of this file.
set -e
for g in gpio i2c spi; do getent group $g >/dev/null || groupadd --system $g; done
usermod -aG gpio,i2c,spi,dialout arduino
cat > /etc/udev/rules.d/99-pi4j.rules <<"RULES"
# Pi4J 4 expects these group names on the device files (Arduino default for gpiochips is "gpiod")
SUBSYSTEM=="gpio", KERNEL=="gpiochip*", GROUP="gpio", MODE="0660"
SUBSYSTEM=="i2c-dev", GROUP="i2c", MODE="0660"
SUBSYSTEM=="spidev", GROUP="spi", MODE="0660"
RULES
udevadm control --reload
udevadm trigger --subsystem-match=gpio --subsystem-match=i2c-dev --subsystem-match=spidev
ls -l /dev/gpiochip* /dev/i2c-*
echo "Done. Log out and in again (or reboot) so the new groups apply."
# Undo: rm /etc/udev/rules.d/99-pi4j.rules; udevadm control --reload; udevadm trigger;
#       gpasswd -d arduino gpio; gpasswd -d arduino i2c; gpasswd -d arduino spi; groupdel gpio; groupdel i2c; groupdel spi
