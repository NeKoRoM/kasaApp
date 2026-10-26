"""
Datecs / Exellio Thermal Receipt Tape Visualizer.
Renders realistic POS thermal paper tape printouts and customer display lines.
"""

from datecs_contract import ActiveReceiptState, PluRecord


class DatecsTapeFormatter:
    """
    Formats receipts and reports as visual ASCII thermal printer tapes.
    Width is standard 40 characters for POS receipt printers.
    """

    WIDTH = 40

    def __init__(self) -> None:
        self.tape_history: list[str] = []
        self.current_receipt_lines: list[str] = []
        self.display_line1: str = ""
        self.display_line2: str = ""
        self.drawer_open: bool = False

    def center(self, text: str) -> str:
        return text.center(self.WIDTH)

    def row(self, left: str, right: str) -> str:
        space = self.WIDTH - len(left) - len(right)
        if space < 1:
            space = 1
        return left + (" " * space) + right

    def line(self, char: str = "-") -> str:
        return char * self.WIDTH

    def append_raw_line(self, text: str) -> None:
        self.current_receipt_lines.append(text)
        self.tape_history.append(text)

    def print_receipt_header(
        self,
        headers: list[str],
        serial_number: str,
        fiscal_number: str,
        tax_number: str,
    ) -> None:
        self.current_receipt_lines.clear()
        self.append_raw_line(self.line("="))
        for h in headers:
            if h.strip():
                self.append_raw_line(self.center(h.strip()))
        self.append_raw_line(self.line("-"))
        self.append_raw_line(self.row(f"ПН: {tax_number}", f"ЗН: {serial_number}"))
        self.append_raw_line(self.row(f"ФН: {fiscal_number}", "КАСИР 01"))
        self.append_raw_line(self.line("="))

    def print_item_line(
        self,
        index: int,
        name: str,
        price: float,
        qty: float,
        tax_group: str,
        discount_type: int = 0,
        discount_val: float = 0.0,
        final_total: float = 0.0,
    ) -> None:
        self.append_raw_line(f"{index:02d}. {name}")
        qty_str = f"{qty:.3f} x {price:.2f}"
        total_str = f"{price * qty:.2f} {tax_group}"
        self.append_raw_line(self.row(f"    {qty_str}", total_str))

        if discount_type > 0 and discount_val > 0:
            if discount_type in (1, 2):
                disc_label = "ЗНИЖКА" if discount_type == 2 else "НАДБАВКА"
                self.append_raw_line(self.row(f"    {disc_label} {discount_val:.2f}%", f"= {final_total:.2f}"))
            elif discount_type in (3, 4):
                disc_label = "ЗНИЖКА" if discount_type == 4 else "НАДБАВКА"
                self.append_raw_line(self.row(f"    {disc_label} {discount_val:.2f}", f"= {final_total:.2f}"))

    def print_subtotal(self, subtotal: float) -> None:
        self.append_raw_line(self.line("-"))
        self.append_raw_line(self.row("ПІДСУМОК (SUBTOTAL):", f"{subtotal:.2f} ГРН"))

    def print_total_payment(
        self,
        payment_name: str,
        amount_paid: float,
        change: float,
    ) -> None:
        self.append_raw_line(self.line("-"))
        self.append_raw_line(self.row(f"ОПЛАТА ({payment_name}):", f"{amount_paid:.2f} ГРН"))
        if change > 0.001:
            self.append_raw_line(self.row("РЕШТА (CHANGE):", f"{change:.2f} ГРН"))

    def print_receipt_footer(
        self,
        slip_number: int,
        date_time_str: str,
        is_storno: bool = False,
    ) -> str:
        self.append_raw_line(self.line("="))
        doc_type = "АНУЛЬОВАНО (STORNO)" if is_storno else "ФІСКАЛЬНИЙ ЧЕК"
        self.append_raw_line(self.center(doc_type))
        self.append_raw_line(self.row(f"ЧЕК № {slip_number:06d}", date_time_str))
        self.append_raw_line(self.center("[ QR CODE / FISCAL SIGNATURE ]"))
        self.append_raw_line(self.line("="))
        self.append_raw_line("")

        finished_receipt = "\n".join(self.current_receipt_lines)
        return finished_receipt

    def print_x_or_z_report(
        self,
        report_type: str,
        report_number: int,
        date_time_str: str,
        turnover_by_tax: dict[str, float],
        tax_rates: dict[str, float],
        total_sales_count: int,
        cash_in_safe: float,
    ) -> str:
        lines: list[str] = [
            self.line("="),
            self.center(f"ДЕННИЙ {report_type}-ЗВІТ № {report_number}"),
            self.center(date_time_str),
            self.line("-"),
            self.row("КІЛЬКІСТЬ ЧЕКІВ:", str(total_sales_count)),
            self.row("ГОТІВКА В КАСІ:", f"{cash_in_safe:.2f} ГРН"),
            self.line("-"),
            self.center("ОБІГ ПО ПОДАТКОВИХ ГРУПАХ:"),
        ]

        total_sum = 0.0
        for tax_grp in ["А", "Б", "В", "Г", "Д"]:
            turnover = turnover_by_tax.get(tax_grp, 0.0)
            rate = tax_rates.get(tax_grp, 0.0)
            if turnover > 0.001 or tax_grp in ("А", "Б"):
                tax_amt = round(turnover * (rate / (100.0 + rate)), 2) if rate > 0 else 0.0
                lines.append(self.row(f"ГРУПА {tax_grp} ({rate:.1f}%):", f"{turnover:.2f}"))
                if tax_amt > 0:
                    lines.append(self.row(f"  в т.ч. ПДВ {tax_grp}:", f"{tax_amt:.2f}"))
                total_sum += turnover

        lines.extend([
            self.line("-"),
            self.row("ЗАГАЛЬНИЙ ОБІГ:", f"{total_sum:.2f} ГРН"),
            self.line("="),
            self.center(f"КІНЕЦЬ {report_type}-ЗВІТУ"),
            self.line("="),
            "",
        ])

        report_text = "\n".join(lines)
        for l in lines:
            self.tape_history.append(l)
        return report_text

    def print_cash_in_out_slip(
        self,
        operation_name: str,
        amount: float,
        cash_in_safe: float,
        date_time_str: str,
    ) -> str:
        lines: list[str] = [
            self.line("="),
            self.center(f"СЛУЖБОВЕ {operation_name.upper()}"),
            self.center(date_time_str),
            self.line("-"),
            self.row("СУМА ОПЕРАЦІЇ:", f"{amount:.2f} ГРН"),
            self.row("ЗАЛИШОК В КАСІ:", f"{cash_in_safe:.2f} ГРН"),
            self.line("="),
            "",
        ]
        slip_text = "\n".join(lines)
        for l in lines:
            self.tape_history.append(l)
        return slip_text

    def set_display(self, line_num: int, text: str) -> None:
        if line_num == 1:
            self.display_line1 = text[:20]
        elif line_num == 2:
            self.display_line2 = text[:20]
        elif line_num == 0:
            self.display_line1 = ""
            self.display_line2 = ""

    def get_recent_tape(self, max_lines: int = 50) -> str:
        return "\n".join(self.tape_history[-max_lines:])
