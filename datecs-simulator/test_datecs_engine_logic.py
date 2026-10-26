"""
Unit tests for Datecs Fiscal Engine and Command Dispatcher.
Verifies complete receipt lifecycle, tax calculations, payments, and reports.
"""

import unittest
from datecs_contract import CommandCode, ErrorCode
from datecs_dispatcher_logic import DatecsCommandDispatcher
from datecs_engine_logic import DatecsFiscalEngine


class TestDatecsEngineLogic(unittest.TestCase):

    def setUp(self) -> None:
        self.engine = DatecsFiscalEngine()
        self.dispatcher = DatecsCommandDispatcher(self.engine)

    def test_open_sale_total_close_receipt(self) -> None:
        # 1. Open receipt
        err, fields = self.dispatcher.dispatch(CommandCode.OPEN_FISCAL, "1\t1\t1\t")
        self.assertEqual(err, ErrorCode.OK)
        self.assertTrue(self.engine.current_receipt.is_open)
        slip_num = int(fields[0])

        # 2. Register sale 1: Топено сирене, price 25.00, qty 2.0
        err, _ = self.dispatcher.dispatch(CommandCode.REGISTER_SALE, "Топено сирене\t1\t25.00\t2.0\t0\t0\t1\tбр.\t")
        self.assertEqual(err, ErrorCode.OK)
        self.assertEqual(len(self.engine.current_receipt.items), 1)

        # 3. Register sale 2: Хліб, price 15.00, qty 1.0, 10% discount
        err, _ = self.dispatcher.dispatch(CommandCode.REGISTER_SALE, "Хліб\t2\t15.00\t1.0\t2\t10.0\t1\tбр.\t")
        self.assertEqual(err, ErrorCode.OK)
        self.assertEqual(len(self.engine.current_receipt.items), 2)

        # Item 1: 25.00 * 2 = 50.00
        # Item 2: 15.00 * 1 = 15.00 - 1.50 = 13.50
        # Subtotal: 63.50
        err, sub_fields = self.dispatcher.dispatch(CommandCode.SUBTOTAL, "1\t0\t0\t0\t")
        self.assertEqual(err, ErrorCode.OK)
        self.assertEqual(sub_fields[1], "63.50")

        # 4. Total payment: cash 100.00 -> Change = 36.50
        err, pay_fields = self.dispatcher.dispatch(CommandCode.TOTAL_PAYMENT, "0\t100.00\t")
        self.assertEqual(err, ErrorCode.OK)
        self.assertEqual(pay_fields[0], "R")
        self.assertEqual(pay_fields[1], "36.50")

        # 5. Close receipt
        err, close_fields = self.dispatcher.dispatch(CommandCode.CLOSE_FISCAL, "")
        self.assertEqual(err, ErrorCode.OK)
        self.assertEqual(int(close_fields[0]), slip_num)
        self.assertFalse(self.engine.current_receipt.is_open)

        # Check safe cash increased by 63.50
        self.assertAlmostEqual(self.engine.cash_in_safe, 1000.0 + 63.50)

    def test_prevent_nested_open_receipt(self) -> None:
        self.dispatcher.dispatch(CommandCode.OPEN_FISCAL, "1\t1\t1\t")
        err, _ = self.dispatcher.dispatch(CommandCode.OPEN_FISCAL, "1\t1\t1\t")
        self.assertEqual(err, ErrorCode.ERR_OPEN_BON)

    def test_prevent_sale_without_receipt(self) -> None:
        err, _ = self.dispatcher.dispatch(CommandCode.REGISTER_SALE, "Кава\t1\t30.00\t1.0\t")
        self.assertEqual(err, ErrorCode.ERR_CLOSED_BON)

    def test_x_and_z_reports(self) -> None:
        # Make a sale first
        self.dispatcher.dispatch(CommandCode.OPEN_FISCAL, "1\t1\t1\t")
        self.dispatcher.dispatch(CommandCode.REGISTER_SALE, "Кава\t1\t50.00\t1.0\t")
        self.dispatcher.dispatch(CommandCode.TOTAL_PAYMENT, "0\t50.00\t")
        self.dispatcher.dispatch(CommandCode.CLOSE_FISCAL, "")

        # X report (does not reset sales)
        err, x_fields = self.dispatcher.dispatch(CommandCode.REPORTS, "X\t")
        self.assertEqual(err, ErrorCode.OK)
        self.assertEqual(x_fields[1], "50.00")  # Tax A turnover

        # Z report (resets daily turnover)
        err, z_fields = self.dispatcher.dispatch(CommandCode.REPORTS, "Z\t")
        self.assertEqual(err, ErrorCode.OK)
        self.assertEqual(z_fields[1], "50.00")

        # After Z report, sales should be 0.00
        self.assertEqual(self.engine.daily_sales_by_tax["А"], 0.0)
        self.assertEqual(self.engine.daily_sales_count, 0)

    def test_programming_parameter_255(self) -> None:
        # Read header 0
        err, fields = self.dispatcher.dispatch(CommandCode.PROGRAMMING, "Header\t0\t\t")
        self.assertEqual(err, ErrorCode.OK)
        self.assertIn("КАТРАН", fields[0])

        # Write header 0
        err, _ = self.dispatcher.dispatch(CommandCode.PROGRAMMING, "Header\t0\tСУПЕРМАРКЕТ 'ЕКО'\t")
        self.assertEqual(err, ErrorCode.OK)

        # Read back
        err, fields = self.dispatcher.dispatch(CommandCode.PROGRAMMING, "Header\t0\t\t")
        self.assertEqual(fields[0], "СУПЕРМАРКЕТ 'ЕКО'")


if __name__ == "__main__":
    unittest.main()
