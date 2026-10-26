"""
Головна точка входу (Application Entrypoint) бекенду KasaApp на базі FastAPI.
Шлях у системі: FastApi/app/main.py

Архітектурна роль:
1. Ініціалізація екземпляра ASGI-застосунку `FastAPI`.
2. Автоматична реєстрація всіх модульних маршрутів (`all_routers` з `api.routes`):
   - /auth (логін, рефреш, реєстрація)
   - /Organization (компанії, торгові точки, каси)
   - /products (товари, ставки ПДВ, УКТЗЕД)
   - /users (співробітники, ролі)
   - /old (legacy сумісність)
3. Додаткова фонова інфраструктура (раніше No-IP DDNS апдейтер для динамічних IP магазинів).
4. Базовий health-check ендпоінт GET "/" для моніторингу працездатності сервісу.
"""

import sys
from pathlib import Path

# Гарантуємо наявність директорії app у sys.path для підтримки абсолютних імпортів (api, core, db, auth)
_app_dir = str(Path(__file__).resolve().parent)
if _app_dir not in sys.path:
    sys.path.insert(0, _app_dir)

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from api.routes import all_routers

# Створення головного екземпляра веб-фреймворка FastAPI
# Відповідає за обробку HTTP запитів, генерацію Swagger UI (/docs) та ReDoc (/redoc)
app = FastAPI(
    title="KasaApp Fiscal API",
    description="REST API для управління торгівельними точками, товарами, користувачами та фіскалізацією",
    version="1.0.0"
)

# Налаштування CORS (Cross-Origin Resource Sharing) для підтримки веб-клієнтів та POS-інтерфейсів
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Фонове оновлення динамічної IP-адреси через No-IP (закоментовано в поточній конфігурації)
# start_noip_updater()

# Динамічне підключення всіх дочірніх маршрутизаторів (APIRouter) системи
for router in all_routers:
    app.include_router(router)


@app.get("/", summary="Health Check / Welcome", tags=["Health"])
def read_root():
    """
    Базовий кореневий ендпоінт перевірки доступності бекенду.
    Використовується балансувальниками навантаження, docker healthcheck
    або клієнтськими застосунками для швидкої перевірки зв'язку із сервером.
    """
    return {"message": "Welcome to the API"}

