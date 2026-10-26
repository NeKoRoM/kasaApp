"""
Datecs / Exellio Fiscal Printer Protocol v2.11 - Data Contracts and Ontology.

Defines protocol byte markers, command codes, status bit masks, error codes,
and state structures for the fiscal simulator.
"""

from dataclasses import dataclass, field
from enum import IntEnum
from typing import TypedDict


# --- PROTOCOL BYTE CONSTANTS ---
class ProtocolBytes(IntEnum):
    PRE = 0x01  # Preamble (SOH)
    PST = 0x05  # Postamble (ENQ)
    EOT = 0x03  # Terminator (ETX)
    SEP = 0x04  # Separator before status bytes
    NAK = 0x15  # Negative Acknowledge
    SYN = 0x16  # Synchronous Idle (busy/processing)
    TAB = 0x09  # Parameter delimiter (\t)


# --- PROTOCOL COMMAND CODES (Decimal & Hex equivalents) ---
class CommandCode(IntEnum):
    CLEAR_DISPLAY = 33       # 0x21
    DISPLAY_LINE2 = 35       # 0x23
    OPEN_NON_FISCAL = 38     # 0x26
    CLOSE_NON_FISCAL = 39    # 0x27
    PRINT_NON_FISCAL = 42    # 0x2A
    OPEN_STORNO = 43         # 0x2B
    PAPER_FEED = 44          # 0x2C
    CHECK_PC_MODE = 45       # 0x2D
    PAPER_CUT = 46           # 0x2E
    DISPLAY_LINE1 = 47       # 0x2F
    OPEN_FISCAL = 48         # 0x30
    REGISTER_SALE = 49       # 0x31
    GET_VAT_RATES = 50       # 0x32
    SUBTOTAL = 51            # 0x33
    TOTAL_PAYMENT = 53       # 0x35
    PRINT_FISCAL_TEXT = 54   # 0x36
    PINPAD_CMD = 55          # 0x37
    CLOSE_FISCAL = 56        # 0x38
    INVOICE_DATA = 57        # 0x39
    SALE_PLU = 58            # 0x3A
    CANCEL_RECEIPT = 60      # 0x3C
    SET_DATE_TIME = 61       # 0x3D
    READ_DATE_TIME = 62      # 0x3E
    SHOW_DATE_TIME = 63      # 0x3F
    LAST_FISCAL_ENTRY = 64   # 0x40
    DAILY_TAXATION = 65      # 0x41
    INVOICE_INTERVAL = 66    # 0x42
    REMAINING_Z_REPORTS = 68 # 0x44
    REPORTS = 69             # 0x45 (X, Z, D, G, P)
    CASH_IN_OUT = 70         # 0x46
    MODEM_TEST = 71          # 0x47
    FISCALIZATION = 72       # 0x48
    READ_STATUS = 74         # 0x4A
    TRANSACTION_STATUS = 76  # 0x4C
    PLAY_SOUND = 80          # 0x50
    PROGRAM_VAT = 83         # 0x53
    PRINT_BARCODE = 84       # 0x54
    LAST_FM_DATE = 86        # 0x56
    ITEM_GROUPS_INFO = 87    # 0x57
    DEPARTMENT_INFO = 88     # 0x58
    TEST_FM = 89             # 0x59
    DIAGNOSTICS = 90         # 0x5A
    PROGRAM_SERIAL_FM = 91   # 0x5B
    SEPARATING_LINE = 92     # 0x5C
    FM_REPORT_DATE = 94      # 0x5E
    FM_REPORT_Z = 95         # 0x5F
    SET_SOFT_PASSWORD = 96   # 0x60
    PROGRAM_TAX_NUM = 98     # 0x62
    READ_TAX_NUM = 99        # 0x63
    READ_ERROR_MSG = 100     # 0x64
    SET_OPER_PASSWORD = 101  # 0x65
    CURRENT_RECEIPT_INFO = 103 # 0x67
    OPERATOR_REPORT = 105    # 0x69
    DRAWER_OPEN = 106        # 0x6A
    DEFINE_READ_PLU = 107    # 0x6B
    PRINT_DUPLICATE = 109    # 0x6D
    DAILY_INFO = 110         # 0x6E
    PLU_REPORT = 111         # 0x6F
    OPERATOR_INFO = 112      # 0x70
    CONVERT_CURRENCY = 115   # 0x73
    READ_FM_BINARY = 116     # 0x74
    VERTICAL_FISCAL_TEXT = 122 # 0x7A
    DEVICE_INFO = 123        # 0x7B
    SEARCH_RECEIPT = 124     # 0x7C
    EJ_INFO = 125            # 0x7D
    FM_STRUCTURED_INFO = 126 # 0x7E
    STAMP_OPS = 127          # 0x7F
    MODEM_INFO = 135         # 0x87
    CLIENTS_OPS = 140        # 0x8C
    CUSTOMER_LOGO = 202      # 0xCA
    STAMP_LOGO = 203         # 0xCB
    SERVICE_OPS = 253        # 0xFD
    PROGRAMMING = 255        # 0xFF


# --- STANDARD PROTOCOL ERROR CODES ---
class ErrorCode(IntEnum):
    OK = 0
    ERR_IO = -100001
    ERR_CHECKSUM = -100002
    ERR_NOT_READY = -100008
    ERR_NOTHING_TO_PRINT = -100009
    ERR_FM_FULL = -100114
    ERR_BAD_INPUT = -101004
    ERR_NOT_POSSIBLE = -101006
    ERR_CANCELLED = -101009
    ERR_NO_PERMISSION = -101017
    ERR_OPER_WRONG_PASSWORD = -102002
    ERR_VAT_RATES_EMPTY = -102009
    ERR_HEADER_EMPTY = -102010
    ERR_PLU_NOT_FOUND = -103000
    ERR_PLU_EXISTS = -103001
    ERR_NEED_Z_REPORT = -104000
    ERR_NOT_ENOUGH_CASH = -110814
    ERR_OPEN_BON = -111015
    ERR_CLOSED_BON = -111016
    ERR_PAY_NOCASH = -111017
    ERR_PAY_STARTED = -111018
    ERR_AMOUNT_BIGGER = -111063
    ERR_AMOUNT_SMALLER = -111064
    ERR_ZERO_BILLAMOUNT = -111065
    ERR_ALL_VOID_EXECUTED = -111066
    ERR_OPEN_STORNOBON = -111067
    ERR_OPEN_NONFISCALBON = -111046
    ERR_OPEN_FISCALBON = -111047
    ERR_INVALID_COMMAND = -112000
    ERR_INVALID_SYNTAX = -112001
    ERR_COMMAND_NOT_PERMITTED = -112002
    ERR_OVERFLOW = -112003


# --- DATA STRUCTURES ---
@dataclass
class PluRecord:
    plu_code: int
    name: str
    price: float
    tax_group: str = "Б"
    department: int = 1
    item_group: int = 1
    price_type: int = 0
    quantity: float = 100.0
    turnover: float = 0.0
    sold_qty: float = 0.0
    barcodes: list[str] = field(default_factory=list)
    unit_id: int = 0


@dataclass
class ReceiptSaleItem:
    name: str
    price: float
    quantity: float
    tax_group: str
    discount_type: int = 0
    discount_value: float = 0.0
    department: int = 0
    unit: str = "бр."

    @property
    def total_before_discount(self) -> float:
        return round(self.price * self.quantity, 2)

    @property
    def discount_amount(self) -> float:
        if self.discount_type == 1:  # Surcharge %
            return round(-self.total_before_discount * (self.discount_value / 100.0), 2)
        if self.discount_type == 2:  # Discount %
            return round(self.total_before_discount * (self.discount_value / 100.0), 2)
        if self.discount_type == 3:  # Surcharge sum
            return round(-self.discount_value, 2)
        if self.discount_type == 4:  # Discount sum
            return round(self.discount_value, 2)
        return 0.0

    @property
    def final_total(self) -> float:
        return max(0.0, round(self.total_before_discount - self.discount_amount, 2))


@dataclass
class ActiveReceiptState:
    is_open: bool = False
    receipt_type: int = 1  # 1 = Sale, 2 = Refund/Storno
    operator_id: int = 1
    slip_number: int = 1
    invoice_number: str = ""
    is_storno: bool = False
    items: list[ReceiptSaleItem] = field(default_factory=list)
    subtotal_discount_type: int = 0
    subtotal_discount_val: float = 0.0
    paid_amounts: dict[int, float] = field(default_factory=dict)
    opened_at: str = ""

    def calculate_subtotal(self) -> float:
        raw_sum = sum(item.final_total for item in self.items)
        if self.subtotal_discount_type == 1:
            raw_sum += raw_sum * (self.subtotal_discount_val / 100.0)
        elif self.subtotal_discount_type == 2:
            raw_sum -= raw_sum * (self.subtotal_discount_val / 100.0)
        elif self.subtotal_discount_type == 3:
            raw_sum += self.subtotal_discount_val
        elif self.subtotal_discount_type == 4:
            raw_sum -= self.subtotal_discount_val
        return max(0.0, round(raw_sum, 2))

    def total_paid(self) -> float:
        return round(sum(self.paid_amounts.values()), 2)


@dataclass
class DatecsFrame:
    sequence: int
    command: int
    data_text: str
    raw_payload: bytes


class DeviceStatusBytes(TypedDict):
    b0_general: int
    b1_general: int
    b2_receipt: int
    b3_reserved: int
    b4_fiscal_mem: int
    b5_fiscal_state: int
    b6_reserved: int
    b7_reserved: int
