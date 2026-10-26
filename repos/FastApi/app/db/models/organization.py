"""
Назва файлу: organization.py
Модуль: db.models
Призначення: Моделі організаційної структури торговельної мережі (Компанії та Магазини).
Архітектурна роль:
  • Company — представляє юридичну особу / ФОП, володіє магазинами та касовими апаратами.
  • Store — фізична торговельна точка (магазин, кіоск, аптека, склад).
  • Асоціативні таблиці user_companies та user_stores — реалізують гнучке закріплення персоналу
    (касирів, менеджерів, власників) за конкретними підприємствами та магазинами.
"""

from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, Boolean, Table
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from ..session import Base

# Зв'язок користувачів із компаніями (з прапорцем is_owner для визначення власника бізнесу)
user_company_association = Table(
    'user_companies',
    Base.metadata,
    Column('user_id', Integer, ForeignKey('users.id'), primary_key=True),
    Column('company_id', Integer, ForeignKey('companies.id'), primary_key=True),
    Column('is_owner', Boolean, default=False),
    Column('created_at', DateTime(timezone=True), server_default=func.now())
)

# Зв'язок касирів/менеджерів із конкретними магазинами (робочими локаціями)
user_store_association = Table(
    'user_stores',
    Base.metadata,
    Column('user_id', Integer, ForeignKey('users.id'), primary_key=True),
    Column('store_id', Integer, ForeignKey('stores.id'), primary_key=True),
    Column('created_at', DateTime(timezone=True), server_default=func.now())
)


class Company(Base):
    """
    Сутність підприємства (Компанії / ФОП).

    Верхньорівневий елемент бізнес-ієрархії: одна компанія може володіти багатьма
    магазинами (Store) та централізовано керувати парком фіскальних реєстраторів (CashRegister).
    """
    __tablename__ = 'companies'

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False, unique=True) # Офіційне найменування юридичної особи
    address = Column(Text)                                 # Юридична адреса
    phone = Column(String(20))                             # Контактний телефон
    email = Column(String(255))                            # Контактний email
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Зв'язки
    cash_registers = relationship("CashRegister", back_populates="company")
    users = relationship(
        "User",
        secondary=user_company_association,
        back_populates="companies"
    )
    stores = relationship("Store", back_populates="company")


class Store(Base):
    """
    Сутність магазину (фізичної торгової точки).

    Локація, де безпосередньо встановлені касові апарати (РРО) та працюють касири.
    """
    __tablename__ = 'stores'

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False)             # Назва торгової точки (наприклад "Магазин №3 (Центр)")
    address = Column(Text)                                 # Фізична адреса магазину
    phone = Column(String(20))                             # Телефон торгової точки
    email = Column(String(255))                            # Email торгової точки
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    company_id = Column(Integer, ForeignKey('companies.id')) # Батьківська компанія

    # Зв'язки
    cash_registers = relationship("CashRegister", back_populates="store")
    users = relationship(
        "User",
        secondary=user_store_association,
        back_populates="stores"
    )
    company = relationship("Company", back_populates="stores")
