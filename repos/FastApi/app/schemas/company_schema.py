"""
Назва файлу: company_schema.py
Модуль: schemas
Призначення: Pydantic DTO схеми для валідації та серіалізації компаній (організацій/ФОП).
"""

from datetime import datetime
from pydantic import BaseModel, EmailStr, ConfigDict


class CompanyBase(BaseModel):
    """Базова схема компанії."""
    name: str
    address: str | None = None
    phone: str | None = None
    email: EmailStr | None = None
    user_ids: list[int] | None = None
    is_owner: bool | None = False


class CompanyCreate(CompanyBase):
    """Схема для створення нової компанії."""
    pass


class CompanyUpdate(BaseModel):
    """Схема для часткового оновлення даних компанії."""
    name: str | None = None
    address: str | None = None
    phone: str | None = None
    email: EmailStr | None = None
    user_ids: list[int] | None = None
    is_owner: bool | None = None


class UserOut(BaseModel):
    """Схема представлення користувача при вкладеному виведенні в CompanyOut."""
    id: int
    username: str
    email: EmailStr

    model_config = ConfigDict(from_attributes=True)


class StoreOut(BaseModel):
    """Схема представлення магазину при вкладеному виведенні в CompanyOut."""
    id: int
    name: str

    model_config = ConfigDict(from_attributes=True)


class CompanyOut(CompanyBase):
    """Схема вихідних даних компанії для відповіді API."""
    id: int
    created_at: datetime | None = None
    users: list[UserOut] | None = None
    stores: list[StoreOut] | None = None

    model_config = ConfigDict(from_attributes=True)
