"""
Кореневий пакетний ініціалізатор бекенд-застосунку KasaApp FastAPI.
Шлях у системі: FastApi/app/__init__.py

Позначає директорію 'app' як кореневий Python-пакет, що об'єднує:
- auth: Аутентифікація, генерація та валідація JWT токенів (RS256).
- core: Конфігурація середовища, константи та системні шляхи.
- db: ORM моделі SQLAlchemy (PostgreSQL/SQLite) та управління сесіями.
- api: REST API маршрутизатори для всіх сутностей торговельної екосистеми.
"""

import sys
from pathlib import Path

# Гарантуємо наявність директорії app у sys.path для коректного імпорту внутрішніх модулів (core, db, api, auth)
_app_dir = str(Path(__file__).resolve().parent)
if _app_dir not in sys.path:
    sys.path.insert(0, _app_dir)
