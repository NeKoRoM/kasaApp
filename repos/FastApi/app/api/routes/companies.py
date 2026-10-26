"""
Модуль API маршрутизації для управління компаніями (організаціями / юрособами / ФОП).
Шлях у системі: FastApi/app/api/routes/companies.py

Архітектурна роль у multi-tenant екосистемі KasaApp:
1. Компанія (Company) — кореневий рівень ієрархічної структури бізнесу:
   [Компанія (ФОП/ТОВ)] -> [Магазини (Stores)] -> [Касові апарати (CashRegisters)].
2. Управління правами доступу (RBAC):
   - SUPER_ADMIN (role_id=100) має повний доступ до всіх компаній у системі,
     може переглядати, створювати та редагувати будь-яку організацію.
   - Звичайний користувач (USER / Власник, role_id=1 або USER_ROLE_ID)
     має доступ лише до своїх власних компаній (перевірка Company.owner_id == current_user.id).
3. Валідація даних:
   - Використовуються Pydantic-схеми CompanyBase, CompanyCreate, CompanyUpdate, CompanyOut.
   - Забезпечується серіалізація пов'язаних сутностей: список користувачів (users) та магазинів (stores).
"""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from db.session import get_db
from db.models import Company
from db.models.organization import user_company_association
from db.models.users import User
from auth.dependencies import require_roles
from core.config import SUPER_ADMIN_ROLE_ID, USER_ROLE_ID
from schemas.company_schema import (
    CompanyBase,
    CompanyCreate,
    CompanyUpdate,
    UserOut,
    StoreOut,
    CompanyOut,
)

__all__ = [
    "router",
    "CompanyBase",
    "CompanyCreate",
    "CompanyUpdate",
    "UserOut",
    "StoreOut",
    "CompanyOut",
]

# Ініціалізація маршрутизатора FastApi для компаній із префіксом /Organization
router = APIRouter(prefix="/Organization", tags=["Companies"])


# ============================================================================
# API Endpoints (Кінцеві точки маршрутизації)
# ============================================================================

@router.post("/companies/", response_model=CompanyOut)
def create_company(
    company: CompanyCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> Company:
    """
    Створення нової компанії в системі.

    Авторизація:
    - Дозволено для SUPER_ADMIN_ROLE_ID та USER_ROLE_ID.

    Логіка роботи:
    1. Створюється об'єкт Company з валідними реквізитами (без user_ids, is_owner).
    2. Поточний користувач прив'язується до компанії у ролі власника (is_owner=True).
    3. Додаткові user_ids (якщо передані) прив'язуються до компанії.
    4. Повертається створена компанія у форматі CompanyOut.
    """
    company_data = company.model_dump(exclude={"user_ids", "is_owner"})
    new_company = Company(**company_data)
    db.add(new_company)
    db.commit()
    db.refresh(new_company)

    # Прив'язуємо творця компанії як власника (is_owner=True)
    db.execute(
        user_company_association.insert().values(
            user_id=current_user.id,
            company_id=new_company.id,
            is_owner=True
        )
    )

    if company.user_ids:
        for uid in company.user_ids:
            if uid != current_user.id:
                u = db.get(User, uid)
                if u:
                    new_company.users.append(u)

    db.commit()
    db.refresh(new_company)
    return new_company


@router.get("/companies/", response_model=list[CompanyOut])
def get_companies(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> list[Company]:
    """
    Отримання списку компаній з фільтрацією за ролями.

    Авторизація:
    - Дозволено для SUPER_ADMIN_ROLE_ID та USER_ROLE_ID.

    Логіка вибірки:
    - Якщо роль користувача USER_ROLE_ID: повертаються виключно компанії,
      до яких прив'язаний цей користувач (ізоляція даних між клієнтами).
    - Якщо роль користувача SUPER_ADMIN_ROLE_ID: повертаються всі компанії в базі.
    """
    if current_user.role_id == USER_ROLE_ID:
        companies = current_user.companies
    elif current_user.role_id == SUPER_ADMIN_ROLE_ID:
        companies = db.query(Company).all()
    else:
        companies = []
    return companies


@router.get("/companies/{company_id}", response_model=CompanyOut)
def get_company(
    company_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID, USER_ROLE_ID]))
) -> Company:
    """
    Отримання детальної інформації про конкретну компанію за її ID.

    Авторизація:
    - Доступно для SUPER_ADMIN_ROLE_ID та USER_ROLE_ID.

    Перевірка доступу (Data Scoping):
    - Для звичайного користувача перевіряється наявність компанії в current_user.companies.
    - Супер-адміністратор отримує компанію без обмеження власності.
    """
    company = None
    if current_user.role_id == USER_ROLE_ID:
        company = next((c for c in current_user.companies if c.id == company_id), None)
        if not company:
            raise HTTPException(status_code=404, detail="Company not found")
    elif current_user.role_id == SUPER_ADMIN_ROLE_ID:
        company = db.query(Company).filter(Company.id == company_id).first()
        if not company:
            raise HTTPException(status_code=404, detail="Company not found")

    return company


@router.put("/companies/{company_id}", response_model=CompanyOut, description="наразі тільки супер-адміністратор може змінювати компанії")
def update_company(
    company_id: int,
    company_update: CompanyUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles([SUPER_ADMIN_ROLE_ID]))
) -> Company:
    """
    Оновлення даних існуючої компанії.

    Авторизація:
    - Дозволено виключно для ролі SUPER_ADMIN_ROLE_ID.

    Логіка оновлення:
    1. Пошук компанії за company_id. Якщо не знайдено — HTTP 404.
    2. Ітерація по всіх полях Pydantic моделі, які були явно передані клієнтом (exclude_unset=True).
    3. Оновлення атрибутів моделі та списку користувачів.
    4. Збереження змін у БД (commit) та повернення оновленого запису.
    """
    company = db.query(Company).filter(Company.id == company_id).first()
    if not company:
        raise HTTPException(status_code=404, detail="Company not found")

    update_data = company_update.model_dump(exclude_unset=True)
    user_ids = update_data.pop("user_ids", None)
    update_data.pop("is_owner", None)

    for field, value in update_data.items():
        setattr(company, field, value)

    if user_ids is not None:
        users = db.query(User).filter(User.id.in_(user_ids)).all()
        company.users = users

    db.commit()
    db.refresh(company)
    return company


