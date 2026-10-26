"""
Назва файлу: products.py
Модуль: api.routes
Призначення: Маршрути для керування каталогом товарів (CRUD, пошук за 4 штрихкодами, фільтрація, прив'язка до РРО).
Архітектурна роль:
  • Дозволяє касирам та адміністраторам отримувати номенклатуру, дозволену для продажу на конкретних касах.
  • Реалізує пошук у реальному часі (GET /products/search) за назвою або будь-яким із 4 штрих-кодів.
  • Забезпечує прив'язку товару до фізичних реєстраторів через Many-to-Many зв'язок (cash_register_ids).
"""

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from db.session import get_db
from db.models import Product, CashRegister
from db.models.users import User
from auth.dependencies import require_roles
from core.config import SUPER_ADMIN_ROLE_ID, USER_ROLE_ID
from schemas.product_schema import (
    ProductBase,
    ProductCreate,
    ProductUpdate,
    ProductOut,
)
from services.product_service import get_current_user_products

__all__ = [
    "router",
    "ProductBase",
    "ProductCreate",
    "ProductUpdate",
    "ProductOut",
    "get_current_user_products",
]

router = APIRouter(prefix="/products", tags=["products"])

# --- API Ендпоінти ---

@router.get("/user-products", response_model=list[ProductOut])
def get_user_products(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> list[ProductOut]:
    """Повертає повний асортимент товарів, дозволений для роботи поточному касиру."""
    return get_current_user_products(db, current_user)


@router.post("/", response_model=ProductOut)
def create_product(
    product: ProductCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> ProductOut:
    """
    Створює новий товар та опціонально зв'язує його з фізичними касами.
    """
    data = product.model_dump()
    data["user_id"] = current_user.id
    cash_register_ids = data.pop("cash_register_ids", None)
    
    db_product = Product(**data)
    db.add(db_product)
    db.commit()
    db.refresh(db_product)
    
    # Прив'язка до вибраних касових апаратів
    if cash_register_ids:
        cash_registers = db.query(CashRegister).filter(CashRegister.id.in_(cash_register_ids)).all()
        db_product.cash_registers = cash_registers
        db.commit()
        db.refresh(db_product)
    return db_product


@router.get("/", response_model=list[ProductOut])
def get_products(
    name: str | None = Query(None, description="Фільтр за назвою товару"),
    barcode: str | None = Query(None, description="Фільтр за будь-яким із 4 штрих-кодів"),
    group_id: int | None = Query(None, description="Фільтр за категорією"),
    pdv_id: int | None = Query(None, description="Фільтр за ставкою ПДВ"),
    uktz_id: int | None = Query(None, description="Фільтр за кодом УКТЗЕД"),
    skip: int = 0,
    limit: int = 50,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> list[ProductOut]:
    """
    Отримує список товарів із пагінацією та гнучкою комбінованою фільтрацією.
    """
    products = get_current_user_products(db, current_user)
    
    # Фільтрація
    if name:
        products = [p for p in products if name.lower() in (p.name or '').lower()]
    if barcode:
        products = [
            p for p in products if barcode in (p.barcode_1, p.barcode_2, p.barcode_3, p.barcode_4)
        ]
    if group_id:
        products = [p for p in products if p.group_id == group_id]
    if pdv_id:
        products = [p for p in products if p.pdv_id == pdv_id]
    if uktz_id:
        products = [p for p in products if p.uktz_id == uktz_id]
        
    # Пагінація
    return products[skip:skip+limit]


@router.get("/search", response_model=list[ProductOut])
def search_products(
    q: str = Query(..., description="Рядок пошуку (назва або штрих-код)"),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> list[ProductOut]:
    """
    Швидкий універсальний пошук для каси: одночасно перевіряє назву та всі 4 штрих-коди.
    """
    products = get_current_user_products(db, current_user)
    q_lower = q.lower()
    result = [
        p for p in products if
        (q_lower in (p.name or '').lower()) or
        (q_lower in (p.barcode_1 or '').lower()) or
        (q_lower in (p.barcode_2 or '').lower()) or
        (q_lower in (p.barcode_3 or '').lower()) or
        (q_lower in (p.barcode_4 or '').lower())
    ]
    return result


@router.get("/{product_id}", response_model=ProductOut)
def get_product(
    product_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> ProductOut:
    """Повертає картку одного конкретного товару за його ідентифікатором."""
    products = get_current_user_products(db, current_user)
    product = next((p for p in products if p.id == product_id), None)
    if not product:
        raise HTTPException(status_code=404, detail="Товар не знайдено")
    return product


@router.put("/{product_id}", response_model=ProductOut)
def update_product(
    product_id: int,
    product_update: ProductUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> ProductOut:
    """Оновлює параметри товару та список закріплених за ним РРО."""
    products = get_current_user_products(db, current_user)
    product = next((p for p in products if p.id == product_id), None)
    if not product:
        raise HTTPException(status_code=404, detail="Товар не знайдено")
        
    update_data = product_update.model_dump(exclude_unset=True)
    cash_register_ids = update_data.pop("cash_register_ids", None)
    
    for field, value in update_data.items():
        setattr(product, field, value)
        
    if cash_register_ids is not None:
        cash_registers = db.query(CashRegister).filter(CashRegister.id.in_(cash_register_ids)).all()
        product.cash_registers = cash_registers
        
    db.commit()
    db.refresh(product)
    return product


@router.delete("/{product_id}")
def delete_product(
    product_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> dict[str, str]:
    """Видаляє товар із каталогу підприємства."""
    products = get_current_user_products(db, current_user)
    product = next((p for p in products if p.id == product_id), None)
    if not product:
        raise HTTPException(status_code=404, detail="Товар не знайдено")
    db.delete(product)
    db.commit()
    return {"detail": "Товар успішно видалено"}
