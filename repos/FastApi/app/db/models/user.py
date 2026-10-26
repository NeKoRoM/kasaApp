"""
Назва файлу: user.py
Модуль: db.models
Призначення: Попередня (legacy) версія моделі користувача UserOld.
Архітектурна роль:
  • Зберігається для зворотної сумісності зі старими міграціями.
  • На відміну від актуальної моделі User (у users.py), де зв'язки з компаніями та магазинами
    організовані через Many-to-Many асоціативні таблиці (user_companies, user_stores),
    тут використовувалися прямі поля store_id та company_id.
"""

from sqlalchemy import Column, Integer, String, Text, Boolean, ForeignKey, JSON, TIMESTAMP
from sqlalchemy.orm import declarative_base

Base = declarative_base()

class UserOld(Base):
    """
    Застаріла модель користувача з жорсткою прив'язкою до одного магазину та компанії.
    Актуальна модель знаходиться у файлі users.py (клас User).
    """
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    username = Column(String(50), unique=True, nullable=False)
    email = Column(String(255), unique=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    role_id = Column(Integer, ForeignKey("roles.id"), nullable=True)
    first_name = Column(String(50))
    last_name = Column(String(50))
    phone = Column(String(20))
    created_at = Column(TIMESTAMP, nullable=False)
    last_login = Column(TIMESTAMP)
    is_active = Column(Boolean, default=True)
    store_id = Column(Integer, ForeignKey("stores.id"), nullable=True)
    company_id = Column(Integer, ForeignKey("companies.id"), nullable=True)
    reset_token = Column(String(255))
    reset_token_expires = Column(TIMESTAMP)
    session_token = Column(String(255))
    avatar_url = Column(String(255))
    settings = Column(JSON)
    notes = Column(Text)
    last_password_change = Column(TIMESTAMP)
