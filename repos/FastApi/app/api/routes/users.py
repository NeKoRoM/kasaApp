"""
Модуль API маршрутизації для управління користувачами (персоналом, касирами, адміністраторами) та ролями.
Шлях у системі: FastApi/app/api/routes/users.py

Архітектурна роль:
1. Повноцінний RBAC (Role-Based Access Control) модуль:
   - Керування обліковими записами персоналу (касири, менеджери, власники).
   - Призначення системних ролей (SUPER_ADMIN=100, USER_ROLE=1, CASHIER=2 тощо).
2. Багатокористувацька multi-tenant модель прив'язки:
   - Багато-до-багатьох (M2M) між User та Store через user_store_association.
   - Багато-до-багатьох (M2M) між User та Company через user_company_association (з прапорцем is_owner).
3. Безпека:
   - Усі чутливі операції створення/зміни користувачів та ролей вимагають ролі SUPER_ADMIN.
   - Паролі гешуються через bcrypt (auth_utils.hash_password) перед збереженням.
"""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from db.models.organization import user_company_association, user_store_association
from db.session import get_db
from db.models.users import User, Role
from auth.dependencies import require_roles
from auth import utils as auth_utils
from core.config import SUPER_ADMIN_ROLE_ID
from schemas.user_schema import (
    UserBase,
    UserCreate,
    UserUpdate,
    UserOut,
    RoleBase,
    RoleCreate,
    RoleOut,
)

__all__ = [
    "router",
    "UserBase",
    "UserCreate",
    "UserUpdate",
    "UserOut",
    "RoleBase",
    "RoleCreate",
    "RoleOut",
]

# Ініціалізація маршрутизатора користувачів із префіксом /users
router = APIRouter(prefix="/users", tags=["Users"])


# ============================================================================
# User Endpoints (Управління користувачами)
# ============================================================================

# GET /users/
@router.get("/", response_model=list[UserOut])
def get_users(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID]))
) -> list[UserOut]:
    """
    Отримання повного списку всіх користувачів системи.
    Доступ дозволено виключно для ролі SUPER_ADMIN_ROLE_ID.
    """
    users = db.query(User).all()
    return users


# GET /users/{user_id}
@router.get("/{user_id}", response_model=UserOut)
def get_user(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID]))
) -> User:
    """
    Отримання детальної інформації про користувача за його ID.
    Повертає 404, якщо користувача з таким ID не існує в БД.
    """
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user


# POST /users/
@router.post("/", response_model=UserOut)
def create_user(
    user: UserCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID]))
) -> User:
    """
    Створення нового користувача (касира або адміністратора).

    Послідовність виконання:
    1. Перевірка на унікальність логіна (username) або email.
       Якщо запис існує — генерується помилка HTTP 400.
    2. Гешування сирого пароля за допомогою bcrypt через auth_utils.hash_password.
    3. Вилучення поля 'password' із вхідних даних для запобігання збереженню відкритого тексту.
    4. Створення моделі User із передачею загешованого password_hash.
    5. Збереження в базі даних та повернення створеного користувача.
    """
    # Перевірка конфліктів унікальних полів
    if db.query(User).filter((User.username == user.username) | (User.email == user.email)).first():
        raise HTTPException(status_code=400, detail="Username or email already exists")

    # Безпечне гешування пароля
    hashed_password = auth_utils.hash_password(user.password).decode()
    user_data = user.model_dump(exclude={"password", "store_ids", "company_ids", "is_owner"})
    db_user = User(**user_data, password_hash=hashed_password)

    db.add(db_user)
    db.commit()
    db.refresh(db_user)

    # Прив'язка до магазинів
    if user.store_ids:
        for store_id in user.store_ids:
            db.execute(user_store_association.insert().values(user_id=db_user.id, store_id=store_id))

    # Прив'язка до компаній
    if user.company_ids:
        for company_id in user.company_ids:
            db.execute(
                user_company_association.insert().values(
                    user_id=db_user.id,
                    company_id=company_id,
                    is_owner=bool(user.is_owner)
                )
            )

    if user.store_ids or user.company_ids:
        db.commit()
        db.refresh(db_user)

    return db_user


# PUT /users/{user_id}
@router.put("/{user_id}", response_model=UserOut, description="ALERT: company and store updates: DELETING all existing associations and add new ones;")
def update_user(
    user_id: int,
    user_update: UserUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID]))
) -> User:
    """
    Оновлення даних користувача та його прав доступу до магазинів і компаній.

    Особливість синхронізації зв'язків Many-to-Many:
    - Якщо передано store_ids: спочатку видаляються ВСІ старі зв'язки в таблиці
      user_store_association для цього user_id, після чого вставляються нові.
    - Якщо передано company_ids: аналогічно видаляються всі старі записи в
      user_company_association, після чого записуються нові прив'язки зі статусом is_owner.
    """
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    # 1. Оновлення скалярних полів профілю користувача
    for field, value in user_update.model_dump(exclude_unset=True, exclude={"store_ids", "company_ids", "is_owner"}).items():
        setattr(user, field, value)

    # 2. Оновлення асоціацій із торговими точками (магазинами)
    if user_update.store_ids is not None:
        # Видалення попередніх прив'язок
        db.execute(user_store_association.delete().where(user_store_association.c.user_id == user_id))
        # Додавання нового переліку магазинів
        for store_id in user_update.store_ids:
            db.execute(user_store_association.insert().values(user_id=user_id, store_id=store_id))

    # 3. Оновлення асоціацій із компаніями (підприємствами)
    if user_update.company_ids is not None:
        # Видалення старих зв'язків
        db.execute(user_company_association.delete().where(user_company_association.c.user_id == user_id))
        # Додавання нових компаній із зазначенням статусу власника
        for company_id in user_update.company_ids:
            is_owner = user_update.is_owner if user_update.is_owner is not None else False
            db.execute(
                user_company_association.insert().values(
                    user_id=user_id,
                    company_id=company_id,
                    is_owner=is_owner
                )
            )

    db.commit()
    db.refresh(user)
    return user


# ============================================================================
# Role Endpoints (Управління системними ролями)
# ============================================================================

# GET /roles/
@router.get("/roles/", response_model=list[RoleOut])
def get_roles(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID]))
) -> list[Role]:
    """
    Отримання довідника всіх ролей у системі.
    Вимагає прав SUPER_ADMIN_ROLE_ID.
    """
    roles = db.query(Role).all()
    return roles


# POST /roles/
@router.post("/roles/", response_model=RoleOut)
def create_role(
    role: RoleCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID]))
) -> Role:
    """
    Створення нової ролі в системі контролю доступу (RBAC).
    Перевіряє унікальність назви ролі (Role.name).
    """
    if db.query(Role).filter(Role.name == role.name).first():
        raise HTTPException(status_code=400, detail="Role already exists")
    new_role = Role(name=role.name, description=role.description)
    db.add(new_role)
    db.commit()
    db.refresh(new_role)
    return new_role

