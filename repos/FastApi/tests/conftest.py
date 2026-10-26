import os
import sys
from pathlib import Path
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# Додаємо app у sys.path
BASE_DIR = Path(__file__).parent.parent
sys.path.insert(0, str(BASE_DIR / "app"))

from main import app
from db.session import Base, get_db
from db.models import Company, Store, CashRegister, User
from db.models.users import Role
from auth.utils import encode_jwt
from core.config import SUPER_ADMIN_ROLE_ID

from sqlalchemy.pool import StaticPool

# SQLite in-memory для швидких ізольованих тестів
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"
engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(autouse=True)
def setup_database():
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)


@pytest.fixture
def db_session():
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture(autouse=True)
def override_db(db_session):
    def _get_test_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = _get_test_db
    yield
    app.dependency_overrides.pop(get_db, None)


@pytest.fixture
def client():
    return TestClient(app)


@pytest.fixture
def test_user(db_session) -> User:
    role = db_session.query(Role).filter(Role.id == SUPER_ADMIN_ROLE_ID).first()
    if not role:
        role = Role(id=SUPER_ADMIN_ROLE_ID, name="SuperAdmin", description="SuperAdmin Role")
        db_session.add(role)
        db_session.flush()

    user = User(
        id=1,
        username="admin_test",
        email="admin@test.ua",
        password_hash="fake_hash",
        role_id=SUPER_ADMIN_ROLE_ID,
        is_active=True
    )
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    return user


@pytest.fixture
def test_infrastructure(db_session, test_user):
    company = Company(id=1, name="ТОВ Тест Рітейл", phone="+380501234567")
    db_session.add(company)
    db_session.flush()

    store = Store(id=1, name="Магазин №1", company_id=company.id)
    db_session.add(store)
    db_session.flush()

    cash_register = CashRegister(id=1, name="Каса №1 (Бар)", company_id=company.id, store_id=store.id)
    db_session.add(cash_register)
    db_session.commit()

    return {
        "company": company,
        "store": store,
        "cash_register": cash_register,
        "user": test_user
    }


@pytest.fixture
def auth_headers(test_user) -> dict[str, str]:
    token = encode_jwt({"sub": str(test_user.id), "username": test_user.username, "role_id": test_user.role_id})
    return {"Authorization": f"Bearer {token}"}
