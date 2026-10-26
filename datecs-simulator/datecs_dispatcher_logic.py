"""
Datecs / Exellio Protocol v2.11 Command Dispatcher.
Parses incoming frame parameters and routes to the fiscal engine.
"""

from datecs_contract import CommandCode, ErrorCode
from datecs_engine_logic import DatecsFiscalEngine
from datecs_params_logic import DatecsParamsHandler


class DatecsCommandDispatcher:
    """Dispatches decoded Datecs frames to corresponding fiscal engine methods."""

    def __init__(self, engine: DatecsFiscalEngine) -> None:
        self.engine = engine

    def dispatch(self, cmd_code: int, data_text: str) -> tuple[int, list[str]]:
        params = data_text.split("\t")
        if params and params[-1] == "":
            params = params[:-1]

        if cmd_code == CommandCode.CHECK_PC_MODE:
            return ErrorCode.OK, []
        if cmd_code == CommandCode.READ_DATE_TIME:
            return ErrorCode.OK, [self.engine.get_current_datetime_str()]
        if cmd_code == CommandCode.SET_DATE_TIME:
            return ErrorCode.OK, []
        if cmd_code == CommandCode.CLEAR_DISPLAY:
            self.engine.tape.set_display(0, "")
            return ErrorCode.OK, []
        if cmd_code == CommandCode.DISPLAY_LINE1:
            self.engine.tape.set_display(1, params[0] if params else "")
            return ErrorCode.OK, []
        if cmd_code == CommandCode.DISPLAY_LINE2:
            self.engine.tape.set_display(2, params[0] if params else "")
            return ErrorCode.OK, []
        if cmd_code == CommandCode.SHOW_DATE_TIME:
            dt = self.engine.get_current_datetime_str()
            self.engine.tape.set_display(1, dt)
            return ErrorCode.OK, [dt]

        if cmd_code == CommandCode.OPEN_FISCAL:
            op_id = int(params[0]) if params and params[0].isdigit() else 1
            rec_type = int(params[2]) if len(params) > 2 and params[2].isdigit() else 1
            return self.engine.open_fiscal_receipt(operator_id=op_id, receipt_type=rec_type)

        if cmd_code == CommandCode.REGISTER_SALE:
            return self._handle_register_sale(params)
        if cmd_code == CommandCode.SALE_PLU:
            return self._handle_sale_plu(params)
        if cmd_code == CommandCode.SUBTOTAL:
            print_out = params[0] != "0" if params else True
            disc_type = int(params[2]) if len(params) > 2 and params[2].isdigit() else 0
            disc_val = float(params[3]) if len(params) > 3 and params[3] else 0.0
            return self.engine.subtotal(print_out, disc_type, disc_val)

        if cmd_code == CommandCode.TOTAL_PAYMENT:
            paid_mode = int(params[0]) if params and params[0].isdigit() else 0
            amount = float(params[1]) if len(params) > 1 and params[1] else 0.0
            return self.engine.total_payment(paid_mode, amount)

        if cmd_code == CommandCode.CLOSE_FISCAL:
            return self.engine.close_fiscal_receipt()
        if cmd_code == CommandCode.CANCEL_RECEIPT:
            return self.engine.cancel_receipt()
        if cmd_code == CommandCode.GET_VAT_RATES:
            fields = [str(self.engine.z_report_counter)]
            for grp in ["А", "Б", "В", "Г", "Д", "Е", "Ж", "З"]:
                fields.append(f"{self.engine.tax_rates.get(grp, 0.0):.2f}")
            fields.append("01-01-00")
            return ErrorCode.OK, fields

        if cmd_code == CommandCode.REPORTS:
            rep_type = params[0].upper() if params else "X"
            return self.engine.perform_z_report() if rep_type == "Z" else self.engine.perform_x_report()

        if cmd_code == CommandCode.CASH_IN_OUT:
            op_type = int(params[0]) if params and params[0].isdigit() else 0
            amount = float(params[1]) if len(params) > 1 and params[1] else 0.0
            return self.engine.cash_in_out(op_type, amount)

        if cmd_code == CommandCode.READ_STATUS:
            return self._handle_read_status(params)
        if cmd_code == CommandCode.TRANSACTION_STATUS:
            return self._handle_transaction_status()
        if cmd_code == CommandCode.DEVICE_INFO:
            return self._handle_device_info(params)
        if cmd_code == CommandCode.DEFINE_READ_PLU:
            return DatecsParamsHandler.handle_plu_ops(self.engine, params)
        if cmd_code == CommandCode.PROGRAMMING:
            return DatecsParamsHandler.handle_programming_255(self.engine, params)

        if cmd_code == CommandCode.DRAWER_OPEN:
            self.engine.tape.drawer_open = True
            return ErrorCode.OK, []
        if cmd_code in (CommandCode.PAPER_FEED, CommandCode.PAPER_CUT):
            return ErrorCode.OK, []
        if cmd_code in (CommandCode.PRINT_FISCAL_TEXT, CommandCode.PRINT_NON_FISCAL):
            self.engine.tape.append_raw_line(params[0] if params else "")
            return ErrorCode.OK, []
        if cmd_code == CommandCode.READ_ERROR_MSG:
            return ErrorCode.OK, [params[0] if params else "0", "Помилка виконана успішно"]

        return self._handle_other_commands(cmd_code, params)

    def _handle_register_sale(self, params: list[str]) -> tuple[int, list[str]]:
        if len(params) < 3:
            return ErrorCode.ERR_INVALID_SYNTAX, []
        tax_map = {"1": "А", "2": "Б", "3": "В", "4": "Г", "5": "Д", "6": "Е", "7": "Ж", "8": "З"}
        tax_grp = tax_map.get(params[1], params[1])
        try:
            price = float(params[2])
            qty = float(params[3]) if len(params) > 3 and params[3] else 1.0
            disc_type = int(params[4]) if len(params) > 4 and params[4] else 0
            disc_val = float(params[5]) if len(params) > 5 and params[5] else 0.0
            dep = int(params[6]) if len(params) > 6 and params[6] else 0
            unit = params[7] if len(params) > 7 else "бр."
        except ValueError:
            return ErrorCode.ERR_BAD_INPUT, []
        return self.engine.register_sale(params[0], tax_grp, price, qty, disc_type, disc_val, dep, unit)

    def _handle_sale_plu(self, params: list[str]) -> tuple[int, list[str]]:
        if not params:
            return ErrorCode.ERR_INVALID_SYNTAX, []
        try:
            plu_id = int(params[0])
            qty = float(params[1]) if len(params) > 1 and params[1] else 1.0
            disc_type = int(params[3]) if len(params) > 3 and params[3] else 0
            disc_val = float(params[4]) if len(params) > 4 and params[4] else 0.0
        except ValueError:
            return ErrorCode.ERR_BAD_INPUT, []
        plu = self.engine.plu_database.get(plu_id)
        if not plu:
            return ErrorCode.ERR_PLU_NOT_FOUND, []
        return self.engine.register_sale(
            plu.name, plu.tax_group, plu.price, qty, disc_type, disc_val, plu.department
        )

    def _handle_read_status(self, params: list[str]) -> tuple[int, list[str]]:
        if not params or not params[0]:
            return ErrorCode.OK, []
        opt = params[0]
        if opt == "0":
            rec_stat = "1" if self.engine.current_receipt.is_open else "0"
            subtotal = f"{self.engine.current_receipt.calculate_subtotal():.2f}"
            return ErrorCode.OK, ["1", rec_stat, str(self.engine.slip_counter), subtotal, str(self.engine.slip_counter), self.engine.get_current_datetime_str()]
        if opt == "1":
            return ErrorCode.OK, [self.engine.last_qr_string or f"{self.engine.serial_number}*{self.engine.slip_counter:07d}*2026-09-25*0.00"]
        return ErrorCode.OK, []

    def _handle_transaction_status(self) -> tuple[int, list[str]]:
        is_op = 1 if self.engine.current_receipt.is_open else 0
        items_cnt = len(self.engine.current_receipt.items)
        amt = self.engine.current_receipt.calculate_subtotal()
        payed = self.engine.current_receipt.total_paid()
        return ErrorCode.OK, [str(is_op), str(self.engine.slip_counter), str(items_cnt), f"{amt:.2f}", f"{payed:.2f}"]

    def _handle_device_info(self, params: list[str]) -> tuple[int, list[str]]:
        opt = params[0] if params else "1"
        if opt == "1":
            return ErrorCode.OK, [
                self.engine.serial_number, self.engine.fiscal_number,
                self.engine.headers[0], self.engine.headers[1],
                self.engine.tax_number, self.engine.headers[2], self.engine.headers[3],
            ]
        if opt == "2":
            return ErrorCode.OK, ["8666", "4157", "96", "1"]
        if opt == "3":
            dt = self.engine.get_current_datetime_str()
            return ErrorCode.OK, [str(self.engine.slip_counter), dt, str(self.engine.z_report_counter), dt]
        if opt == "5":
            return ErrorCode.OK, ["8666", "99"]
        return ErrorCode.OK, []

    def _handle_other_commands(self, cmd_code: int, params: list[str]) -> tuple[int, list[str]]:
        if cmd_code == CommandCode.DIAGNOSTICS:
            return ErrorCode.OK, [
                self.engine.device_model, self.firmware_rev, "12Mar19", "1631", "1426",
                "00000000", self.engine.serial_number, self.engine.fiscal_number
            ]
        if cmd_code == CommandCode.READ_TAX_NUM:
            return ErrorCode.OK, [self.engine.tax_number]
        if cmd_code == CommandCode.REMAINING_Z_REPORTS:
            return ErrorCode.OK, [str(self.engine.remaining_z_reports)]
        if cmd_code == CommandCode.INVOICE_INTERVAL:
            return ErrorCode.OK, ["1000000001", "1000999999", "1000000042"]
        if cmd_code in (CommandCode.DAILY_TAXATION, CommandCode.LAST_FISCAL_ENTRY):
            f = [str(self.engine.z_report_counter)]
            f.extend([f"{self.engine.daily_sales_by_tax.get(g, 0.0):.2f}" for g in ["А","Б","В","Г","Д","Е","Ж","З"]])
            return ErrorCode.OK, f
        return ErrorCode.OK, []
