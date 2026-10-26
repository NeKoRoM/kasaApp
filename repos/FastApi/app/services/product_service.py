"""
Назва файлу: product_service.py
Модуль: services
Призначення: Доменна бізнес-логіка фільтрації, розмежування прав та пошуку товарів каталогу.
"""

from sqlalchemy.orm import Session
from db.models import Product, CashRegister
from db.models.users import User
from core.config import SUPER_ADMIN_ROLE_ID


def get_current_user_products(db: Session, current_user: User) -> list[Product]:
    """
    Отримує перелік товарів, доступних поточному авторизованому користувачу/касиру.

    Логіка вибірки:
      1. Якщо SuperAdmin — повертає всі товари системи.
      2. Знаходить магазини та компанії, до яких закріплено користувача.
      3. Знаходить усі касові апарати (РРО), встановлені у цих магазинах.
      4. Робить вибірку товарів, прив'язаних до цих РРО через таблицю cash_register_products.
      5. Додає товари, створені безпосередньо цим користувачем.
      6. Усуває дублікати за унікальним id товару.
    """
    if current_user.role_id == SUPER_ADMIN_ROLE_ID:
        return db.query(Product).all()

    # 1. Магазини та компанії користувача
    store_ids = [s.id for s in getattr(current_user, 'stores', [])]
    company_ids = [c.id for c in getattr(current_user, 'companies', [])]
    if hasattr(current_user, "store_id") and current_user.store_id:
        store_ids.append(current_user.store_id)
    if hasattr(current_user, "company_id") and current_user.company_id:
        company_ids.append(current_user.company_id)

    # 2. Каси, що належать цим магазинам та компаніям
    cash_registers = db.query(CashRegister).filter(
        (CashRegister.store_id.in_(store_ids)) |
        (CashRegister.company_id.in_(company_ids))
    ).all()
    cash_register_ids = [cr.id for cr in cash_registers]

    # 3. Товари, закріплені за цими касами
    products_on_cash_registers = []
    if cash_register_ids:
        products_on_cash_registers = db.query(Product).join(
            Product.cash_registers
        ).filter(
            CashRegister.id.in_(cash_register_ids)
        ).all()

    # 4. Товари, закріплені персонально за користувачем
    user_products = db.query(Product).filter(Product.user_id == current_user.id).all()

    # 5. Дедуплікація
    all_products = {p.id: p for p in products_on_cash_registers + user_products}
    return list(all_products.values())
