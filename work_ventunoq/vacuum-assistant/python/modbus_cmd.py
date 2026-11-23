"""Modbus TCP write client for the assistant's command path.

Kept apart from modbus_ro.py on purpose: only actions.py imports this module, and it
writes only when "commands_enabled" is true in config.json.

- write_trigger: function 6, one 16-bit command register (valves, pumps, heating channels).
  The panel checks these writes and passes them to the device.
- write_float: function 16, the two registers of a FLOAT32 value (Sicem setpoints), low word
  first as the panel stores them. The two halves must arrive together, hence function 16.
"""
import socket
import struct


class ModbusCommandError(Exception):
    pass


class ModbusCommandClient:
    def __init__(self, host, port=1288, unit=1, timeout=3.0):
        self.host, self.port, self.unit, self.timeout = host, port, unit, timeout
        self._tid = 0

    def _exchange(self, pdu):
        try:
            return self._exchange_once(pdu)
        except socket.timeout:
            # the panel answers nothing to a write it refuses
            raise ModbusCommandError("no answer from the panel (write refused, or panel busy)")
        except OSError as e:
            raise ModbusCommandError("panel not reachable (%s)" % e)

    def _exchange_once(self, pdu):
        self._tid = (self._tid + 1) & 0xFFFF
        with socket.create_connection((self.host, self.port), timeout=self.timeout) as sock:
            sock.settimeout(self.timeout)
            sock.sendall(struct.pack(">HHHB", self._tid, 0, len(pdu) + 1, self.unit) + pdu)
            head = b""
            while len(head) < 8:
                chunk = sock.recv(8 - len(head))
                if not chunk:
                    raise ModbusCommandError("connection closed by the panel")
                head += chunk
            _tid, _proto, length, _unit, func = struct.unpack(">HHHBB", head)
            body = b""
            while len(body) < length - 2:
                chunk = sock.recv(length - 2 - len(body))
                if not chunk:
                    raise ModbusCommandError("connection closed by the panel")
                body += chunk
        if func & 0x80:
            raise ModbusCommandError("panel refused the write (Modbus exception %d)" % body[0])
        return func, body

    def write_trigger(self, offset, value):
        func, body = self._exchange(struct.pack(">BHH", 6, offset, value & 0xFFFF))
        if func != 6 or struct.unpack(">HH", body[:4]) != (offset, value & 0xFFFF):
            raise ModbusCommandError("unexpected reply to the write")

    def write_float(self, offset, value):
        hi, lo = struct.unpack(">HH", struct.pack(">f", value))
        func, body = self._exchange(struct.pack(">BHHBHH", 16, offset, 2, 4, lo, hi))
        if func != 16 or struct.unpack(">HH", body[:4]) != (offset, 2):
            raise ModbusCommandError("unexpected reply to the write")
