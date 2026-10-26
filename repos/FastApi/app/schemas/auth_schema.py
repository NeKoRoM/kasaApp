"""
Назва файлу: auth_schema.py
Модуль: schemas
Призначення: Pydantic DTO схеми для аутентифікації, випуску токенів та сумісності зі старими клієнтами.
"""

from pydantic import BaseModel, EmailStr, ConfigDict, Field


class TokenInfo(BaseModel):
    """Схема відповіді з виданим токеном доступу JWT."""
    access_token: str = Field(description="Підписаний асиметричний RS256 JWT токен доступу")
    token_type: str = Field(default="Bearer", description="Тип токена авторизації")


class UserLogin(BaseModel):
    """Схема вхідних даних форми автентифікації касира / користувача."""
    username: str = Field(description="Унікальне ім'я користувача (логін)")
    password: str = Field(description="Сирий текстовий пароль для перевірки через bcrypt")


class LegacyUserCreate(BaseModel):
    """
    Спрощена схема створення нового облікового запису користувача.
    Використовується для зворотної сумісності з ранніми версіями POS-додатку.
    """
    username: str = Field(description="Логін нового облікового запису")
    email: EmailStr = Field(description="Електронна адреса")
    password: str = Field(description="Початковий пароль облікового запису")
    first_name: str | None = Field(default=None, description="Ім'я співробітника")
    last_name: str | None = Field(default=None, description="Прізвище співробітника")
    role_id: int = Field(default=1, description="Ідентифікатор ролі доступу (1 = User, 100 = SuperAdmin)")


class LegacyUserOut(BaseModel):
    """
    Спрощена схема виведення даних користувача.
    Не містить асоціацій з організаціями та аудиторських міток часу.
    """
    id: int = Field(description="Унікальний ідентифікатор користувача в базі")
    username: str = Field(description="Логін користувача")
    email: EmailStr = Field(description="Електронна адреса")
    first_name: str | None = Field(default=None, description="Ім'я")
    last_name: str | None = Field(default=None, description="Прізвище")
    role_id: int = Field(description="Ідентифікатор призначеної ролі")

    model_config = ConfigDict(from_attributes=True)


class UserActionResponse(BaseModel):
    """Схема стандартної відповіді про успішне виконання адміністративної дії."""
    status: str = Field(description="Статус операції, наприклад 'success'")
    message: str = Field(description="Деталізоване повідомлення про результат дії")


# Аліаси для зворотної сумісності зі старими маршрутами
UserCreate = LegacyUserCreate
UserOut = LegacyUserOut

__all__ = [
    "TokenInfo",
    "UserLogin",
    "LegacyUserCreate",
    "LegacyUserOut",
    "UserActionResponse",
    "UserCreate",
    "UserOut",
]
