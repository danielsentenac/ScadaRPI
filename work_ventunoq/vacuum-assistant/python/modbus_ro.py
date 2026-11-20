"""Minimal read-only Modbus TCP client.

It implements only function 3 (read holding registers). There is deliberately
no write function in this module: the assistant cannot send commands.
"""
import socket
import struct


class ModbusError(Exception):
    pass


class ModbusReadOnlyClient:
    MAX_BLOCK = 120  # registers per request (protocol limit is 125)

    def __init__(self, host, port=1288, unit=1, timeout=3.0):
        self.host, self.port, self.unit, self.timeout = host, port, unit, timeout
        self._tid = 0

    def _recv_exact(self, sock, n):
        buf = b""
        while len(buf) < n:
            chunk = sock.recv(n - len(buf))
            if not chunk:
                raise ModbusError("connection closed by the panel")
            buf += chunk
        return buf

    def _read_block(self, sock, start, count):
        self._tid = (self._tid + 1) & 0xFFFF
        sock.sendall(struct.pack(">HHHBBHH", self._tid, 0, 6, self.unit, 3, start, count))
        tid, _proto, length, _unit, func = struct.unpack(">HHHBB", self._recv_exact(sock, 8))
        body = self._recv_exact(sock, length - 2)
        if tid != self._tid:
            raise ModbusError("transaction id mismatch")
        if func & 0x80:
            raise ModbusError("panel returned Modbus exception %d" % body[0])
        if func != 3 or body[0] != 2 * count:
            raise ModbusError("unexpected reply")
        return list(struct.unpack(">%dH" % count, body[1:1 + 2 * count]))

    def read_registers(self, start, count):
        """Read `count` holding registers from `start`. A new connection is opened for
        each call, because the panel closes connections idle for more than 5 s."""
        out = []
        with socket.create_connection((self.host, self.port), timeout=self.timeout) as sock:
            sock.settimeout(self.timeout)
            pos = start
            while pos < start + count:
                n = min(self.MAX_BLOCK, start + count - pos)
                out.extend(self._read_block(sock, pos, n))
                pos += n
        return out
