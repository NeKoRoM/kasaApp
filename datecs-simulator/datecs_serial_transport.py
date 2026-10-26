"""
Datecs / Exellio Virtual Serial Port (PTY) Transport.
Creates a pseudo-terminal pair on macOS/Linux for virtual COM/Serial communication.
"""

import os
import sys
import asyncio
import logging
from datecs_contract import ErrorCode
from datecs_dispatcher_logic import DatecsCommandDispatcher
from datecs_engine_logic import DatecsFiscalEngine
from datecs_framer_logic import DatecsByteFramer, DatecsFrameCodec

logger = logging.getLogger("DatecsSerialTransport")


class DatecsSerialServer:
    """Simulates a physical RS-232 / Bluetooth SPP serial port via Unix PTY."""

    def __init__(
        self,
        engine: DatecsFiscalEngine,
        dispatcher: DatecsCommandDispatcher,
        symlink_path: str = "./kasa_serial_port",
    ) -> None:
        self.engine = engine
        self.dispatcher = dispatcher
        self.symlink_path = symlink_path
        self.codec = DatecsFrameCodec()
        self.master_fd: int | None = None
        self.slave_fd: int | None = None
        self.slave_name: str = ""
        self._running = False
        self._framer = DatecsByteFramer()
        self.last_seq: int | None = None
        self.last_response: bytes | None = None

    def start(self) -> bool:
        if sys.platform == "win32":
            print("[!] Virtual PTY не підтримується на Windows. Використовуйте TCP порт 4999.")
            return False

        try:
            self.master_fd, self.slave_fd = os.openpty()
            import tty
            tty.setraw(self.master_fd)
            tty.setraw(self.slave_fd)
            try:
                import ctypes
                libc = ctypes.CDLL(None)
                ptsname_fn = libc.ptsname
                ptsname_fn.restype = ctypes.c_char_p
                ptsname_fn.argtypes = [ctypes.c_int]
                res = ptsname_fn(self.master_fd)
                self.slave_name = res.decode() if res else os.ttyname(self.slave_fd)
            except (AttributeError, OSError):
                self.slave_name = os.ttyname(self.slave_fd)

            # Create or update symlink for easy client access
            if os.path.islink(self.symlink_path) or os.path.exists(self.symlink_path):
                os.remove(self.symlink_path)
            os.symlink(self.slave_name, self.symlink_path)

            self._running = True
            loop = asyncio.get_event_loop()
            loop.add_reader(self.master_fd, self._on_master_readable)

            print(f"[*] Datecs Virtual Serial Port створено:")
            print(f"    -> Slave TTY:   {self.slave_name}")
            print(f"    -> Символьне посилання: {os.path.abspath(self.symlink_path)}")
            return True
        except OSError as e:
            logger.error(f"Помилка створення PTY serial port: {e}")
            return False

    def stop(self) -> None:
        self._running = False
        if self.master_fd is not None:
            try:
                loop = asyncio.get_event_loop()
                loop.remove_reader(self.master_fd)
                os.close(self.master_fd)
            except (OSError, RuntimeError):
                pass
            self.master_fd = None

        if self.slave_fd is not None:
            try:
                os.close(self.slave_fd)
            except OSError:
                pass
            self.slave_fd = None

        if os.path.islink(self.symlink_path):
            try:
                os.remove(self.symlink_path)
            except OSError:
                pass

        print("[*] Datecs Virtual Serial Port закрито.")

    def _on_master_readable(self) -> None:
        if not self._running or self.master_fd is None:
            return

        try:
            raw_chunk = os.read(self.master_fd, 1024)
            if not raw_chunk:
                return

            frames = self._framer.append_data(raw_chunk)
            for raw_frame in frames:
                response = self._process_frame(raw_frame)
                if response and self.master_fd is not None:
                    os.write(self.master_fd, response)

        except Exception as e:
            logger.error(f"Помилка читання з віртуального PTY: {e}")

    def _process_frame(self, raw_frame: bytes) -> bytes:
        frame, decode_err = self.codec.decode_request(raw_frame)

        if decode_err is not None or frame is None:
            status_bytes = self.engine.build_status_bytes(syntax_error=True)
            return self.codec.encode_response(
                sequence=0x20,
                command=0,
                error_code=decode_err or ErrorCode.ERR_INVALID_SYNTAX,
                data_fields=[],
                status_bytes=status_bytes,
            )

        if self.last_seq is not None and frame.sequence == self.last_seq and self.last_response:
            return self.last_response

        err_code, res_fields = self.dispatcher.dispatch(frame.command, frame.data_text)
        is_err = err_code != ErrorCode.OK
        status_bytes = self.engine.build_status_bytes(cmd_error=is_err)

        resp_bytes = self.codec.encode_response(
            sequence=frame.sequence,
            command=frame.command,
            error_code=err_code,
            data_fields=res_fields,
            status_bytes=status_bytes,
        )

        self.last_seq = frame.sequence
        self.last_response = resp_bytes
        return resp_bytes
