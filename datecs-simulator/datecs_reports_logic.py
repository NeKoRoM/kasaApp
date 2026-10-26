"""
Datecs / Exellio Fiscal Reports and Cash Operations Logic.
Handles daily X-reports, Z-reports, and Cash-in / Cash-out operations.
"""

from datecs_contract import ErrorCode


class DatecsReportsManager:
    """Manages fiscal reporting routines and cash drawer audit operations."""

    @staticmethod
    def perform_z_report(engine) -> tuple[int, list[str]]:
        dt_str = engine.get_current_datetime_str()
        rep_num = engine.z_report_counter
        engine.tape.print_x_or_z_report(
            report_type="Z",
            report_number=rep_num,
            date_time_str=dt_str,
            turnover_by_tax=engine.daily_sales_by_tax,
            tax_rates=engine.tax_rates,
            total_sales_count=engine.daily_sales_count,
            cash_in_safe=engine.cash_in_safe,
        )

        fields = [str(rep_num)]
        for tg in ["А", "Б", "В", "Г", "Д", "Е", "Ж", "З"]:
            fields.append(f"{engine.daily_sales_by_tax.get(tg, 0.0):.2f}")
        for tg in ["А", "Б", "В", "Г", "Д", "Е", "Ж", "З"]:
            fields.append(f"{engine.daily_storno_by_tax.get(tg, 0.0):.2f}")

        # Reset daily sales registers
        engine.z_report_counter += 1
        engine.remaining_z_reports = max(0, engine.remaining_z_reports - 1)
        engine.daily_sales_by_tax = {k: 0.0 for k in engine.tax_rates}
        engine.daily_storno_by_tax = {k: 0.0 for k in engine.tax_rates}
        engine.daily_sales_count = 0
        return ErrorCode.OK, fields

    @staticmethod
    def perform_x_report(engine) -> tuple[int, list[str]]:
        dt_str = engine.get_current_datetime_str()
        rep_num = engine.z_report_counter
        engine.tape.print_x_or_z_report(
            report_type="X",
            report_number=rep_num,
            date_time_str=dt_str,
            turnover_by_tax=engine.daily_sales_by_tax,
            tax_rates=engine.tax_rates,
            total_sales_count=engine.daily_sales_count,
            cash_in_safe=engine.cash_in_safe,
        )
        fields = [str(rep_num)]
        for tg in ["А", "Б", "В", "Г", "Д", "Е", "Ж", "З"]:
            fields.append(f"{engine.daily_sales_by_tax.get(tg, 0.0):.2f}")
        for tg in ["А", "Б", "В", "Г", "Д", "Е", "Ж", "З"]:
            fields.append(f"{engine.daily_storno_by_tax.get(tg, 0.0):.2f}")
        return ErrorCode.OK, fields

    @staticmethod
    def cash_in_out(engine, op_type: int, amount: float) -> tuple[int, list[str]]:
        if op_type in (0, 2):  # Cash In
            engine.cash_in_safe += amount
            engine.daily_cash_in += amount
            op_label = "внесення"
        else:  # Cash Out
            if amount > engine.cash_in_safe:
                return ErrorCode.ERR_NOT_ENOUGH_CASH, []
            engine.cash_in_safe -= amount
            engine.daily_cash_out += amount
            op_label = "видача"

        if amount > 0:
            engine.tape.print_cash_in_out_slip(
                op_label, amount, engine.cash_in_safe, engine.get_current_datetime_str()
            )

        fields = [
            f"{engine.cash_in_safe:.2f}",
            f"{engine.daily_cash_in:.2f}",
            f"-{engine.daily_cash_out:.2f}" if engine.daily_cash_out > 0 else f"{engine.daily_cash_out:.2f}",
        ]
        return ErrorCode.OK, fields
