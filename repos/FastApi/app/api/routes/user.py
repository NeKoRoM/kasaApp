"""
Модуль API маршрутизації (Legacy) для управління користувачами.
Шлях у системі: FastApi/app/api/routes/user.py

Архітектурна роль:
- Забезпечує зворотну сумісність (Backwards Compatibility) із ранніми прототипами
  клієнтських застосунків (перші версії Android POS або скрипти адміністрування).
- Використовує префікс "/old" та тег "OLD" у документації Swagger/OpenAPI.
- На відміну від основного модуля app/api/routes/users.py:
  1. Не підтримує складні зв'язки з компаніями та магазинами (user_company_association, user_store_association).
  2. Має спрощені DTO (UserCreate, UserOut) без полів аватарів, JSON-налаштувань та списків точок продажу.
- Усі операції захищені авторизацією SUPER_ADMIN.
"""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from core.config import SUPER_ADMIN_ROLE_ID
from db.session import get_db
from db.models.users import User
from auth.dependencies import require_roles
from auth.utils import hash_password
from schemas.auth_schema import LegacyUserCreate as UserCreate, LegacyUserOut as UserOut

__all__ = ["router", "UserCreate", "UserOut", "read_users", "create_user"]

# Маршрутизатор застарілого API з префіксом /old
router = APIRouter(prefix="/old", tags=["OLD"])


# ============================================================================
# Legacy Endpoints (Кінцеві точки зворотної сумісності)
# ============================================================================

@router.get("/users",
            summary="Отримати список усіх користувачів (Legacy)",
            description="Повертає повний список користувачів із бази даних. "
                        "Доступ дозволений лише для користувачів з роллю SUPER_ADMIN.",
            response_description="Список користувачів",
            response_model=list[UserOut]
            )
async def read_users(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID]))
) -> list[User]:
    """
    Асинхронний ендпоінт отримання всіх користувачів.
    Повертає спрощений список UserOut.
    """
    users = db.query(User).all()
    return users


@router.post("/user",
             description="Доступ дозволений лише для користувачів з роллю SUPER_ADMIN (Legacy).",
             response_model=UserOut
             )
async def create_user(
    user: UserCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID]))
) -> User:
    """
    Створення нового користувача за спрощеним legacy-сценарієм:
    1. Перевірка на конфлікти username та email.
    2. Гешування пароля через bcrypt (hash_password).
    3. Створення запису в таблиці users з активним статусом is_active=True.
    4. Повернення спрощеного об'єкта UserOut.
    """
    # Перевірка унікальності логіна
    if db.query(User).filter(User.username == user.username).first():
        raise HTTPException(status_code=400, detail="Username already exists")

    # Перевірка унікальності email
    if db.query(User).filter(User.email == user.email).first():
        raise HTTPException(status_code=400, detail="Email already exists")

    # Безпечне гешування
    hashed_password = hash_password(user.password).decode()
    new_user = User(
        username=user.username,
        email=user.email,
        password_hash=hashed_password,
        first_name=user.first_name,
        last_name=user.last_name,
        role_id=user.role_id,
        is_active=True,
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return new_user

