"""
Назва файлу: utils.py
Модуль: auth
Призначення: Утиліти криптографічного захисту, генерації токенів та хешування паролів.
Архітектурна роль:
  • encode_jwt — асиметричне підписування токенів алгоритмом RS256 за допомогою приватного RSA-ключа.
  • decode_jwt — верифікація цифрового підпису вхідних Bearer-токенів публічним RSA-ключем.
  • hash_password — одностороннє безпечне хешування паролів за стандартом bcrypt із сіллю (salt).
  • verify_password — криптографічна звірка введеного пароля з збереженим гешем.
"""

from datetime import datetime, timedelta, timezone
import bcrypt
import jwt
from core.config import settings


def encode_jwt(
    payload: dict,
    private_key: str | None = None,
    algorithm: str = settings.auth_jwt.algorithm,
    expire_minutes: int = settings.auth_jwt.access_token_expire_minutes,
    expire_timedelta: timedelta | None = None,
) -> str:
    """
    Генерує підписаний JWT access-токен за допомогою приватного RSA-ключа (RS256).

    @param payload: словник даних користувача (наприклад sub=user_id, username, role_id).
    @param private_key: вміст закритого ключа у форматі PEM.
    @param algorithm: алгоритм підпису ("RS256").
    @param expire_minutes: час життя токена у хвилинах (за замовчуванням 15 хв).
    @param expire_timedelta: опціональний кастомний час дії токена.
    @return: закодований підписаний JWT-рядок.
    """
    if private_key is None:
        private_key = settings.auth_jwt.private_key_path.read_text()

    to_encode = payload.copy()
    now = datetime.now(timezone.utc)
    if expire_timedelta:
        expire = now + expire_timedelta
    else:
        expire = now + timedelta(minutes=expire_minutes)
    
    # Службові JWT-заголовки: час випуску (iat) та час закінчення (exp)
    to_encode.update(
        exp=expire,
        iat=now,
    )
    encoded = jwt.encode(
        to_encode,
        private_key,
        algorithm=algorithm,
    )
    return encoded


def decode_jwt(
    token: str | bytes,
    public_key: str | None = None,
    algorithm: str = settings.auth_jwt.algorithm,
) -> dict:
    """
    Декодує та перевіряє валідність вхідного JWT-токена за допомогою відкритого RSA-ключа.

    @param token: вхідний Bearer-токен від клієнта.
    @param public_key: відкритий ключ сервера для перевірки підпису.
    @param algorithm: очікуваний алгоритм ("RS256").
    @return: корисне навантаження токена (claims: sub, exp, iat тощо).
    @raises jwt.PyJWTError: якщо токен прострочений або підпис недійсний.
    """
    if public_key is None:
        public_key = settings.auth_jwt.public_key_path.read_text()

    decoded = jwt.decode(
        token,
        public_key,
        algorithms=[algorithm],
    )
    return decoded


def hash_password(
    password: str,
) -> bytes:
    """
    Хешує пароль у відкритому вигляді за допомогою bcrypt із криптографічною сіллю.
    
    @param password: сирий пароль користувача.
    @return: байтовий геш для збереження в базі даних (колонка password_hash).
    """
    salt = bcrypt.gensalt()
    pwd_bytes: bytes = password.encode()
    return bcrypt.hashpw(pwd_bytes, salt)


def verify_password(
    password: str,
    hashed_password: str,
) -> bool:
    """
    Перевіряє відповідність введеного користувачем пароля збереженому bcrypt-гешу.

    @param password: введений при вході сирий пароль.
    @param hashed_password: збережений у БД геш.
    @return: True, якщо пароль вірний, інакше False.
    """
    return bcrypt.checkpw(
        password=password.encode(),
        hashed_password=hashed_password.encode(),
    )