"""
Назва файлу: product.py
Модуль: db.models
Призначення: Моделі бази даних для обліку товарної номенклатури, категорій, ставок ПДВ та кодів УКТЗЕД.
Архітектурна роль:
  • Product — центральна сутність товару, чиї атрибути (17 параметрів) 1-в-1 відповідають
    структурі команди програмування касового артикулу (PLU) фіскального реєстратора Exellio (006;P).
  • GroupItem — товарні групи/категорії з ознакою акцизного збору (group_excise).
  • Pdv — ставки податкових груп (ПДВ та роздрібного акцизу).
  • Uktz — класифікатор кодів УКТЗЕД (10 цифр), обов'язкових для підакцизних товарів.
"""

from decimal import Decimal
from sqlalchemy import Column, Integer, Text, Boolean, Numeric, ForeignKey
from sqlalchemy.orm import relationship
from ..session import Base


class Product(Base):
    """
    Сутність товару в каталозі.

    Атрибути розроблені для повної сумісності з пам'яттю апаратних кас Exellio/Datecs:
    підтримує до 4 окремих штрих-кодів, роздрібну ціну, залишок, накопичені продажі,
    ознаку ваги та прив'язку до фіскальних груп оподаткування.
    """
    __tablename__ = 'products'

    id = Column(Integer, primary_key=True, index=True)
    product_id = Column(Integer, default=0)                # Номер артикулу в пам'яті РРО (PLU: 1..100000)
    pdv_id = Column(Integer, ForeignKey('pdv.id'))         # Посилання на ставку ПДВ (Групи А..Д)
    group_id = Column(Integer, ForeignKey('group_items.id')) # Посилання на категорію товару
    uktz_id = Column(Integer, ForeignKey('uktz.id'))       # Посилання на код УКТЗЕД (для підакцизу)
    section = Column(Text, default='')                     # Номер відділу магазину (Department: 1..99)
    type = Column(Text, default='')                        # Тип ціни (фіксована або вільна)
    price = Column(Numeric(10, 2), default=Decimal('0.00'))              # Роздрібна ціна за одиницю
    sales_amount = Column(Numeric(10, 2), default=Decimal('0.00'))       # Сума накопичених продажів за зміну
    sales_count = Column(Integer, default=0)               # Кількість здійснених продажів
    remainder = Column(Numeric(10, 2), default=Decimal('0.00'))          # Поточний товарний залишок на складі
    barcode_1 = Column(Text, default='')                   # Основний штрих-код (EAN-13 / UPC)
    barcode_2 = Column(Text, default='')                   # Додатковий штрих-код №2
    barcode_3 = Column(Text, default='')                   # Додатковий штрих-код №3
    barcode_4 = Column(Text, default='')                   # Додатковий штрих-код №4
    name = Column(Text, default='')                        # Назва товару (до 40 символів для друку на чеку)
    weight_or_unit = Column(Text, default='')              # Ознака товару: "0" = штучний, "1" = ваговий
    unit_of_measurement = Column(Text, default='')         # Одиниця виміру (шт, кг, л, м)
    is_independent = Column(Boolean, default=False)        # Прапорець локального товару каси
    user_id = Column(Integer, ForeignKey('users.id', ondelete="SET NULL"), nullable=True) # Ідентифікатор власника/касира

    # Зв'язки
    user = relationship("User", back_populates="products")
    pdv = relationship("Pdv", back_populates="products")
    group = relationship("GroupItem", back_populates="products")
    uktz = relationship("Uktz", back_populates="products")
    cash_registers = relationship("CashRegister", secondary="cash_register_products", back_populates="products")


class GroupItem(Base):
    """
    Сутність товарної групи (категорії).

    Містить критично важливий для фіскального обліку прапорець group_excise:
    якщо group_excise == True, клієнтський POS-додаток зобов'язаний вимагати сканування
    марки акцизного податку при додаванні будь-якого товару цієї групи у чек.
    """
    __tablename__ = 'group_items'

    id = Column(Integer, primary_key=True, index=True)
    group_name = Column(Text, default='')                  # Назва групи (наприклад "Алкогольні напої", "Хлібобулочні")
    group_value = Column(Text)                             # Числовий ідентифікатор групи в касі (1..99)
    group_excise = Column(Boolean, default=False)          # Ознака підакцизності товарів у цій групі
    user_id = Column(Integer, ForeignKey('users.id', ondelete="SET NULL"), nullable=True)

    # Зв'язки
    user = relationship("User", back_populates="group_items")
    products = relationship("Product", back_populates="group")
    cash_registers = relationship("CashRegister", secondary="cash_register_groups", back_populates="groups")


class Pdv(Base):
    """
    Сутність ставки ПДВ (податкової групи).

    Визначає правила нарахування основного податку (ПДВ) та додаткового роздрібного акцизу (5%).
    У касах Datecs/Exellio податкові групи позначаються літерами (А, Б, В, Г, Д).
    """
    __tablename__ = 'pdv'

    id = Column(Integer, primary_key=True, index=True)
    pdv_id = Column(Integer)                               # Індекс ставки в пам'яті РРО (0..7)
    pdv_name = Column(Text, default='')                    # Літерний символ ставки (А, Б, В тощо)
    pdv_nds = Column(Text, default='')                     # Ставка ПДВ у відсотках (наприклад "20.00")
    pdv_excise = Column(Text, default='')                  # Додатковий акцизний збір (наприклад "5.00")
    user_id = Column(Integer, ForeignKey('users.id', ondelete="SET NULL"), nullable=True)

    # Зв'язки
    user = relationship("User", back_populates="pdvs")
    products = relationship("Product", back_populates="pdv")
    cash_registers = relationship("CashRegister", secondary="cash_register_pdv", back_populates="pdvs")


class Uktz(Base):
    """
    Сутність класифікатора кодів УКТЗЕД.

    Українська класифікація товарів зовнішньоекономічної діяльності (10 цифр).
    Відповідно до закону України про РРО, друк коду УКТЗЕД у фіскальному чеку
    є суворо обов'язковим для всіх підакцизних товарів.
    """
    __tablename__ = 'uktz'

    id = Column(Integer, primary_key=True, index=True)
    pdv_name = Column(Text, default='')                    # Назва товарної підгрупи за класифікатором
    pdv_value = Column(Text, default='')                   # 10-значний числовий код УКТЗЕД
    user_id = Column(Integer, ForeignKey('users.id', ondelete="SET NULL"), nullable=True)

    # Зв'язки
    user = relationship("User", back_populates="uktzs")
    products = relationship("Product", back_populates="uktz")
    cash_registers = relationship("CashRegister", secondary="cash_register_uktz", back_populates="uktzs")