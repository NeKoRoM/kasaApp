"""
Назва файлу: stores.py
Модуль: api.routes
Призначення: Керування торговими точками (магазинами) та закріпленням персоналу.
Архітектурна роль:
  • Дозволяє адміністраторам створювати, оновлювати та переглядати магазини мережі.
  • Розподіляє доступ касирів: касир бачить виключно ті магазини, до яких він прив'язаний (current_user.stores).
  • Керує прив'язкою персоналу через Many-to-Many зв'язок (user_store_association).
"""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from db.session import get_db
from db.models import Store
from db.models.users import User
from auth.dependencies import require_roles
from core.config import SUPER_ADMIN_ROLE_ID, USER_ROLE_ID
from schemas.store_schema import (
    StoreBase,
    StoreCreate,
    StoreUpdate,
    UserOut,
    StoreOut,
)

__all__ = [
    "router",
    "StoreBase",
    "StoreCreate",
    "StoreUpdate",
    "UserOut",
    "StoreOut",
]

router = APIRouter(prefix="/Organization", tags=["stores"])

# --- API Ендпоінти ---

@router.post("/stores/", response_model=StoreOut)
def create_store(
    store: StoreCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> Store:
    """
    Створює нову торгову точку та закріплює за нею початковий перелік співробітників.
    """
    store_data = store.model_dump(exclude={"user_ids"})
    new_store = Store(**store_data)
    db.add(new_store)
    db.commit()
    db.refresh(new_store)

    # Прив'язка співробітників до створеного магазину
    if store.user_ids:
        for user_id in store.user_ids:
            user = db.get(User, user_id)
            if not user:
                db.rollback()
                raise HTTPException(status_code=404, detail=f"Користувача з ID {user_id} не знайдено")
            new_store.users.append(user)

        db.commit()
        db.refresh(new_store)

    return new_store


@router.get("/stores/", response_model=list[StoreOut])
def get_stores(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> list[Store]:
    """
    Отримує список магазинів.
    SuperAdmin бачить усі магазини системи, звичайний користувач/касир — лише свої призначені магазини.
    """
    if current_user.role_id == SUPER_ADMIN_ROLE_ID:
        stores = db.query(Store).all()
    else:
        stores = current_user.stores 
    return stores


@router.get("/stores/{store_id}", response_model=StoreOut)
def get_store(
    store_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> Store:
    """Отримує детальні дані конкретного магазину з перевіркою прав доступу."""
    store = db.get(Store, store_id)
    if not store:
        raise HTTPException(status_code=404, detail="Магазин не знайдено")

    if current_user.role_id == USER_ROLE_ID and store not in current_user.stores:
        raise HTTPException(status_code=403, detail="Немає доступу до цієї торгової точки")

    return store


@router.put("/stores/{store_id}", response_model=StoreOut)
def update_store(
    store_id: int,
    store_update: StoreUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> Store:
    """
    Оновлює реквізити магазину та перезаписує список призначених співробітників.
    """
    store = db.get(Store, store_id)
    if not store:
        raise HTTPException(status_code=404, detail="Магазин не знайдено")

    if current_user.role_id == USER_ROLE_ID and store not in current_user.stores:
        raise HTTPException(status_code=403, detail="Немає прав на оновлення цього магазину")

    update_data = store_update.model_dump(exclude_unset=True, exclude={"user_ids"})
    for field, value in update_data.items():
        setattr(store, field, value)

    # Оновлення списку користувачів
    if store_update.user_ids is not None:
        store.users.clear()
        for user_id in store_update.user_ids:
            user = db.get(User, user_id)
            if not user:
                db.rollback()
                raise HTTPException(status_code=404, detail=f"Користувача з ID {user_id} не знайдено")
            store.users.append(user)

    db.commit()
    db.refresh(store)
    return store