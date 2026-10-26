"""
Datecs / Exellio PLU Database and Parameter Programming (CMD 255) Handlers.
"""

from datecs_contract import ErrorCode, PluRecord


class DatecsParamsHandler:
    """Processes PLU database queries (CMD 107) and parameter programming (CMD 255)."""

    @staticmethod
    def handle_plu_ops(engine, params: list[str]) -> tuple[int, list[str]]:
        if not params:
            return ErrorCode.ERR_INVALID_SYNTAX, []
        sub_op = params[0]

        if sub_op == "P":  # Program item
            try:
                plu_id = int(params[1])
                tax_g = params[2] if len(params) > 2 else "А"
                dep = int(params[3]) if len(params) > 3 and params[3] else 1
                grp = int(params[4]) if len(params) > 4 and params[4] else 1
                pr_type = int(params[5]) if len(params) > 5 and params[5] else 0
                price = float(params[6]) if len(params) > 6 and params[6] else 0.0
                qty = float(params[8]) if len(params) > 8 and params[8] else 100.0
                name = params[13] if len(params) > 13 else f"Товар {plu_id}"
                engine.plu_database[plu_id] = PluRecord(
                    plu_code=plu_id, name=name, price=price, tax_group=tax_g,
                    department=dep, item_group=grp, price_type=pr_type, quantity=qty
                )
                return ErrorCode.OK, []
            except (ValueError, IndexError):
                return ErrorCode.ERR_BAD_INPUT, []

        if sub_op == "I":
            return ErrorCode.OK, ["100000", str(len(engine.plu_database)), "72"]

        if sub_op in ("R", "F", "L", "N"):
            plu_id = int(params[1]) if len(params) > 1 and params[1].isdigit() else 1
            plu = engine.plu_database.get(plu_id)
            if not plu:
                return ErrorCode.ERR_PLU_NOT_FOUND, []
            return ErrorCode.OK, [
                str(plu.plu_code), plu.tax_group, str(plu.department), str(plu.item_group),
                str(plu.price_type), f"{plu.price:.2f}", f"{plu.turnover:.2f}",
                f"{plu.sold_qty:.3f}", f"{plu.quantity:.3f}", "", "", "", "", plu.name, "0"
            ]

        if sub_op == "D":
            plu_id = int(params[1]) if len(params) > 1 and params[1].isdigit() else 0
            engine.plu_database.pop(plu_id, None)
            return ErrorCode.OK, []

        return ErrorCode.OK, []

    @staticmethod
    def handle_programming_255(engine, params: list[str]) -> tuple[int, list[str]]:
        if not params:
            return ErrorCode.ERR_INVALID_SYNTAX, []
        var_name = params[0]

        # Write operation: Name\tIndex\tValue\t
        if len(params) > 2 and params[2] != "":
            val = params[2]
            idx = int(params[1]) if params[1].isdigit() else 0
            if var_name == "Header" and 0 <= idx < len(engine.headers):
                engine.headers[idx] = val
            elif var_name == "Footer" and 0 <= idx < len(engine.footers):
                engine.footers[idx] = val
            return ErrorCode.OK, []

        # Read operation: Name\tIndex\t\t
        idx = int(params[1]) if len(params) > 1 and params[1].isdigit() else 0
        if var_name == "Header":
            val = engine.headers[idx] if 0 <= idx < len(engine.headers) else ""
            return ErrorCode.OK, [val]
        if var_name == "Footer":
            val = engine.footers[idx] if 0 <= idx < len(engine.footers) else ""
            return ErrorCode.OK, [val]
        if var_name == "IDnumber":
            return ErrorCode.OK, [engine.serial_number]
        if var_name == "FMnumber":
            return ErrorCode.OK, [engine.fiscal_number]
        if var_name == "TAXnumber":
            return ErrorCode.OK, [engine.tax_number]
        if var_name == "AutoPowerOff":
            return ErrorCode.OK, ["1"]
        if var_name == "PrintColumns":
            return ErrorCode.OK, ["42"]
        if var_name == "BthAddress":
            return ErrorCode.OK, ["00:11:22:33:44:55"]
        if var_name == "LANport_fpCommands":
            return ErrorCode.OK, ["4999"]

        return ErrorCode.OK, ["0"]
