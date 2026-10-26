import pytest
from auth.utils import encode_jwt
from db.models.users import User, Role
from core.config import USER_ROLE_ID


def test_create_and_get_company(client, auth_headers):
    payload = {
        "name": "ТОВ Нова Компанія",
        "address": "м. Київ, вул. Хрещатик 1",
        "phone": "+380441112233",
        "email": "info@novakompaniya.ua"
    }
    create_resp = client.post("/Organization/companies/", json=payload, headers=auth_headers)
    assert create_resp.status_code == 200
    created = create_resp.json()
    assert created["id"] > 0
    assert created["name"] == "ТОВ Нова Компанія"
    company_id = created["id"]

    get_resp = client.get(f"/Organization/companies/{company_id}", headers=auth_headers)
    assert get_resp.status_code == 200
    data = get_resp.json()
    assert data["id"] == company_id
    assert data["name"] == "ТОВ Нова Компанія"


def test_get_companies_list(client, auth_headers, test_infrastructure):
    resp = client.get("/Organization/companies/", headers=auth_headers)
    assert resp.status_code == 200
    companies = resp.json()
    assert len(companies) >= 1
    assert any(c["id"] == test_infrastructure["company"].id for c in companies)


def test_update_company(client, auth_headers, test_infrastructure):
    company_id = test_infrastructure["company"].id
    update_payload = {
        "name": "ТОВ Тест Рітейл Модифікований",
        "phone": "+380509998877"
    }
    resp = client.put(f"/Organization/companies/{company_id}", json=update_payload, headers=auth_headers)
    assert resp.status_code == 200
    updated = resp.json()
    assert updated["name"] == "ТОВ Тест Рітейл Модифікований"
    assert updated["phone"] == "+380509998877"


def test_company_scoping_for_cashier_user(client, db_session, test_infrastructure):
    # Створюємо роль звичайного касира (USER_ROLE_ID = 2)
    role = db_session.query(Role).filter(Role.id == USER_ROLE_ID).first()
    if not role:
        role = Role(id=USER_ROLE_ID, name="Cashier", description="Cashier Role")
        db_session.add(role)
        db_session.flush()

    user = User(
        id=20,
        username="cashier_test",
        email="cashier@test.ua",
        password_hash="fake_hash",
        role_id=USER_ROLE_ID,
        is_active=True
    )
    # Прив'язуємо до тестової компанії
    user.companies.append(test_infrastructure["company"])
    db_session.add(user)
    db_session.commit()

    token = encode_jwt({"sub": str(user.id), "username": user.username, "role_id": user.role_id})
    cashier_headers = {"Authorization": f"Bearer {token}"}

    # Касир повинен бачити лише свою компанію
    resp = client.get("/Organization/companies/", headers=cashier_headers)
    assert resp.status_code == 200
    companies = resp.json()
    assert len(companies) == 1
    assert companies[0]["id"] == test_infrastructure["company"].id

    # Запит на неіснуючу або недоступну компанію
    resp_forbidden = client.get("/Organization/companies/9999", headers=cashier_headers)
    assert resp_forbidden.status_code == 404
