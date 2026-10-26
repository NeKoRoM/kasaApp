"""
Назва файлу: login.py
Модуль: api.routes
Призначення: Маршрут входу в систему та випуску цифрових токенів доступу (JWT).
Архітектурна роль:
  • Приймає облікові дані касира/користувача (UserLogin: username, password).
  • Звіряє пароль з збереженим bcrypt-гешем за допомогою validate_auth_user().
  • Генерує підписаний асиметричний RS256 Bearer JWT-токен із subject = user_id.
"""

from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    status,
)
from sqlalchemy.orm import Session

from auth import utils as auth_utils
from db.models.users import User
from db.session import get_db
from schemas.auth_schema import TokenInfo, UserLogin

__all__ = [
    "router",
    "TokenInfo",
    "UserLogin",
    "validate_auth_user",
    "auth_user_issue_jwt",
]

router = APIRouter(prefix="/jwt", tags=["JWT"])


def validate_auth_user(
    user_login: UserLogin,
    db: Session,
) -> User:
    """
    Валідує логін та пароль користувача перед випуском токена.

    @param user_login: пара username та raw password.
    @param db: активна сесія бази даних.
    @return: екземпляр User у разі успіху.
    @raises HTTPException(401): якщо користувача не знайдено або пароль не збігається.
    """
    unauthed_exc = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Невірний логін або пароль",
    )
    # Пошук користувача за унікальним ім'ям
    user: User = db.query(User).filter(User.username == user_login.username).first()
    if not user:
        raise unauthed_exc
    
    # Звірка пароля через bcrypt
    if auth_utils.verify_password(user_login.password, user.password_hash) is False:
        raise unauthed_exc

    return user


@router.post("/login/", response_model=TokenInfo)
def auth_user_issue_jwt(
    user_login: UserLogin,
    db: Session = Depends(get_db),
) -> TokenInfo:
    """
    Ендпоінт автентифікації касира / користувача:
    Перевіряє облікові дані та повертає підписаний токен для подальших запитів до API.
    """
    # 1. Валідація вхідних даних
    user = validate_auth_user(user_login, db)

    # 2. Формування корисного навантаження токена (Claims)
    jwt_payload = {
        "sub": str(user.id),
        "username": user.username,
        "role_id": user.role_id,
    }

    # 3. Асиметричний підпис RS256 закритим ключем сервера
    token = auth_utils.encode_jwt(jwt_payload)

    # 4. Повернення клієнту Bearer-токена
    return TokenInfo(
        access_token=token,
        token_type="Bearer",
    )
