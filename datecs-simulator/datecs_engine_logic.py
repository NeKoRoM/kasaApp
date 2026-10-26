"""
Datecs / Exellio Fiscal Engine Logic.
Manages ECR state machine, shift registers, PLU storage, and status bits.
"""

from datetime import datetime
from datecs_contract import (
    ActiveReceiptState,
    ErrorCode,
    PluRecord,
    ReceiptSaleItem,
)
from datecs_reports_logic import DatecsReportsManager
from datecs_tape_logic import DatecsTapeFormatter


class DatecsFiscalEngine:
    """Simulates internal registers and business rules of a Datecs fiscal printer."""

    def __init__(self, tape_formatter: DatecsTapeFormatter | None = None) -> None:
        self.tape = tape_formatter or DatecsTapeFormatter()

        # Device Identity
        self.serial_number: str = "DT636555"
        self.fiscal_number: str = "02636555"
        self.tax_number: str = "000713391"
        self.device_model: str = "WP-50X"
        self.firmware_rev: str = "261216"
        self.country: str = "UA"

        # Headers and Footers (Index 0..9)
        self.headers: list[str] = [
            "ТОВ \"КАТРАН-ТРЕЙД\"",
            "м. Київ, вул. Хрещатик, 1",
            "Магазин № 1 'Каса'",
            "Відділ роздрібного продажу",
            "", "", "", "", "", ""
        ]
        self.footers: list[str] = ["Дякуємо за покупку!", "Завітайте до нас ще!", "", "", ""]

        # VAT Rates (%): Ukrainian standard
        self.tax_rates: dict[str, float] = {
            "А": 20.0, "Б": 0.0, "В": 7.0, "Г": 0.0, "Д": 0.0,
            "Е": 0.0, "Ж": 0.0, "З": 0.0,
        }

        # Daily Fiscal Registers
        self.z_report_counter: int = 1
        self.remaining_z_reports: int = 3644
        self.slip_counter: int = 470
        self.fiscal_receipt_counter: int = 268
        self.cash_in_safe: float = 1000.0
        self.daily_cash_in: float = 1000.0
        self.daily_cash_out: float = 0.0

        # Accumulated sales by tax group for the current shift
        self.daily_sales_by_tax: dict[str, float] = {k: 0.0 for k in self.tax_rates}
        self.daily_storno_by_tax: dict[str, float] = {k: 0.0 for k in self.tax_rates}
        self.daily_sales_count: int = 0
        self.daily_storno_count: int = 0

        # Active Receipt
        self.current_receipt = ActiveReceiptState()
        self.non_fiscal_open: bool = False
        self.last_qr_string: str = ""

        # PLU Database: plu_code -> PluRecord
        self.plu_database: dict[int, PluRecord] = {}
        self._init_default_plus()

        # Hardware simulation flags
        self.paper_present: bool = True
        self.cover_open: bool = False

    def _init_default_plus(self) -> None:
        self.plu_database[1] = PluRecord(1, "Кава Американо", 45.00, "А", 1, 1, 0, 99.0)
        self.plu_database[2] = PluRecord(2, "Круасан класичний", 38.50, "А", 1, 1, 0, 50.0)
        self.plu_database[3] = PluRecord(3, "Хліб пшеничний", 24.00, "Б", 1, 1, 0, 30.0)

    def get_current_datetime_str(self) -> str:
        return datetime.now().strftime("%d-%m-%y %H:%M:%S DST")

    def build_status_bytes(self, cmd_error: bool = False, syntax_error: bool = False) -> bytes:
        b0 = 0x80
        if self.cover_open:
            b0 |= 0x40 | 0x20
        if cmd_error:
            b0 |= 0x02 | 0x20
        if syntax_error:
            b0 |= 0x01 | 0x20

        b1 = 0x80 | (0x02 if cmd_error else 0x00)
        b2 = 0x80
        if self.non_fiscal_open:
            b2 |= 0x20
        if self.current_receipt.is_open:
            b2 |= 0x08
        if not self.paper_present:
            b2 |= 0x01
            b0 |= 0x20

        return bytes([b0, b1, b2, 0x80, 0x86, 0x9A, 0x80, 0x80])

    def open_fiscal_receipt(self, operator_id: int, receipt_type: int = 1) -> tuple[int, list[str]]:
        if self.current_receipt.is_open:
            return ErrorCode.ERR_OPEN_BON, []
        if self.non_fiscal_open:
            return ErrorCode.ERR_OPEN_NONFISCALBON, []

        self.slip_counter += 1
        self.fiscal_receipt_counter += 1
        self.current_receipt = ActiveReceiptState(
            is_open=True,
            receipt_type=receipt_type,
            operator_id=operator_id,
            slip_number=self.slip_counter,
            opened_at=self.get_current_datetime_str(),
        )
        self.tape.print_receipt_header(
            headers=self.headers[:4],
            serial_number=self.serial_number,
            fiscal_number=self.fiscal_number,
            tax_number=self.tax_number,
        )
        return ErrorCode.OK, [str(self.slip_counter)]

    def register_sale(
        self,
        name: str,
        tax_group: str,
        price: float,
        qty: float = 1.0,
        discount_type: int = 0,
        discount_val: float = 0.0,
        department: int = 0,
        unit: str = "бр.",
    ) -> tuple[int, list[str]]:
        if not self.current_receipt.is_open:
            return ErrorCode.ERR_CLOSED_BON, []

        item = ReceiptSaleItem(
            name=name, price=price, quantity=qty, tax_group=tax_group,
            discount_type=discount_type, discount_value=discount_val,
            department=department, unit=unit,
        )
        self.current_receipt.items.append(item)
        self.tape.print_item_line(
            index=len(self.current_receipt.items),
            name=name, price=price, qty=qty, tax_group=tax_group,
            discount_type=discount_type, discount_val=discount_val,
            final_total=item.final_total,
        )
        self.tape.set_display(1, name)
        self.tape.set_display(2, f"{item.final_total:.2f} UAH")
        return ErrorCode.OK, [str(self.slip_counter)]

    def subtotal(
        self,
        print_out: bool = True,
        discount_type: int = 0,
        discount_val: float = 0.0,
    ) -> tuple[int, list[str]]:
        if not self.current_receipt.is_open:
            return ErrorCode.ERR_CLOSED_BON, []

        self.current_receipt.subtotal_discount_type = discount_type
        self.current_receipt.subtotal_discount_val = discount_val
        subtotal_val = self.current_receipt.calculate_subtotal()

        if print_out:
            self.tape.print_subtotal(subtotal_val)

        tax_turns: dict[str, float] = {k: 0.0 for k in self.tax_rates}
        for it in self.current_receipt.items:
            tax_turns[it.tax_group] = tax_turns.get(it.tax_group, 0.0) + it.final_total

        fields = [str(self.slip_counter), f"{subtotal_val:.2f}"]
        for tg in ["А", "Б", "В", "Г", "Д", "Е", "Ж", "З"]:
            fields.append(f"{tax_turns.get(tg, 0.0):.2f}")
        return ErrorCode.OK, fields

    def total_payment(self, paid_mode: int, amount: float) -> tuple[int, list[str]]:
        if not self.current_receipt.is_open:
            return ErrorCode.ERR_CLOSED_BON, []

        subtotal_val = self.current_receipt.calculate_subtotal()
        due = round(subtotal_val - self.current_receipt.total_paid(), 2)
        if amount <= 0:
            amount = due

        self.current_receipt.paid_amounts[paid_mode] = (
            self.current_receipt.paid_amounts.get(paid_mode, 0.0) + amount
        )
        total_paid_now = self.current_receipt.total_paid()

        pay_names = {0: "ГОТІВКА", 1: "КРЕДИТНА КАРТКА", 2: "ДЕБЕТОВА КАРТКА"}
        pay_name = pay_names.get(paid_mode, f"ОПЛАТА #{paid_mode}")

        if total_paid_now < subtotal_val - 0.001:
            residual = round(subtotal_val - total_paid_now, 2)
            self.tape.print_total_payment(pay_name, amount, 0.0)
            return ErrorCode.OK, ["D", f"{residual:.2f}"]

        change = max(0.0, round(total_paid_now - subtotal_val, 2))
        self.tape.print_total_payment(pay_name, amount, change)
        return ErrorCode.OK, ["R", f"{change:.2f}"]

    def close_fiscal_receipt(self) -> tuple[int, list[str]]:
        if not self.current_receipt.is_open:
            return ErrorCode.ERR_CLOSED_BON, []

        subtotal_val = self.current_receipt.calculate_subtotal()
        if self.current_receipt.total_paid() < subtotal_val - 0.001:
            return ErrorCode.ERR_AMOUNT_SMALLER, []

        for it in self.current_receipt.items:
            self.daily_sales_by_tax[it.tax_group] = (
                self.daily_sales_by_tax.get(it.tax_group, 0.0) + it.final_total
            )

        cash_paid = self.current_receipt.paid_amounts.get(0, 0.0)
        change = max(0.0, round(self.current_receipt.total_paid() - subtotal_val, 2))
        self.cash_in_safe += round(cash_paid - change, 2)
        self.daily_sales_count += 1

        slip_num = self.slip_counter
        dt_str = self.get_current_datetime_str()
        self.tape.print_receipt_footer(slip_num, dt_str, self.current_receipt.is_storno)
        self.last_qr_string = f"{self.serial_number}*{slip_num:07d}*{dt_str}*{subtotal_val:.2f}"
        self.current_receipt = ActiveReceiptState()
        return ErrorCode.OK, [str(slip_num)]

    def cancel_receipt(self) -> tuple[int, list[str]]:
        if not self.current_receipt.is_open:
            return ErrorCode.ERR_CLOSED_BON, []
        self.tape.append_raw_line("------- ЧЕК АНУЛЬОВАНО (VOID) -------")
        self.current_receipt = ActiveReceiptState()
        return ErrorCode.OK, []

    def perform_z_report(self) -> tuple[int, list[str]]:
        return DatecsReportsManager.perform_z_report(self)

    def perform_x_report(self) -> tuple[int, list[str]]:
        return DatecsReportsManager.perform_x_report(self)

    def cash_in_out(self, op_type: int, amount: float) -> tuple[int, list[str]]:
        return DatecsReportsManager.cash_in_out(self, op_type, amount)
