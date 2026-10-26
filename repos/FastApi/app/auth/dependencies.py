"""
Назва файлу: dependencies.py
Модуль: auth
Призначення: Залежності FastAPI для автентифікації та рольового контролю доступу (RBAC).
Архітектурна роль:
  • require_roles — фабрика залежностей, що перевіряє валідність JWT-токена та наявність у користувача
    необхідної ролі (SuperAdmin, Admin чи Cashier) для доступу до конкретного ендпоінту.
"""

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session
from db.session import get_db
from db.models.users import User
from auth.utils import decode_jwt

# Схема вилучення Bearer-токена із HTTP-заголовка Authorization: Bearer <token>
http_bearer = HTTPBearer()


def require_roles(allowed_role_ids: list[int]):
    """
    Фабрика перевірки ролей користувача (Role-Based Access Control).

    @param allowed_role_ids: список числових ID ролей, яким дозволено доступ до маршруту.
    @return: функція-залежність FastAPI, що повертає автентифікованого користувача User.
    @raises HTTPException(401): якщо токен відсутній, недійсний або прострочений.
    @raises HTTPException(403): якщо роль користувача не входить до списку дозволених.
    """
    def dependency(
        credentials: HTTPAuthorizationCredentials = Depends(http_bearer),
        db: Session = Depends(get_db)
    ) -> User:
        token = credentials.credentials
        try:
            # Декодуємо токен публічним RSA-ключем
            payload = decode_jwt(token)
        except jwt.PyJWTError:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Недійсний або прострочений токен авторизації",
            )

        # Отримуємо ID користувача із payload.sub
        user_id = payload.get("sub")
        if isinstance(user_id, str) and user_id.isdigit():
            user_id = int(user_id)
        user = db.query(User).filter(User.id == user_id).first()
        
        # Перевірка існування користувача та його ролі
        if not user or user.role_id not in allowed_role_ids:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Недостатньо прав для виконання цієї операції",
            )
        return user

    return dependency