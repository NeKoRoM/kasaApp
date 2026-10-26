"""
Назва файлу: authentication.py
Модуль: api.routes
Призначення: Маршрут реєстрації нових користувачів адміністратором системи.
Архітектурна роль:
  • Дозволяє суперадміністратору (SUPER_ADMIN_ROLE_ID = 100) створювати нові облікові записи.
  • Валідує унікальність імені користувача (username).
  • Безпечно хешує пароль перед збереженням у базу даних за допомогою bcrypt.
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from db.session import get_db
from db.models.users import User
from auth.dependencies import require_roles
from auth import utils as auth_utils
from core.config import SUPER_ADMIN_ROLE_ID
from schemas.auth_schema import LegacyUserCreate as UserCreate, UserActionResponse

__all__ = [
    "router",
    "UserCreate",
    "UserActionResponse",
    "create_new_user",
]

router = APIRouter(tags=["Authentication"])


@router.post(
    "/CreateNewUser",
    response_model=UserActionResponse,
    summary="Створення нового облікового запису (Admin)",
    description="Реєструє новий обліковий запис користувача або касира в системі. "
                "Операція доступна виключно для адміністратора з роллю SuperAdmin. "
                "Пароль обов'язково хешується алгоритмом bcrypt перед збереженням у базі даних.",
    response_description="Результат створення облікового запису користувача"
)
async def create_new_user(
    user: UserCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID]))
) -> UserActionResponse:
    """
    Ендпоінт створення нового користувача. Доступний виключно для SuperAdmin.

    Аргументи:
      - user: Pydantic DTO (UserCreate) з полями облікового запису.
      - db: активна транзакційна сесія SQLAlchemy.
      - current_user: поточний автентифікований користувач із перевіреною роллю SuperAdmin.

    Кроки виконання:
      1. Перевірка, чи не зайнятий бажаний username у базі даних.
      2. Генерація bcrypt-гешу пароля з криптографічною сіллю.
      3. Створення та фіксація запису User у реляційній базі даних.
      4. Повернення структурованої відповіді UserActionResponse.
    """
    if db.query(User).filter(User.username == user.username).first():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Користувач із таким іменем вже існує"
        )

    hashed_password = auth_utils.hash_password(user.password).decode()

    new_user = User(
        username=user.username,
        email=user.email,
        password_hash=hashed_password,
        first_name=user.first_name,
        last_name=user.last_name,
        role_id=user.role_id,
        is_active=True
    )

    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return UserActionResponse(
        status="success",
        message=f"Користувача {user.username} успішно створено"
    )
