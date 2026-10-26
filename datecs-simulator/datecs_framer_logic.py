"""
Datecs / Exellio Protocol v2.11 Framing, Nibble Encoding & BCC Calculation.
Handles serialization and deserialization of binary frames.
"""

from datecs_contract import DatecsFrame, ProtocolBytes


def encode_datecs_nibbles(value: int, count: int = 4) -> bytes:
    """
    Encodes an integer into Datecs Nibble ASCII-Hex format.
    Each 4-bit nibble is incremented by 0x30 ('0'):
    0..9 -> '0'..'9', 10..15 -> ':'..'?'
    """
    nibbles = bytearray(count)
    for i in range(count):
        shift = 4 * (count - 1 - i)
        nibble = (value >> shift) & 0x0F
        nibbles[i] = nibble + 0x30
    return bytes(nibbles)


def decode_datecs_nibbles(nibble_bytes: bytes) -> int:
    """
    Decodes Datecs Nibble ASCII-Hex bytes into an integer.
    """
    result = 0
    for b in nibble_bytes:
        nibble = (b - 0x30) & 0x0F
        result = (result << 4) | nibble
    return result


def calculate_bcc(bytes_from_len_to_pst: bytes) -> bytes:
    """
    Calculates 4-byte Datecs BCC control sum over all bytes from
    the start of LEN to PST inclusive: sum & 0xFFFF encoded with nibble encoding.
    """
    sum_val = sum(bytes_from_len_to_pst) & 0xFFFF
    return encode_datecs_nibbles(sum_val, count=4)


def verify_bcc(bytes_from_len_to_pst: bytes, expected_bcc_bytes: bytes) -> bool:
    """Verifies that the provided BCC bytes match the calculated BCC."""
    if len(expected_bcc_bytes) != 4:
        return False
    calculated = calculate_bcc(bytes_from_len_to_pst)
    return calculated == expected_bcc_bytes


class DatecsByteFramer:
    """
    Streaming buffer that extracts complete Datecs frames (PRE..EOT).
    Handles TCP socket chunking and serial stream fragmentation.
    """

    def __init__(self) -> None:
        self._buffer = bytearray()

    def append_data(self, incoming: bytes) -> list[bytes]:
        """Appends raw bytes and extracts all complete frames."""
        if not incoming:
            return []

        self._buffer.extend(incoming)
        complete_frames: list[bytes] = []

        while True:
            # Find start marker PRE (0x01)
            soh_idx = self._buffer.find(ProtocolBytes.PRE)
            if soh_idx == -1:
                # No start marker found, discard garbage
                self._buffer.clear()
                break

            # Discard bytes before PRE
            if soh_idx > 0:
                del self._buffer[:soh_idx]

            # Find terminator EOT (0x03) after PRE
            eot_idx = self._buffer.find(ProtocolBytes.EOT, 1)
            if eot_idx == -1:
                # Frame incomplete, await further incoming bytes
                break

            # Extract full frame PRE..EOT
            frame_len = eot_idx + 1
            complete_frame = bytes(self._buffer[:frame_len])
            del self._buffer[:frame_len]
            complete_frames.append(complete_frame)

        return complete_frames

    def reset(self) -> None:
        """Clears the internal accumulator."""
        self._buffer.clear()


class DatecsFrameCodec:
    """
    Encodes and decodes Datecs protocol frames using CP1251 text encoding.
    """

    def __init__(self, encoding: str = "windows-1251") -> None:
        self.encoding = encoding

    def decode_request(self, raw_frame: bytes) -> tuple[DatecsFrame | None, int | None]:
        """
        Decodes a raw request frame from host to fiscal device.
        Format: <PRE><LEN(4)><SEQ(1)><CMD(4)><DATA(0..496)><PST><BCC(4)><EOT>
        Returns: (DatecsFrame, None) on success, or (None, error_code) on failure.
        """
        if len(raw_frame) < 12:
            return None, -112001  # Syntax error

        if raw_frame[0] != ProtocolBytes.PRE or raw_frame[-1] != ProtocolBytes.EOT:
            return None, -112001

        # Check PST marker
        pst_idx = len(raw_frame) - 6
        if raw_frame[pst_idx] != ProtocolBytes.PST:
            return None, -112001

        bytes_len_to_pst = raw_frame[1:pst_idx + 1]
        expected_bcc = raw_frame[pst_idx + 1:pst_idx + 5]

        if not verify_bcc(bytes_len_to_pst, expected_bcc):
            return None, -100002  # Checksum error

        seq = raw_frame[5]
        cmd_code = decode_datecs_nibbles(raw_frame[6:10])

        raw_data = raw_frame[10:pst_idx]
        try:
            data_text = raw_data.decode(self.encoding)
        except UnicodeDecodeError:
            data_text = raw_data.decode("latin1", errors="replace")

        frame = DatecsFrame(
            sequence=seq,
            command=cmd_code,
            data_text=data_text,
            raw_payload=bytes(raw_data),
        )
        return frame, None

    def encode_response(
        self,
        sequence: int,
        command: int,
        error_code: int,
        data_fields: list[str],
        status_bytes: bytes,
    ) -> bytes:
        """
        Encodes a fiscal device answer frame.
        Format: <PRE><LEN(4)><SEQ(1)><CMD(4)><DATA><SEP(0x04)><STAT(8)><PST(0x05)><BCC(4)><EOT(0x03)>
        """
        # Format DATA field: ErrorCode\t + field1\t + field2\t...
        data_text_parts = [str(error_code)]
        data_text_parts.extend(data_fields)
        # Each parameter in Datecs must end with tab \t
        full_data_str = "\t".join(data_text_parts) + "\t"
        data_bytes = full_data_str.encode(self.encoding, errors="replace")

        # Command bytes (4 bytes)
        cmd_bytes = encode_datecs_nibbles(command, count=4)

        # Ensure status_bytes is exactly 8 bytes
        safe_stat = bytearray(8)
        for i in range(8):
            if i < len(status_bytes):
                safe_stat[i] = status_bytes[i] | 0x80
            else:
                safe_stat[i] = 0x80

        # Calculate message length: bytes between PRE (excl) and PST (incl) + 0x20
        # LEN(4) + SEQ(1) + CMD(4) + DATA(len) + SEP(1) + STAT(8) + PST(1) = 19 + len(DATA)
        raw_length = 4 + 1 + 4 + len(data_bytes) + 1 + 8 + 1
        len_with_offset = raw_length + 0x20
        len_bytes = encode_datecs_nibbles(len_with_offset, count=4)

        # Construct bytes between PRE and PST for BCC calculation
        body_to_pst = bytearray()
        body_to_pst.extend(len_bytes)
        body_to_pst.append(sequence & 0xFF)
        body_to_pst.extend(cmd_bytes)
        body_to_pst.extend(data_bytes)
        body_to_pst.append(ProtocolBytes.SEP)
        body_to_pst.extend(safe_stat)
        body_to_pst.append(ProtocolBytes.PST)

        # BCC calculation
        bcc_bytes = calculate_bcc(bytes(body_to_pst))

        # Assemble full frame
        frame = bytearray()
        frame.append(ProtocolBytes.PRE)
        frame.extend(body_to_pst)
        frame.extend(bcc_bytes)
        frame.append(ProtocolBytes.EOT)

        return bytes(frame)
