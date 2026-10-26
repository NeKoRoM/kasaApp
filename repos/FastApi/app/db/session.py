"""
Назва файлу: session.py
Модуль: db
Призначення: Керування підключенням до реляційної бази даних та життєвим циклом сесій SQLAlchemy.
Архітектурна роль:
  • Ініціалізує рушій підключення (Engine) на основі налаштувань settings.database_url.
  • Створює фабрику сесій SessionLocal.
  • Надає генератор залежностей get_db() для ін'єкції активної сесії БД у маршрути FastAPI (Dependency Injection).
  • Оголошує базовий декларативний клас Base для всіх ORM-моделей.
"""

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from core.config import settings

# Ініціалізація пулу з'єднань SQLAlchemy до цільової бази даних (PostgreSQL / SQLite)
engine = create_engine(settings.database_url)

# Фабрика сесій:
# autocommit=False — транзакція фіксується явно через db.commit()
# autoflush=False — запобігає передчасному скиданню непідтверджених змін у БД
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def get_db():
    """
    Генератор залежності (FastAPI Dependency) для надання сесії бази даних.
    
    Суть:
      1. Створює новий екземпляр сесії для кожного вхідного HTTP-запиту.
      2. Передає сесію у функцію-обробник маршруту через yield.
      3. Гарантовано закриває сесію у блоці finally після завершення обробки запиту,
         запобігаючи витоку відкритих з'єднань у пулі БД.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Базовий декларативний клас для успадкування всіма ORM-моделями системи
Base = declarative_base()
