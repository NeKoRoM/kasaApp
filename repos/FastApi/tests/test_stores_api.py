import pytest
from auth.utils import encode_jwt
from db.models.users import User, Role
from core.config import USER_ROLE_ID


def test_create_and_get_store(client, auth_headers, test_infrastructure):
    company_id = test_infrastructure["company"].id
    payload = {
        "name": "Новий Магазин Центр",
        "address": "вул. Франка, 10",
        "phone": "+380501112244",
        "company_id": company_id
    }
    create_resp = client.post("/Organization/stores/", json=payload, headers=auth_headers)
    assert create_resp.status_code == 200
    created = create_resp.json()
    assert created["id"] > 0
    assert created["name"] == "Новий Магазин Центр"
    store_id = created["id"]

    get_resp = client.get(f"/Organization/stores/{store_id}", headers=auth_headers)
    assert get_resp.status_code == 200
    assert get_resp.json()["id"] == store_id
    assert get_resp.json()["name"] == "Новий Магазин Центр"


def test_get_stores_list(client, auth_headers, test_infrastructure):
    resp = client.get("/Organization/stores/", headers=auth_headers)
    assert resp.status_code == 200
    stores = resp.json()
    assert len(stores) >= 1
    assert any(s["id"] == test_infrastructure["store"].id for s in stores)


def test_update_store(client, auth_headers, test_infrastructure):
    store_id = test_infrastructure["store"].id
    payload = {
        "name": "Магазин №1 Оновлений",
        "phone": "+380509990011"
    }
    resp = client.put(f"/Organization/stores/{store_id}", json=payload, headers=auth_headers)
    assert resp.status_code == 200
    assert resp.json()["name"] == "Магазин №1 Оновлений"
    assert resp.json()["phone"] == "+380509990011"


def test_cashier_store_scoping(client, db_session, test_infrastructure):
    role = db_session.query(Role).filter(Role.id == USER_ROLE_ID).first()
    if not role:
        role = Role(id=USER_ROLE_ID, name="Cashier", description="Cashier Role")
        db_session.add(role)
        db_session.flush()

    user = User(
        id=30,
        username="cashier_store_test",
        email="cashier_store@test.ua",
        password_hash="fake_hash",
        role_id=USER_ROLE_ID,
        is_active=True
    )
    user.stores.append(test_infrastructure["store"])
    db_session.add(user)
    db_session.commit()

    token = encode_jwt({"sub": str(user.id), "username": user.username, "role_id": user.role_id})
    cashier_headers = {"Authorization": f"Bearer {token}"}

    resp = client.get("/Organization/stores/", headers=cashier_headers)
    assert resp.status_code == 200
    stores = resp.json()
    assert len(stores) == 1
    assert stores[0]["id"] == test_infrastructure["store"].id

    resp_forbidden = client.get("/Organization/stores/9999", headers=cashier_headers)
    assert resp_forbidden.status_code == 404
