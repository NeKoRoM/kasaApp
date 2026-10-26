"""
Unit tests for Datecs Framer, Nibble Encoding, and BCC Calculation.
Verifies against official Datecs test vectors from PM_CommunicationProtocol_v2.11_eng.pdf.
"""

import unittest
from datecs_contract import ProtocolBytes
from datecs_framer_logic import (
    calculate_bcc,
    decode_datecs_nibbles,
    encode_datecs_nibbles,
    verify_bcc,
    DatecsByteFramer,
    DatecsFrameCodec,
)


class TestDatecsFramerLogic(unittest.TestCase):

    def test_nibble_encoding(self) -> None:
        # 0x002F -> '0', '0', '2', '?'
        encoded = encode_datecs_nibbles(0x002F, 4)
        self.assertEqual(encoded, b"002?")
        # Decode back
        decoded = decode_datecs_nibbles(encoded)
        self.assertEqual(decoded, 0x002F)

    def test_page8_request_example(self) -> None:
        # Binary log from Page 8:
        # 01 30 30 32 3F 24 30 30 32 3A 54 65 73 74 09 05 30 33 36 3F 03
        raw_req = bytes.fromhex("01 30 30 32 3F 24 30 30 32 3A 54 65 73 74 09 05 30 33 36 3F 03")

        # Verify BCC
        bytes_to_pst = raw_req[1:16]
        expected_bcc = raw_req[16:20]
        self.assertTrue(verify_bcc(bytes_to_pst, expected_bcc))
        self.assertEqual(calculate_bcc(bytes_to_pst), b"036?")

        # Test codec decoding
        codec = DatecsFrameCodec()
        frame, err = codec.decode_request(raw_req)
        self.assertIsNone(err)
        self.assertIsNotNone(frame)
        self.assertEqual(frame.sequence, 0x24)
        self.assertEqual(frame.command, 42)  # 0x2A = 42
        self.assertEqual(frame.data_text, "Test\t")

    def test_page9_answer_example(self) -> None:
        # Binary log Answer from Page 9:
        # 01 30 30 33 35 24 30 30 32 31 30 09 04 80 80 80 80 86 9A 80 80 05 30 36 31 31 03
        expected_ans = bytes.fromhex(
            "01 30 30 33 35 24 30 30 32 31 30 09 04 80 80 80 80 86 9A 80 80 05 30 36 31 31 03"
        )
        codec = DatecsFrameCodec()
        status_bytes = bytes.fromhex("80 80 80 80 86 9A 80 80")
        encoded_ans = codec.encode_response(
            sequence=0x24,
            command=33,  # 0x21
            error_code=0,
            data_fields=[],
            status_bytes=status_bytes,
        )
        self.assertEqual(encoded_ans, expected_ans)

    def test_page20_cyrillic_registration_sale(self) -> None:
        # Page 20 Request binary log:
        # 01 30 30 34 37 36 30 30 33 31 D2 EE EF E5 ED EE 20 F1 E8 F0 E5 ED E5 09 32 09 32 2E 36 35 09 33 09 32 09 35 09 32 09 05 30 3E 3E 31 03
        raw_req = bytes.fromhex(
            "01 30 30 34 37 36 30 30 33 31 D2 EE EF E5 ED EE 20 F1 E8 F0 E5 ED E5 09 32 09 32 2E 36 35 09 33 09 32 09 35 09 32 09 05 30 3E 3E 31 03"
        )
        codec = DatecsFrameCodec()
        frame, err = codec.decode_request(raw_req)
        self.assertIsNone(err)
        self.assertIsNotNone(frame)
        self.assertEqual(frame.sequence, 0x36)  # '6'
        self.assertEqual(frame.command, 49)    # 0x31 = 49 (Registration of sale)
        self.assertTrue(frame.data_text.startswith("Топено сирене\t2\t2.65\t3\t"))

    def test_stream_framer(self) -> None:
        framer = DatecsByteFramer()
        frame1 = bytes.fromhex("01 30 30 32 3F 24 30 30 32 3A 54 65 73 74 09 05 30 33 36 3F 03")
        # Split across two chunks
        frames = framer.append_data(frame1[:10])
        self.assertEqual(len(frames), 0)
        frames = framer.append_data(frame1[10:])
        self.assertEqual(len(frames), 1)
        self.assertEqual(frames[0], frame1)


if __name__ == "__main__":
    unittest.main()
