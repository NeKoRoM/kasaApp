"""
Назва файлу: config.py
Модуль: core
Призначення: Центральна конфігурація бекенд-додатку FastAPI.
Архітектурна роль:
  • Визначає глобальні константи системи (ідентифікатори ролей користувачів).
  • Задає налаштування асиметричної криптографії RS256 для випуску та перевірки JWT-токенів.
  • Зчитує параметри оточення (DATABASE_URL, SECRET_KEY, ALLOWED_HOSTS) із файлу .env через Pydantic Settings.
"""

from pydantic import Field, BaseModel
from pydantic_settings import BaseSettings, SettingsConfigDict
from pathlib import Path

# Базова директорія додатку (папка app/)
BASE_DIR = Path(__file__).parent.parent

# --- Системні ідентифікатори ролей користувачів ---
# Використовуються у декораторі require_roles() для перевірки прав доступу до ендпоінтів
SUPER_ADMIN_ROLE_ID = 100  # Головний адміністратор системи (повний доступ до всіх компаній та магазинів)
ADMIN_ROLE_ID = 1          # Адміністратор компанії/мережі магазинів
USER_ROLE_ID = 2           # Касир / оператор торгової точки (доступ лише до свого магазину та кас)


class AuthJWT(BaseModel):
    """
    Конфігурація асиметричної автентифікації на основі JWT (JSON Web Tokens).

    Суть:
      Замість симетричного секретного ключа (HS256) використовується пара ключів RSA (RS256):
      • Приватний ключ (jwt-private.pem) використовується виключно сервером для підписування токенів при логіні.
      • Публічний ключ (jwt-public.pem) використовується для швидкої верифікації підпису вхідних запитів.
    """
    private_key_path: Path = BASE_DIR / "auth" / "certs" / "jwt-private.pem"
    public_key_path: Path = BASE_DIR / "auth" / "certs" / "jwt-public.pem"
    algorithm: str = "RS256"
    access_token_expire_minutes: int = 15  # Час життя access-токена в хвилинах


class Settings(BaseSettings):
    """
    Головний клас налаштувань середовища Pydantic.

    Автоматично вичитує змінні середовища з файлу .env або операційної системи.
    Забезпечує сувору типізацію та валідацію обов'язкових параметрів при старті сервера.
    """
    # Секретний ключ для сесійних механізмів та внутрішнього шифрування
    secret_key: str = Field("change-me-in-production-secret-key-12345", alias="SECRET_KEY")
    allowed_hosts: str = Field("*", alias="ALLOWED_HOSTS")
    database_url: str = Field("sqlite:///kasa_local.db", alias="DATABASE_URL")

    model_config = SettingsConfigDict(
        env_file=[str(BASE_DIR.parent / ".env"), ".env"],
        populate_by_name=True,
        extra="ignore"
    )

    # Вкладені налаштування JWT-криптографії
    auth_jwt: AuthJWT = AuthJWT()


# Екземпляр конфігурації (Singleton) для імпорту в усі модулі бекенду
settings = Settings()
