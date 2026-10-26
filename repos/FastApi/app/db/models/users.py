"""
Назва файлу: users.py
Модуль: db.models
Призначення: Моделі користувачів, прав доступу (RBAC), сесій та налаштувань.
Архітектурна роль:
  • User — центральна сутність користувача системи (касир, адміністратор, власник).
  • Role — ролі доступу (SuperAdmin = 100, Admin = 1, Cashier/User = 2).
  • UserSession — активні сесії та токени входу.
  • PasswordReset — тимчасові токени для безпечного скидання пароля.
  • UserSetting — персональні користувацькі налаштування ключ-значення.
"""

from sqlalchemy import Column, Integer, String, Text, Boolean, DateTime, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from ..session import Base
from sqlalchemy import JSON
from .organization import Company, Store, user_company_association, user_store_association


class Role(Base):
    """
    Сутність системної ролі користувача (Role-Based Access Control).

    Визначає рівень привілеїв:
      • ID 100: SuperAdmin (головний системний адміністратор)
      • ID 1:   Admin (менеджер мережі магазинів)
      • ID 2:   User / Cashier (касир торговельної точки)
    """
    __tablename__ = 'roles'

    id = Column(Integer, primary_key=True)
    name = Column(String(50), unique=True, nullable=False) # Назва ролі (SuperAdmin, Admin, Cashier)
    description = Column(Text)                            # Опис повноважень ролі
    users = relationship("User", back_populates="role")


class User(Base):
    """
    Сутність користувача (касира, адміністратора, власника).

    Зберігає облікові дані, геш пароля (bcrypt), стан активності та зв'язки
    з компаніями, магазинами, закріпленими РРО, замовленнями та створеними товарами.
    """
    __tablename__ = 'users'

    id = Column(Integer, primary_key=True)
    username = Column(String(50), unique=True, nullable=False) # Логін для входу в POS / систему
    email = Column(String(255), unique=True, nullable=False)    # Контактний та відновлювальний email
    password_hash = Column(String(255), nullable=False)        # Геш пароля (алгоритм bcrypt)
    role_id = Column(Integer, ForeignKey('roles.id'))          # Призначена роль
    first_name = Column(String(50))                            # Ім'я касира
    last_name = Column(String(50))                             # Прізвище касира
    phone = Column(String(20))                                 # Номер телефону
    created_at = Column(DateTime, server_default=func.now())
    last_login = Column(DateTime)                              # Час останнього успішного входу
    is_active = Column(Boolean, default=True)                  # Чи активний акаунт (блокування доступу)
    reset_token = Column(String(255))
    reset_token_expires = Column(DateTime)
    session_token = Column(String(255))
    avatar_url = Column(String(255))
    settings = Column(JSON)                                    # Додаткові налаштування у форматі JSON
    notes = Column(Text)                                       # Службові примітки
    last_password_change = Column(DateTime)

    # Зв'язки
    role = relationship("Role", back_populates="users")
    companies = relationship(
        "Company",
        secondary=user_company_association,
        back_populates="users"
    )
    stores = relationship(
        "Store",
        secondary=user_store_association,
        back_populates="users"
    )
    password_resets = relationship("PasswordReset", back_populates="user", cascade="all, delete-orphan")
    sessions = relationship("UserSession", back_populates="user", cascade="all, delete-orphan")
    user_settings = relationship("UserSetting", back_populates="user", cascade="all, delete-orphan")
    orders = relationship("Order", back_populates="user")
    cash_registers = relationship("CashRegister", secondary="cash_register_users", back_populates="users")

    # Зв'язки з довідниками, створеними цим користувачем
    group_items = relationship("GroupItem", back_populates="user")
    pdvs = relationship("Pdv", back_populates="user")
    uktzs = relationship("Uktz", back_populates="user")
    products = relationship("Product", back_populates="user")

    def is_owner_of(self, company):
        """Перевіряє, чи є користувач власником вказаної компанії."""
        from sqlalchemy.orm import object_session
        from sqlalchemy import select
        sess = object_session(self)
        if sess is None:
            return company in self.companies
        stmt = select(user_company_association.c.is_owner).where(
            user_company_association.c.user_id == self.id,
            user_company_association.c.company_id == company.id
        )
        val = sess.execute(stmt).scalar()
        return bool(val)

    def get_owned_companies(self):
        """Повертає список компаній, де цей користувач є зареєстрованим власником."""
        from sqlalchemy.orm import object_session
        from sqlalchemy import select
        sess = object_session(self)
        if sess is None:
            return self.companies
        stmt = select(user_company_association.c.company_id).where(
            user_company_association.c.user_id == self.id,
            user_company_association.c.is_owner.is_(True)
        )
        owned_ids = set(sess.execute(stmt).scalars().all())
        return [c for c in self.companies if c.id in owned_ids]

    @property
    def store_ids(self) -> list[int]:
        """Список ID закріплених магазинів."""
        return [s.id for s in self.stores] if self.stores else []

    @property
    def company_ids(self) -> list[int]:
        """Список ID закріплених компаній."""
        return [c.id for c in self.companies] if self.companies else []


class PasswordReset(Base):
    """
    Сутність токенів відновлення пароля.

    Зберігає згенерований одноразовий токен та термін його валідності.
    """
    __tablename__ = 'password_resets'

    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey('users.id', ondelete="CASCADE"), nullable=False)
    reset_token = Column(String(255), nullable=False, unique=True)
    expires_at = Column(DateTime, nullable=False, server_default=func.now())
    created_at = Column(DateTime, server_default=func.now())

    user = relationship("User", back_populates="password_resets")


class UserSession(Base):
    """
    Сутність користувацької сесії.

    Відстежує активні сеанси роботи касирів або адміністраторів у системі.
    """
    __tablename__ = 'user_sessions'

    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey('users.id', ondelete="CASCADE"), nullable=False)
    session_token = Column(String(255), nullable=False, unique=True)
    created_at = Column(DateTime, server_default=func.now())
    expires_at = Column(DateTime, nullable=False)
    is_active = Column(Boolean, default=True)

    user = relationship("User", back_populates="sessions")


class UserSetting(Base):
    """
    Таблиця персональних налаштувань інтерфейсу та поведінки POS-клієнта.
    
    Зберігає пари setting_key -> setting_value з унікальним обмеженням на рівні користувача.
    """
    __tablename__ = 'user_settings'

    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey('users.id', ondelete="CASCADE"), nullable=False)
    setting_key = Column(String(50), nullable=False)
    setting_value = Column(Text)
    updated_at = Column(DateTime, server_default=func.now())

    user = relationship("User", back_populates="user_settings")

    __table_args__ = (
        UniqueConstraint('user_id', 'setting_key', name='uq_user_setting'),
    )
