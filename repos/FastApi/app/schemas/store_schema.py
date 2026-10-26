"""
Назва файлу: store_schema.py
Модуль: schemas
Призначення: Pydantic DTO схеми для валідації та серіалізації магазинів (торгових точок).
"""

from datetime import datetime
from pydantic import BaseModel, EmailStr, ConfigDict


class StoreBase(BaseModel):
    """Базові параметри торгової точки."""
    name: str
    address: str | None = None
    phone: str | None = None
    email: EmailStr | None = None
    company_id: int | None = None
    user_ids: list[int] | None = None


class StoreCreate(StoreBase):
    """Схема створення магазину."""
    pass


class StoreUpdate(BaseModel):
    """Схема редагування магазину."""
    name: str | None = None
    address: str | None = None
    phone: str | None = None
    email: EmailStr | None = None
    company_id: int | None = None
    user_ids: list[int] | None = None


class UserOut(BaseModel):
    """Коротка інформація про співробітника магазину."""
    id: int
    username: str
    email: EmailStr

    model_config = ConfigDict(from_attributes=True)


class StoreOut(StoreBase):
    """Схема вихідних даних магазину з доданим списком співробітників."""
    id: int
    created_at: datetime | None = None
    users: list[UserOut] | None = None

    model_config = ConfigDict(from_attributes=True)
