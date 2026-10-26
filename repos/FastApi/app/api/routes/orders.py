"""
Назва файлу: orders.py
Модуль: api.routes
Призначення: Маршрути для збереження та перегляду фіскальних чеків (Orders).
Архітектурна роль:
  • Приймає замовлення/чеки, сформовані касирами (POST /orders/).
  • Забезпечує збереження як загальних атрибутів чека, так і кожної позиції (OrderItem) з акцизними марками.
  • Надає історію продажів за касою та магазином (GET /orders/).
"""

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from db.session import get_db
from db.models import Order, OrderItem, CashRegister
from db.models.users import User
from auth.dependencies import require_roles
from core.config import SUPER_ADMIN_ROLE_ID, USER_ROLE_ID
from schemas.order_schema import (
    OrderItemCreate,
    OrderCreate,
    OrderItemResponse,
    OrderResponse,
)

__all__ = [
    "router",
    "OrderItemCreate",
    "OrderCreate",
    "OrderItemResponse",
    "OrderResponse",
]

router = APIRouter(prefix="/orders", tags=["Orders"])


@router.post("/", response_model=OrderResponse, status_code=status.HTTP_201_CREATED)
def create_order(
    payload: OrderCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> Order:
    cash_reg = db.query(CashRegister).filter(CashRegister.id == payload.cash_register_id).first()
    if not cash_reg:
        raise HTTPException(status_code=404, detail="Касовий апарат не знайдено")

    new_order = Order(
        cash_register_id=payload.cash_register_id,
        receipt_id=payload.receipt_id,
        fiscal_check_number=payload.fiscal_check_number,
        total_price=payload.total_price,
        payment_type=payload.payment_type,
        status=payload.status,
        customer_name=payload.customer_name,
        user_id=current_user.id
    )
    db.add(new_order)
    db.flush()

    for item_data in payload.items:
        order_item = OrderItem(
            order_id=new_order.id,
            product_id=item_data.product_id,
            product_name=item_data.product_name,
            product_price=item_data.product_price,
            quantity=item_data.quantity,
            total_price=item_data.total_price,
            excise_stamp=item_data.excise_stamp,
            tax_group=item_data.tax_group,
            uktz_code=item_data.uktz_code
        )
        db.add(order_item)

    db.commit()
    db.refresh(new_order)
    return new_order


@router.get("/", response_model=list[OrderResponse])
def get_orders(
    cash_register_id: int | None = Query(None),
    limit: int = Query(50, le=200),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> list[Order]:
    query = db.query(Order)
    if cash_register_id:
        query = query.filter(Order.cash_register_id == cash_register_id)

    return query.order_by(Order.id.desc()).limit(limit).all()
