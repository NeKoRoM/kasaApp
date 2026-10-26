"""
Назва файлу: product_schema.py
Модуль: schemas
Призначення: Pydantic DTO схеми для валідації та серіалізації каталогу товарів.
"""

from decimal import Decimal
from pydantic import BaseModel, condecimal, ConfigDict


class ProductBase(BaseModel):
    """Базові атрибути товару, сумісні з полями пам'яті РРО Datecs/Exellio."""
    product_id: int | None = 0
    pdv_id: int | None = None
    group_id: int | None = None
    uktz_id: int | None = None
    section: str | None = ''
    type: str | None = ''
    price: condecimal(max_digits=10, decimal_places=2) | None = Decimal('0.00')
    sales_amount: condecimal(max_digits=10, decimal_places=2) | None = Decimal('0.00')
    sales_count: int | None = 0
    remainder: condecimal(max_digits=10, decimal_places=2) | None = Decimal('0.00')
    barcode_1: str | None = ''
    barcode_2: str | None = ''
    barcode_3: str | None = ''
    barcode_4: str | None = ''
    name: str | None = ''
    weight_or_unit: str | None = ''
    unit_of_measurement: str | None = ''
    is_independent: bool | None = False
    user_id: int | None = 0
    cash_register_ids: list[int] | None = None


class ProductCreate(ProductBase):
    """Схема створення нового товару."""
    pass


class ProductUpdate(BaseModel):
    """Схема оновлення параметрів існуючого товару."""
    product_id: int | None = None
    pdv_id: int | None = None
    group_id: int | None = None
    uktz_id: int | None = None
    section: str | None = None
    type: str | None = None
    price: condecimal(max_digits=10, decimal_places=2) | None = None
    sales_amount: condecimal(max_digits=10, decimal_places=2) | None = None
    sales_count: int | None = None
    remainder: condecimal(max_digits=10, decimal_places=2) | None = None
    barcode_1: str | None = None
    barcode_2: str | None = None
    barcode_3: str | None = None
    barcode_4: str | None = None
    name: str | None = None
    weight_or_unit: str | None = None
    unit_of_measurement: str | None = None
    is_independent: bool | None = None
    user_id: int | None = None
    cash_register_ids: list[int] | None = None


class ProductOut(ProductBase):
    """Схема вихідних даних товару з бази даних."""
    id: int

    model_config = ConfigDict(from_attributes=True)
