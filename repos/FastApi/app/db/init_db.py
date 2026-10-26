"""
Назва файлу: init_db.py
Модуль: db
Призначення: Автоматична ініціалізація структури бази даних.
Архітектурна роль: Створює всі таблиці в базі даних, якщо вони ще не існують (на основі ORM-метаданих).
"""

import sys
from pathlib import Path

# Гарантуємо наявність директорії app у sys.path
_app_dir = str(Path(__file__).resolve().parent.parent)
if _app_dir not in sys.path:
    sys.path.insert(0, _app_dir)

from db.session import Base, engine
import db.models  # Реєструє всі ORM-моделі в Base.metadata


def init_db():
    """
    Створює всі зареєстровані таблиці в цільовій базі даних на основі метаданих SQLAlchemy.
    
    Викликається при першому розгортанні сервера або під час ініціалізації середовища тестування.
    """
    Base.metadata.create_all(bind=engine)
    print("[✓] Всі таблиці бази даних успішно створено.")


if __name__ == "__main__":
    init_db()
