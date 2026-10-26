"""
Назва файлу: order_schema.py
Модуль: schemas
Призначення: Pydantic DTO схеми для валідації та серіалізації фіскальних чеків (Orders).
"""

from decimal import Decimal
from pydantic import BaseModel, ConfigDict


class OrderItemCreate(BaseModel):
    """Схема додавання позиції товару до замовлення/чека."""
    product_id: int | None = None
    product_name: str
    product_price: Decimal
    quantity: Decimal
    total_price: Decimal
    excise_stamp: str | None = None
    tax_group: str = "А"
    uktz_code: str | None = None


class OrderCreate(BaseModel):
    """Схема створення нового фіскального замовлення/чека."""
    cash_register_id: int = 1
    receipt_id: str
    fiscal_check_number: str | None = None
    total_price: Decimal
    payment_type: int = 0
    status: str = "completed"
    created_at: int | None = None
    customer_name: str | None = None
    items: list[OrderItemCreate]


class OrderItemResponse(BaseModel):
    """Схема вихідних даних товарної позиції чека."""
    id: int
    product_id: int | None = None
    product_name: str
    product_price: Decimal
    quantity: Decimal
    total_price: Decimal
    excise_stamp: str | None = None
    tax_group: str

    model_config = ConfigDict(from_attributes=True)


class OrderResponse(BaseModel):
    """Схема вихідних даних фіскального замовлення/чека."""
    id: int
    cash_register_id: int
    receipt_id: str | None = None
    fiscal_check_number: str | None = None
    total_price: Decimal
    payment_type: int
    status: str
    customer_name: str | None = None
    items: list[OrderItemResponse] = []

    model_config = ConfigDict(from_attributes=True)
