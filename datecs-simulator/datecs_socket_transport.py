"""
Datecs / Exellio TCP Socket Server Transport.
Listens on TCP port (default 4999) for POS network connections.
"""

import asyncio
import logging
from datecs_contract import ErrorCode
from datecs_dispatcher_logic import DatecsCommandDispatcher
from datecs_engine_logic import DatecsFiscalEngine
from datecs_framer_logic import DatecsByteFramer, DatecsFrameCodec

logger = logging.getLogger("DatecsSocketTransport")


class DatecsSocketServer:
    """Asyncio TCP server simulating a Datecs fiscal printer network interface."""

    def __init__(
        self,
        engine: DatecsFiscalEngine,
        dispatcher: DatecsCommandDispatcher,
        host: str = "0.0.0.0",
        port: int = 4999,
    ) -> None:
        self.engine = engine
        self.dispatcher = dispatcher
        self.host = host
        self.port = port
        self.codec = DatecsFrameCodec()
        self._server: asyncio.AbstractServer | None = None
        self._running = False

    async def start(self) -> None:
        self._running = True
        self._server = await asyncio.start_server(
            self._handle_client, self.host, self.port
        )
        addrs = ", ".join(str(sock.getsockname()) for sock in self._server.sockets)
        print(f"[*] Datecs TCP Server активний на {addrs} (Стандартний порт 4999)")

    async def stop(self) -> None:
        self._running = False
        if self._server:
            self._server.close()
            await self._server.wait_closed()
            print("[*] Datecs TCP Server зупинено.")

    async def _handle_client(
        self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter
    ) -> None:
        peer = writer.get_extra_info("peername")
        print(f"[+] Нове підключення клієнта: {peer}")
        framer = DatecsByteFramer()

        last_seq: int | None = None
        last_response: bytes | None = None

        try:
            while self._running:
                data = await reader.read(1024)
                if not data:
                    break

                frames = framer.append_data(data)
                for raw_frame in frames:
                    response_bytes = self._process_frame(
                        raw_frame, last_seq, last_response
                    )
                    if response_bytes:
                        writer.write(response_bytes)
                        await writer.drain()

                        # Update last sequence cache
                        decoded, _ = self.codec.decode_request(raw_frame)
                        if decoded:
                            last_seq = decoded.sequence
                            last_response = response_bytes

        except (asyncio.IncompleteReadError, ConnectionResetError, BrokenPipeError):
            pass
        except Exception as e:
            logger.error(f"Помилка обробки клієнта {peer}: {e}")
        finally:
            try:
                writer.close()
                await writer.wait_closed()
            except (OSError, asyncio.CancelledError):
                pass
            print(f"[-] Клієнт відключився: {peer}")

    def _process_frame(
        self,
        raw_frame: bytes,
        last_seq: Optional[int],
        last_response: Optional[bytes],
    ) -> bytes:
        frame, decode_err = self.codec.decode_request(raw_frame)

        # Checksum or framing error -> return NAK or error frame
        if decode_err is not None or frame is None:
            status_bytes = self.engine.build_status_bytes(syntax_error=True)
            return self.codec.encode_response(
                sequence=0x20,
                command=0,
                error_code=decode_err or ErrorCode.ERR_INVALID_SYNTAX,
                data_fields=[],
                status_bytes=status_bytes,
            )

        # Duplicate sequence handling per Datecs spec
        if last_seq is not None and frame.sequence == last_seq and last_response:
            print(f"[REPEAT] Повтор послідовності SEQ={frame.sequence:#x}, відправка кешованої відповіді")
            return last_response

        # Execute command
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

        # Console logging
        cmd_hex = f"{frame.command:#06x} ({frame.command})"
        clean_req = frame.data_text.replace("\t", " | ").strip()
        clean_res = " | ".join([str(err_code)] + res_fields)
        print(f"[CMD {cmd_hex}] REQ: [{clean_req}] -> RESP: [{clean_res}]")

        return resp_bytes
