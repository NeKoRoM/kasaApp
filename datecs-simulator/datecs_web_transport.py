"""
Datecs / Exellio Web Dashboard & Monitoring Transport.
Provides real-time visual thermal receipt tape, VFD customer display,
and interactive hardware controls via an integrated web server.
"""

import asyncio
import json
import logging
import os
from datecs_contract import CommandCode
from datecs_dispatcher_logic import DatecsCommandDispatcher
from datecs_engine_logic import DatecsFiscalEngine

logger = logging.getLogger("DatecsWebTransport")


class DatecsWebServer:
    """Lightweight HTTP server providing status dashboard and REST API."""

    def __init__(
        self,
        engine: DatecsFiscalEngine,
        dispatcher: DatecsCommandDispatcher,
        host: str = "0.0.0.0",
        port: int = 8088,
    ) -> None:
        self.engine = engine
        self.dispatcher = dispatcher
        self.host = host
        self.port = port
        self._server: asyncio.AbstractServer | None = None
        self._running = False
        self._html_cache: bytes | None = None

    def _get_html(self) -> bytes:
        if self._html_cache is None:
            html_path = os.path.join(os.path.dirname(__file__), "datecs_dashboard.html")
            try:
                with open(html_path, "rb") as f:
                    self._html_cache = f.read()
            except OSError:
                self._html_cache = b"<h1>Datecs Simulator Dashboard</h1>"
        return self._html_cache

    async def start(self) -> None:
        self._running = True
        self._server = await asyncio.start_server(
            self._handle_http_client, self.host, self.port
        )
        addrs = ", ".join(str(sock.getsockname()) for sock in self._server.sockets)
        print(f"[*] Datecs Web Dashboard активний: http://localhost:{self.port} (на {addrs})")

    async def stop(self) -> None:
        self._running = False
        if self._server:
            self._server.close()
            await self._server.wait_closed()
            print("[*] Datecs Web Dashboard зупинено.")

    async def _handle_http_client(
        self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter
    ) -> None:
        try:
            line = await reader.readline()
            if not line:
                return

            req_line = line.decode("latin1", errors="replace").strip()
            parts = req_line.split(" ")
            if len(parts) < 2:
                return
            method, path = parts[0], parts[1]

            # Read remaining headers
            while True:
                h_line = await reader.readline()
                if not h_line or h_line == b"\r\n":
                    break

            if path == "/" or path.startswith("/index"):
                body = self._get_html()
                writer.write(b"HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n")
                writer.write(f"Content-Length: {len(body)}\r\nConnection: close\r\n\r\n".encode())
                writer.write(body)
            elif path == "/api/state":
                state_data = {
                    "serial_number": self.engine.serial_number,
                    "fiscal_number": self.engine.fiscal_number,
                    "cash_in_safe": self.engine.cash_in_safe,
                    "z_report_counter": self.engine.z_report_counter,
                    "slip_counter": self.engine.slip_counter,
                    "is_receipt_open": self.engine.current_receipt.is_open,
                    "display_line1": self.engine.tape.display_line1,
                    "display_line2": self.engine.tape.display_line2,
                    "paper_present": self.engine.paper_present,
                    "cover_open": self.engine.cover_open,
                }
                body = json.dumps(state_data).encode("utf-8")
                writer.write(b"HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\n")
                writer.write(f"Content-Length: {len(body)}\r\nConnection: close\r\n\r\n".encode())
                writer.write(body)
            elif path == "/api/tape":
                body = self.engine.tape.get_recent_tape(max_lines=150).encode("utf-8")
                writer.write(b"HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n")
                writer.write(f"Content-Length: {len(body)}\r\nConnection: close\r\n\r\n".encode())
                writer.write(body)
            elif path.startswith("/api/action") and method == "POST":
                self._handle_action(path)
                writer.write(b"HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n")
                writer.write(b"Content-Length: 15\r\nConnection: close\r\n\r\n{\"status\":\"ok\"}")
            elif path == "/favicon.ico":
                writer.write(b"HTTP/1.1 204 No Content\r\nContent-Length: 0\r\nConnection: close\r\n\r\n")
            else:
                writer.write(b"HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n")

            await writer.drain()
        except Exception as e:
            logger.error(f"Помилка HTTP з'єднання: {e}")
        finally:
            try:
                writer.close()
                await writer.wait_closed()
            except (OSError, asyncio.CancelledError):
                pass

    def _handle_action(self, path: str) -> None:
        cmd = path.split("cmd=")[-1] if "cmd=" in path else ""
        if cmd == "test_receipt":
            self.dispatcher.dispatch(CommandCode.OPEN_FISCAL, "1\t1\t1\t")
            self.dispatcher.dispatch(CommandCode.REGISTER_SALE, "Кава Лате\t1\t48.00\t1.0\t0\t0\t1\t")
            self.dispatcher.dispatch(CommandCode.REGISTER_SALE, "Круасан мигдалевий\t1\t55.00\t1.0\t0\t0\t1\t")
            self.dispatcher.dispatch(CommandCode.SUBTOTAL, "1\t0\t0\t0\t")
            self.dispatcher.dispatch(CommandCode.TOTAL_PAYMENT, "0\t150.00\t")
            self.dispatcher.dispatch(CommandCode.CLOSE_FISCAL, "")
        elif cmd == "x_report":
            self.dispatcher.dispatch(CommandCode.REPORTS, "X\t")
        elif cmd == "z_report":
            self.dispatcher.dispatch(CommandCode.REPORTS, "Z\t")
        elif cmd == "cash_in":
            self.dispatcher.dispatch(CommandCode.CASH_IN_OUT, "0\t500.00\t")
        elif cmd == "open_drawer":
            self.dispatcher.dispatch(CommandCode.DRAWER_OPEN, "")
        elif cmd == "toggle_paper":
            self.engine.paper_present = not self.engine.paper_present
        elif cmd == "toggle_cover":
            self.engine.cover_open = not self.engine.cover_open
        elif cmd == "clear_tape":
            self.engine.tape.tape_history.clear()
            self.engine.tape.current_receipt_lines.clear()
