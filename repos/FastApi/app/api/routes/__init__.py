"""
Пакетний ініціалізатор та агрегатор маршрутів FastApi.
Шлях у системі: FastApi/app/api/routes/__init__.py

Архітектурна роль:
- Централізує імпорт усіх модульних маршрутизаторів (APIRouter) системи KasaApp.
- Формує єдиний список `all_routers`, який підключається в головній точці входу app/main.py.
- Об'єднує:
  1. users_router_old (/old) - зворотна сумісність для ранніх клієнтів.
  2. login_router (/auth) - маршрути входу за паролем або рефреш токеном.
  3. authentication_router (/auth) - маршрути реєстрації та перевірки токенів.
  4. users_router (/users) - основний RBAC менеджмент персоналу та зв'язків.
  5. companies_router (/Organization) - керування юридичними особами/ФОП.
  6. stores_router (/Organization) - керування магазинами, складами та касовими апаратами.
  7. products_router (/products) - довідники товарів, UKTZED, ставок PDV та штрихкодів.
"""

# Імпорт окремих модулів маршрутизації
from .user import router as users_router_old
from .login import router as login_router
from .authentication import router as authentication_router
from .users import router as users_router
from .companies import router as companies_router
from .stores import router as stores_router
from .products import router as products_router
from .orders import router as orders_router

# Єдиний список усіх активних роутерів застосунку для циклічного монтування в FastAPI app
all_routers = [
    users_router_old,
    login_router,
    authentication_router,
    users_router,
    companies_router,
    stores_router,
    products_router,
    orders_router
]