"""
Datecs / Exellio Fiscal Printer Simulator - Main Entry Point.
Protocol version: 2.11 (Eng).
"""

import argparse
import asyncio
import os
import signal
import sys
from datecs_dispatcher_logic import DatecsCommandDispatcher
from datecs_engine_logic import DatecsFiscalEngine
from datecs_serial_transport import DatecsSerialServer
from datecs_socket_transport import DatecsSocketServer
from datecs_tape_logic import DatecsTapeFormatter
from datecs_web_transport import DatecsWebServer


def print_banner(host: str, tcp_port: int, web_port: int, pty_enabled: bool) -> None:
    print("=" * 64)
    print("  🚀 DATECS / EXELLIO FISCAL PRINTER SIMULATOR v2.11")
    print("=" * 64)
    print(f"  • TCP Сервер:       {host}:{tcp_port} (Стандартний порт РРО)")
    if pty_enabled and sys.platform != "win32":
        symlink_path = os.path.abspath("./kasa_serial_port")
        print(f"  • Virtual Serial:   {symlink_path} (PTY COM-порт)")
    print(f"  • Веб-дашборд:      http://localhost:{web_port}")
    print("  • Кодування тексту: Windows-1251 (CP1251) / UTF-8")
    print("=" * 64)
    print("  Підключення з POS додатку:")
    print(f"    - По мережі (TCP/IP): host='127.0.0.1', port={tcp_port}")
    print("    - Через віртуальний COM-порт: './kasa_serial_port'")
    print("    - Перевірка через netcat: nc 127.0.0.1 4999")
    print("  Натисніть Ctrl+C для зупинки.")
    print("=" * 64 + "\n")


async def main() -> None:
    parser = argparse.ArgumentParser(
        description="Datecs / Exellio Fiscal Printer Protocol v2.11 Simulator"
    )
    parser.add_argument("--host", default="0.0.0.0", help="Host IP для TCP сервера (default: 0.0.0.0)")
    parser.add_argument("--port", type=int, default=4999, help="TCP порт РРО (default: 4999)")
    parser.add_argument("--web-port", type=int, default=8088, help="Порт веб-дашборду (default: 8088)")
    parser.add_argument("--no-web", action="store_true", help="Вимкнути веб-дашборд")
    parser.add_argument("--no-pty", action="store_true", help="Вимкнути створення віртуального PTY порту")
    parser.add_argument("--pty-path", default="./kasa_serial_port", help="Шлях до симлінку PTY (default: ./kasa_serial_port)")

    args = parser.parse_args()

    # Core instances
    tape = DatecsTapeFormatter()
    engine = DatecsFiscalEngine(tape_formatter=tape)
    dispatcher = DatecsCommandDispatcher(engine=engine)

    # Transports
    socket_server = DatecsSocketServer(
        engine=engine, dispatcher=dispatcher, host=args.host, port=args.port
    )

    serial_server = None
    if not args.no_pty and sys.platform != "win32":
        serial_server = DatecsSerialServer(
            engine=engine, dispatcher=dispatcher, symlink_path=args.pty_path
        )

    web_server = None
    if not args.no_web:
        web_server = DatecsWebServer(
            engine=engine, dispatcher=dispatcher, host=args.host, port=args.web_port
        )

    # Start services
    await socket_server.start()
    if serial_server:
        serial_server.start()
    if web_server:
        await web_server.start()

    print_banner(
        host=args.host,
        tcp_port=args.port,
        web_port=args.web_port,
        pty_enabled=(serial_server is not None),
    )

    stop_event = asyncio.Event()

    def signal_handler() -> None:
        print("\n[*] Отримано сигнал зупинки...")
        stop_event.set()

    loop = asyncio.get_running_loop()
    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(sig, signal_handler)
        except NotImplementedError:
            pass

    try:
        await stop_event.wait()
    except asyncio.CancelledError:
        pass
    finally:
        print("[*] Зупинка сервісів симулятора...")
        if web_server:
            await web_server.stop()
        if serial_server:
            serial_server.stop()
        await socket_server.stop()
        print("[✓] Симулятор повністю завершив роботу.")


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
