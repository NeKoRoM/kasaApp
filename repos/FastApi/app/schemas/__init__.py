"""
Назва файлу: __init__.py
Модуль: schemas
Призначення: Агрегатор Pydantic DTO схем для серіалізації та валідації запитів.
"""

from .user_schema import (
    UserBase,
    UserCreate,
    UserUpdate,
    UserOut,
    RoleBase,
    RoleCreate,
    RoleOut,
)
from .product_schema import (
    ProductBase,
    ProductCreate,
    ProductUpdate,
    ProductOut,
)
from .company_schema import (
    CompanyBase,
    CompanyCreate,
    CompanyUpdate,
    CompanyOut,
)
from .store_schema import (
    StoreBase,
    StoreCreate,
    StoreUpdate,
    StoreOut,
)
from .order_schema import (
    OrderItemCreate,
    OrderCreate,
    OrderItemResponse,
    OrderResponse,
)
from .auth_schema import (
    TokenInfo,
    UserLogin,
    LegacyUserCreate,
    LegacyUserOut,
    UserActionResponse,
)

__all__ = [
    "UserBase",
    "UserCreate",
    "UserUpdate",
    "UserOut",
    "RoleBase",
    "RoleCreate",
    "RoleOut",
    "ProductBase",
    "ProductCreate",
    "ProductUpdate",
    "ProductOut",
    "CompanyBase",
    "CompanyCreate",
    "CompanyUpdate",
    "CompanyOut",
    "StoreBase",
    "StoreCreate",
    "StoreUpdate",
    "StoreOut",
    "OrderItemCreate",
    "OrderCreate",
    "OrderItemResponse",
    "OrderResponse",
    "TokenInfo",
    "UserLogin",
    "LegacyUserCreate",
    "LegacyUserOut",
    "UserActionResponse",
]
