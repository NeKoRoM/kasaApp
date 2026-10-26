#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Еталонний клієнт протоколу Datecs v2 / Exellio мовою Python 3.
Використовується для швидкого тестування зв'язку по TCP сокету (порт 4999),
Bluetooth SPP (/dev/rfcomm0 або /dev/tty.Bluetooth-Incoming-Port) або USB COM-порту.
"""

import socket
import sys
import time

def encode_nibbles(val: int) -> bytes:
    """Кодує ціле число у 4 байти за алгоритмом Datecs Nibble Encoding (hex_digit + 0x30)."""
    hex_str = f"{val & 0xFFFF:04X}"
    return bytes([(int(c, 16) + 0x30) for c in hex_str])

def decode_nibbles(raw_bytes: bytes) -> int:
    """Декодує 4 нібл-байти Datecs назад у ціле число."""
    hex_chars = [f"{(b - 0x30):X}" for b in raw_bytes]
    return int("".join(hex_chars), 16)

class DatecsTcpClient:
    def __init__(self, host: str = "127.0.0.1", port: int = 4999):
        self.host = host
        self.port = port
        self.sock = None
        self.seq = 0x40  # '@'

    def connect(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.settimeout(3.0)
        self.sock.connect((self.host, self.port))
        print(f"Підключено до РРО за адресою {self.host}:{self.port}")

    def next_seq(self) -> int:
        seq = self.seq
        self.seq = 0x40 if self.seq >= 0x7C else self.seq + 1
        return seq

    def send_command(self, cmd_code: int, params: list = None) -> dict:
        if params is None:
            params = []
        seq = self.next_seq()
        cmd_bytes = encode_nibbles(cmd_code)
        
        # Дані через табуляцію
        data_str = "\t".join(params) + ("\t" if params else "")
        data_bytes = data_str.encode("cp1251")

        # LEN = (SEQ + CMD + DATA + ENQ) + 0x20
        length_val = (1 + 4 + len(data_bytes) + 1) + 0x20
        len_bytes = encode_nibbles(length_val)

        # Тіло кадру
        body = len_bytes + bytes([seq]) + cmd_bytes + data_bytes + b"\x05"
        bcc_sum = sum(body) & 0xFFFF
        bcc_bytes = encode_nibbles(bcc_sum)

        full_frame = b"\x01" + body + bcc_bytes + b"\x03"
        self.sock.sendall(full_frame)

        # Зчитування відповіді
        while True:
            b = self.sock.recv(1)
            if not b:
                raise ConnectionError("З'єднання з РРО розірвано")
            if b == b"\x16":
                # SYN: пристрій зайнятий
                time.sleep(0.06)
                continue
            if b == b"\x15":
                raise ValueError("РРО повернув NAK (помилка BCC)")
            if b == b"\x01":
                break

        len_resp = self.sock.recv(4)
        resp_seq = self.sock.recv(1)[0]
        resp_cmd = self.sock.recv(4)

        # Дані до 0x04
        resp_data = bytearray()
        while True:
            ch = self.sock.recv(1)
            if ch == b"\x04" or not ch:
                break
            resp_data.extend(ch)

        # 8 байтів статусу
        status_bytes = self.sock.recv(8)
        enq = self.sock.recv(1)
        resp_bcc = self.sock.recv(4)
        etx = self.sock.recv(1)

        data_text = resp_data.decode("cp1251", errors="replace")
        tokens = [t for t in data_text.split("\t") if t]
        error_code = int(tokens[0]) if tokens and tokens[0].lstrip("-").isdigit() else 0

        return {
            "error_code": error_code,
            "tokens": tokens[1:],
            "status_raw": status_bytes,
            "paper_end": bool(status_bytes[2] & 0x01),
            "cover_open": bool(status_bytes[0] & 0x40),
            "blocked_24h": bool(status_bytes[1] & 0x01),
            "fiscalized": bool(status_bytes[5] & 0x08),
        }

    def close(self):
        if self.sock:
            self.sock.close()

if __name__ == "__main__":
    client = DatecsTcpClient("127.0.0.1", 4999)
    try:
        client.connect()
        # 1. Запит статусу (Команда 74 / 0x4A)
        res = client.send_command(0x4A, ["0"])
        print("Статус РРО:", res)

        # 2. Зчитування часу RTC (Команда 62 / 0x3E)
        time_res = client.send_command(0x3E)
        print("Час у касі:", time_res.get("tokens"))
    finally:
        client.close()
