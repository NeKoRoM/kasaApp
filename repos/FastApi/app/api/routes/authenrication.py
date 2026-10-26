"""
Назва файлу: authenrication.py (Legacy Alias)
Модуль: api.routes
Призначення: Шим зворотної сумісності. Канонічний модуль перейменовано на authentication.py.
"""

from .authentication import router, UserCreate, create_new_user

__all__ = ["router", "UserCreate", "create_new_user"]
