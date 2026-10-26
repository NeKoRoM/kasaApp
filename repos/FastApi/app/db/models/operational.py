"""
Назва файлу: operational.py
Модуль: db.models
Призначення: Моделі операційної діяльності (фізичні фіскальні реєстратори та чеки/замовлення).
Архітектурна роль:
  • CashRegister — описує конкретний касовий апарат, його прив'язку до магазину, компанії та перелік закріплених касирів/товарів.
  • Order — зберігає зафіксовані касові продажі (чеки) з розбивкою сум, податків та акцизу.
  • Таблиці зв'язків (Association Tables) — реалізують зв'язки Many-to-Many між касою та товарами/ПДВ/УКТЗЕД/касирами.
"""

from sqlalchemy import Column, Integer, String, Text, DateTime, Numeric, ForeignKey, Table
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from ..session import Base

# --- Асоціативні таблиці для зв'язків Many-to-Many (Багато-до-Багатьох) ---

# Зв'язок касового апарата з товарами, дозволеними для продажу на ньому
cash_register_products = Table(
    'cash_register_products',
    Base.metadata,
    Column('cash_register_id', Integer, ForeignKey('cash_registers.id', ondelete="CASCADE"), primary_key=True),
    Column('product_id', Integer, ForeignKey('products.id', ondelete="CASCADE"), primary_key=True)
)

# Зв'язок каси з товарними групами (категоріями)
cash_register_groups = Table(
    'cash_register_groups',
    Base.metadata,
    Column('cash_register_id', Integer, ForeignKey('cash_registers.id', ondelete="CASCADE"), primary_key=True),
    Column('group_id', Integer, ForeignKey('group_items.id', ondelete="CASCADE"), primary_key=True)
)

# Зв'язок каси зі ставками ПДВ/акцизу, запрограмованими в її фіскальну пам'ять
cash_register_pdv = Table(
    'cash_register_pdv',
    Base.metadata,
    Column('cash_register_id', Integer, ForeignKey('cash_registers.id', ondelete="CASCADE"), primary_key=True),
    Column('pdv_id', Integer, ForeignKey('pdv.id', ondelete="CASCADE"), primary_key=True)
)

# Зв'язок каси з довідником кодів УКТЗЕД для підакцизних товарів
cash_register_uktz = Table(
    'cash_register_uktz',
    Base.metadata,
    Column('cash_register_id', Integer, ForeignKey('cash_registers.id', ondelete="CASCADE"), primary_key=True),
    Column('uktz_id', Integer, ForeignKey('uktz.id', ondelete="CASCADE"), primary_key=True)
)

# Зв'язок каси з касирами (користувачами), які мають право відкривати зміну на цій касі
cash_register_users = Table(
    'cash_register_users',
    Base.metadata,
    Column('cash_register_id', Integer, ForeignKey('cash_registers.id', ondelete="CASCADE"), primary_key=True),
    Column('user_id', Integer, ForeignKey('users.id', ondelete="CASCADE"), primary_key=True)
)


class CashRegister(Base):
    """
    Сутність фізичного фіскального реєстратора розрахункових операцій (РРО).

    Зберігає технічні та облікові параметри апарата (Datecs / Exellio), його фізичне розташування
    та зв'язки з компанією, магазином, списком товарів та касирів.
    """
    __tablename__ = 'cash_registers'

    id = Column(Integer, primary_key=True)
    name = Column(Text, nullable=False)           # Внутрішня назва каси (наприклад "Каса №1 (Бар)")
    model = Column(Text, default='')              # Модель реєстратора (наприклад "Exellio DP-05")
    serial_number = Column(Text)                  # Заводський / Фіскальний номер апарата
    location = Column(Text, default='')           # Опис фізичного розташування в торговому залі
    created_at = Column(DateTime, server_default=func.now())
    company_id = Column(Integer, ForeignKey('companies.id', ondelete="SET NULL"), nullable=False)
    store_id = Column(Integer, ForeignKey('stores.id', ondelete="SET NULL"))

    # Зв'язки One-to-Many / Many-to-One
    company = relationship("Company", back_populates="cash_registers")
    store = relationship("Store", back_populates="cash_registers")
    orders = relationship("Order", back_populates="cash_register", cascade="all, delete-orphan")

    # Зв'язки Many-to-Many через проміжні таблиці
    products = relationship("Product", secondary=cash_register_products, back_populates="cash_registers")
    groups = relationship("GroupItem", secondary=cash_register_groups, back_populates="cash_registers")
    pdvs = relationship("Pdv", secondary=cash_register_pdv, back_populates="cash_registers")
    uktzs = relationship("Uktz", secondary=cash_register_uktz, back_populates="cash_registers")
    users = relationship("User", secondary=cash_register_users, back_populates="cash_registers")


class Order(Base):
    """
    Сутність касового замовлення / фіскального чека.

    Фіксує факт та фінансові параметри продажу товарів касиром на конкретному РРО.
    Примітка: У поточній схемі замовлення зберігає дані однієї товарної позиції;
    заплановано нормалізацію до схеми Order (чек) -> OrderItem (позиції чека).
    """
    __tablename__ = 'orders'

    id = Column(Integer, primary_key=True)
    cash_register_id = Column(Integer, ForeignKey('cash_registers.id', ondelete="CASCADE"))
    customer_name = Column(String(100), nullable=True)     # Ім'я клієнта (для програм лояльності або B2B)
    status = Column(String(20), default="completed")       # Статус чека (completed, voided, pending)
    receipt_id = Column(String(100), nullable=True)        # Клієнтський UUID чека
    fiscal_check_number = Column(String(50), nullable=True)# Фіскальний номер з РРО
    payment_type = Column(Integer, default=0)              # 0 = готівка, 1 = картка
    total_price = Column(Numeric(10, 2))                   # Загальна вартість чека
    created_at = Column(DateTime, server_default=func.now())
    user_id = Column(Integer, ForeignKey('users.id', ondelete="SET NULL"))

    # Зв'язки
    cash_register = relationship("CashRegister", back_populates="orders")
    user = relationship("User", back_populates="orders")
    items = relationship("OrderItem", back_populates="order", cascade="all, delete-orphan")


class OrderItem(Base):
    """
    Товарна позиція фіскалізованого чека / замовлення.
    """
    __tablename__ = 'order_items'

    id = Column(Integer, primary_key=True)
    order_id = Column(Integer, ForeignKey('orders.id', ondelete="CASCADE"), nullable=False)
    product_id = Column(Integer, nullable=True)
    product_name = Column(Text, nullable=False)
    product_price = Column(Numeric(10, 2), nullable=False)
    quantity = Column(Numeric(10, 3), nullable=False)
    total_price = Column(Numeric(10, 2), nullable=False)
    excise_stamp = Column(String(100), nullable=True)      # Серія та номер марки акцизу
    tax_group = Column(String(10), default="А")            # Податкова ставка (А, Б, В тощо)
    uktz_code = Column(String(20), nullable=True)          # Код УКТЗЕД

    # Зворотний зв'язок до замовлення
    order = relationship("Order", back_populates="items")