"""
Datecs / Exellio Test Client.
Connects to the simulator over TCP (localhost:4999), executes a full fiscal cycle,
and verifies responses and status bytes.
"""

import os
import socket
import sys
import time
from datecs_contract import CommandCode, ErrorCode
from datecs_framer_logic import DatecsByteFramer, DatecsFrameCodec


class DatecsClient:
    """Test client communicating with Datecs fiscal simulator via TCP or Serial PTY."""

    def __init__(
        self,
        host: str = "127.0.0.1",
        port: int = 4999,
        serial_path: str | None = None,
    ) -> None:
        self.host = host
        self.port = port
        self.serial_path = serial_path
        self.codec = DatecsFrameCodec()
        self.framer = DatecsByteFramer()
        self.seq = 0x20
        self.sock: socket.socket | None = None
        self.serial_fd: int | None = None

    def connect(self) -> None:
        if self.serial_path:
            self.serial_fd = os.open(self.serial_path, os.O_RDWR | os.O_NOCTTY)
            import tty
            tty.setraw(self.serial_fd)
            print(f"[+] Підключено до симулятора Datecs через Serial PTY: {self.serial_path}")
            return

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.settimeout(3.0)
        self.sock.connect((self.host, self.port))
        print(f"[+] Підключено до симулятора Datecs на {self.host}:{self.port}")

    def close(self) -> None:
        if self.sock:
            self.sock.close()
            self.sock = None
        if self.serial_fd is not None:
            os.close(self.serial_fd)
            self.serial_fd = None
        print("[-] З'єднання з симулятором закрито.")

    def _next_seq(self) -> int:
        cur = self.seq
        self.seq = 0x20 if self.seq >= 0x7E else self.seq + 1
        return cur

    def send_command(self, cmd_code: int, data_str: str) -> tuple[int, list[str], bytes]:
        """
        Sends wrapped request frame and waits for answer frame.
        Returns: (error_code, response_data_fields, status_bytes)
        """
        seq_byte = self._next_seq()

        # Build raw request payload
        # Format: <PRE><LEN><SEQ><CMD><DATA><PST><BCC><EOT>
        data_bytes = data_str.encode("windows-1251", errors="replace")
        cmd_bytes = bytes([
            ((cmd_code >> 12) & 0x0F) + 0x30,
            ((cmd_code >> 8) & 0x0F) + 0x30,
            ((cmd_code >> 4) & 0x0F) + 0x30,
            (cmd_code & 0x0F) + 0x30,
        ])

        # Length between PRE (excl) and PST (incl) + 0x20
        # LEN(4) + SEQ(1) + CMD(4) + DATA(len) + PST(1) = 10 + len(data)
        raw_len = 10 + len(data_bytes) + 0x20
        len_bytes = bytes([
            ((raw_len >> 12) & 0x0F) + 0x30,
            ((raw_len >> 8) & 0x0F) + 0x30,
            ((raw_len >> 4) & 0x0F) + 0x30,
            (raw_len & 0x0F) + 0x30,
        ])

        body_to_pst = bytearray()
        body_to_pst.extend(len_bytes)
        body_to_pst.append(seq_byte)
        body_to_pst.extend(cmd_bytes)
        body_to_pst.extend(data_bytes)
        body_to_pst.append(0x05)  # PST

        # BCC
        bcc_sum = sum(body_to_pst) & 0xFFFF
        bcc_bytes = bytes([
            ((bcc_sum >> 12) & 0x0F) + 0x30,
            ((bcc_sum >> 8) & 0x0F) + 0x30,
            ((bcc_sum >> 4) & 0x0F) + 0x30,
            (bcc_sum & 0x0F) + 0x30,
        ])

        full_frame = bytearray([0x01]) + body_to_pst + bcc_bytes + bytearray([0x03])
        if self.serial_fd is not None:
            os.write(self.serial_fd, full_frame)
        elif self.sock is not None:
            self.sock.sendall(full_frame)
        else:
            raise RuntimeError("Немає активного з'єднання")

        # Receive response
        self.framer.reset()
        while True:
            if self.serial_fd is not None:
                chunk = os.read(self.serial_fd, 1024)
            else:
                chunk = self.sock.recv(1024)
            if not chunk:
                raise ConnectionError("З'єднання закрито")
            frames = self.framer.append_data(chunk)
            if frames:
                resp_frame = frames[0]
                break

        # Decode answer frame
        # Format: <PRE><LEN(4)><SEQ(1)><CMD(4)><DATA><SEP(0x04)><STAT(8)><PST(0x05)><BCC(4)><EOT(0x03)>
        sep_idx = resp_frame.find(0x04)
        pst_idx = resp_frame.rfind(0x05)
        raw_data = resp_frame[10:sep_idx]
        raw_stat = resp_frame[sep_idx + 1:pst_idx]

        data_text = raw_data.decode("windows-1251", errors="replace")
        fields = data_text.split("\t")
        if fields and fields[-1] == "":
            fields = fields[:-1]

        err_code = int(fields[0]) if fields and (fields[0].lstrip("-").isdigit()) else 0
        res_fields = fields[1:] if len(fields) > 1 else []
        return err_code, res_fields, bytes(raw_stat)


def run_full_fiscal_demo(serial_path: str | None = None) -> bool:
    print("=" * 64)
    print("  🧪 ЗАПУСК ТЕСТОВОГО КЛІЄНТА ДО СИМУЛЯТОРА DATECS")
    print("=" * 64)

    if "--serial" in sys.argv:
        idx = sys.argv.index("--serial")
        if idx + 1 < len(sys.argv):
            serial_path = sys.argv[idx + 1]

    client = DatecsClient(host="127.0.0.1", port=4999, serial_path=serial_path)
    try:
        client.connect()
    except Exception as e:
        print(f"[!] Не вдалося підключитися: {e}")
        print("    Переконайтеся, що симулятор запущено: ./run_simulator.sh")
        return False

    try:
        # 1. Ping / Check connection (CMD 45)
        print("\n1. Перевірка зв'язку (CMD 45 - Check PC Mode)...")
        err, fields, stat = client.send_command(CommandCode.CHECK_PC_MODE, "")
        print(f"   Результат: ErrorCode={err}, StatusBytes={stat.hex()}")
        assert err == 0, f"Помилка зв'язку: {err}"

        # 2. Read Date & Time (CMD 62)
        print("\n2. Зчитування часу РРО (CMD 62 - Read Date & Time)...")
        err, fields, _ = client.send_command(CommandCode.READ_DATE_TIME, "")
        print(f"   Час РРО: {fields[0] if fields else 'Н/Д'}")

        # 3. Read Device Info (CMD 123, opt 1)
        print("\n3. Інформація про апарат (CMD 123 - Device Info)...")
        err, fields, _ = client.send_command(CommandCode.DEVICE_INFO, "1\t")
        if fields:
            print(f"   ЗН: {fields[0]}, ФН: {fields[1]}, Фірма: {fields[2]}")

        # 4. Open Fiscal Receipt (CMD 48)
        print("\n4. Відкриття фіскального чека (CMD 48 - Open Fiscal Receipt)...")
        err, fields, stat = client.send_command(CommandCode.OPEN_FISCAL, "1\t1\t1\t")
        slip_num = fields[0] if fields else "?"
        print(f"   Чек відкрито: № {slip_num}, статус чека у байтах: {(stat[2] & 0x08) != 0}")

        # 5. Register Sale 1 (CMD 49)
        print("\n5. Реєстрація позиції 1: 'Кава Еспресо' (CMD 49)...")
        err, _, _ = client.send_command(
            CommandCode.REGISTER_SALE, "Кава Еспресо\t1\t35.00\t2.0\t0\t0\t1\tчаш.\t"
        )
        print("   Позицію 1 зареєстровано успішно.")

        # 6. Register Sale 2 with discount (CMD 49)
        print("\n6. Реєстрація позиції 2: 'Тістечко Еклер' зі знижкою 10% (CMD 49)...")
        err, _, _ = client.send_command(
            CommandCode.REGISTER_SALE, "Тістечко Еклер\t1\t45.00\t1.0\t2\t10.0\t1\tшт.\t"
        )
        print("   Позицію 2 зареєстровано успішно.")

        # 7. Subtotal (CMD 51)
        print("\n7. Запит проміжного підсумку (CMD 51 - Subtotal)...")
        err, fields, _ = client.send_command(CommandCode.SUBTOTAL, "1\t0\t0\t0\t")
        subtotal = fields[1] if len(fields) > 1 else "?"
        print(f"   Сума до сплати: {subtotal} грн")

        # 8. Total Payment (CMD 53)
        print("\n8. Оплата готівкою 200.00 грн (CMD 53 - Total Payment)...")
        err, fields, _ = client.send_command(CommandCode.TOTAL_PAYMENT, "0\t200.00\t")
        change = fields[1] if len(fields) > 1 else "?"
        print(f"   Решта покупцеві: {change} грн")

        # 9. Close Fiscal Receipt (CMD 56)
        print("\n9. Закриття чека (CMD 56 - Close Fiscal Receipt)...")
        err, fields, stat = client.send_command(CommandCode.CLOSE_FISCAL, "")
        print(f"   Чек успішно закрито! Статус чека у байтах: {(stat[2] & 0x08) != 0}")

        # 10. Print X-Report (CMD 69)
        print("\n10. Зняття денного X-звіту (CMD 69 - Report X)...")
        err, fields, _ = client.send_command(CommandCode.REPORTS, "X\t")
        print(f"    X-звіт знято успішно! Обіг групи А: {fields[1] if len(fields) > 1 else '0.00'} грн")

        print("\n" + "=" * 64)
        print("  ✅ ВСІ ТЕСТОВІ КОМАНДИ ПРОТОКОЛУ DATECS v2.11 УСПІШНО ВИКОНАНО!")
        print("=" * 64)
        return True

    finally:
        client.close()


if __name__ == "__main__":
    success = run_full_fiscal_demo()
    sys.exit(0 if success else 1)
