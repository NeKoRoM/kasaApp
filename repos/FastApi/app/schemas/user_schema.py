"""
Назва файлу: user_schema.py
Модуль: schemas
Призначення: Pydantic DTO схеми для валідації та серіалізації користувачів і ролей.
"""

from datetime import datetime
from pydantic import BaseModel, EmailStr, ConfigDict, JsonValue


class UserBase(BaseModel):
    """
    Базова схема користувача системи KasaApp.
    Містить персональні дані працівника, контактну інформацію, роль
    та списки ідентифікаторів закріплених магазинів та компаній.
    """
    username: str
    email: EmailStr
    first_name: str | None = None
    last_name: str | None = None
    phone: str | None = None
    role_id: int | None = None
    avatar_url: str | None = None
    settings: JsonValue | None = None
    notes: str | None = None
    store_ids: list[int] | None = None
    company_ids: list[int] | None = None


class UserCreate(UserBase):
    """Схема створення нового користувача."""
    password: str
    is_owner: bool | None = False


class UserUpdate(BaseModel):
    """Схема оновлення профілю та зв'язків користувача."""
    first_name: str | None = None
    last_name: str | None = None
    phone: str | None = None
    role_id: int | None = None
    is_active: bool | None = None
    avatar_url: str | None = None
    settings: JsonValue | None = None
    notes: str | None = None
    store_ids: list[int] | None = None
    company_ids: list[int] | None = None
    is_owner: bool | None = None


class UserOut(UserBase):
    """Схема публічного представлення користувача у відповідях API."""
    id: int
    created_at: datetime | None = None
    last_login: datetime | None = None
    is_active: bool | None = None

    model_config = ConfigDict(from_attributes=True)


class RoleBase(BaseModel):
    """Базова схема ролі в системі (наприклад: 'super_admin', 'admin', 'cashier')."""
    name: str
    description: str | None = None


class RoleCreate(RoleBase):
    """Схема створення нової ролі."""
    pass


class RoleOut(RoleBase):
    """Схема повернення даних ролі з системним ID."""
    id: int

    model_config = ConfigDict(from_attributes=True)
