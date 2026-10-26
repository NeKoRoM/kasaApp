import pytest
from auth.utils import hash_password
from db.models.users import User, Role
from core.config import SUPER_ADMIN_ROLE_ID, USER_ROLE_ID


def test_create_and_get_user(client, auth_headers, test_infrastructure):
    payload = {
        "username": "cashier_john",
        "email": "john@cashier.ua",
        "password": "SecretPassword123",
        "first_name": "Іван",
        "last_name": "Коваленко",
        "role_id": USER_ROLE_ID,
        "store_ids": [test_infrastructure["store"].id],
        "company_ids": [test_infrastructure["company"].id]
    }
    resp = client.post("/users/", json=payload, headers=auth_headers)
    assert resp.status_code == 200
    created = resp.json()
    assert created["id"] > 0
    assert created["username"] == "cashier_john"
    assert created["email"] == "john@cashier.ua"
    user_id = created["id"]

    # Отримання користувача за ID
    get_resp = client.get(f"/users/{user_id}", headers=auth_headers)
    assert get_resp.status_code == 200
    assert get_resp.json()["id"] == user_id


def test_get_users_list(client, auth_headers, test_user):
    resp = client.get("/users/", headers=auth_headers)
    assert resp.status_code == 200
    users = resp.json()
    assert len(users) >= 1
    assert any(u["id"] == test_user.id for u in users)


def test_update_user(client, auth_headers, test_infrastructure):
    # Створюємо користувача для редагування
    payload = {
        "username": "edit_user",
        "email": "edit@user.ua",
        "password": "Password123",
        "role_id": USER_ROLE_ID
    }
    create_resp = client.post("/users/", json=payload, headers=auth_headers)
    assert create_resp.status_code == 200
    user_id = create_resp.json()["id"]

    update_payload = {
        "first_name": "ОновленеІм'я",
        "phone": "+380509990022",
        "store_ids": [test_infrastructure["store"].id]
    }
    update_resp = client.put(f"/users/{user_id}", json=update_payload, headers=auth_headers)
    assert update_resp.status_code == 200
    data = update_resp.json()
    assert data["first_name"] == "ОновленеІм'я"
    assert data["phone"] == "+380509990022"


def test_roles_management(client, auth_headers):
    # Створення нової ролі
    create_payload = {"name": "Auditor", "description": "Financial auditor"}
    resp = client.post("/users/roles/", json=create_payload, headers=auth_headers)
    assert resp.status_code == 200
    role_id = resp.json()["id"]
    assert resp.json()["name"] == "Auditor"

    # Отримання списку ролей
    list_resp = client.get("/users/roles/", headers=auth_headers)
    assert list_resp.status_code == 200
    roles = list_resp.json()
    assert any(r["id"] == role_id for r in roles)


def test_login_jwt(client, db_session):
    # Створюємо користувача з відомим паролем
    hashed = hash_password("MySuperPassword999").decode()
    user = User(
        id=99,
        username="login_test_user",
        email="login_test@kasa.ua",
        password_hash=hashed,
        role_id=USER_ROLE_ID,
        is_active=True
    )
    db_session.add(user)
    db_session.commit()

    # Успішний вхід
    login_resp = client.post("/jwt/login/", json={
        "username": "login_test_user",
        "password": "MySuperPassword999"
    })
    assert login_resp.status_code == 200
    token_data = login_resp.json()
    assert "access_token" in token_data
    assert token_data["token_type"] == "Bearer"

    # Невірний пароль
    bad_resp = client.post("/jwt/login/", json={
        "username": "login_test_user",
        "password": "WrongPassword"
    })
    assert bad_resp.status_code == 401


def test_legacy_user_endpoints(client, auth_headers):
    # Тест застарілих маршрутів /old
    resp = client.get("/old/users", headers=auth_headers)
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)


def test_create_new_user_auth_route(client, auth_headers):
    payload = {
        "username": "auth_new_user",
        "email": "auth_new@kasa.ua",
        "password": "Password777",
        "first_name": "Тест",
        "last_name": "Аутентифікація",
        "role_id": 2
    }
    resp = client.post("/CreateNewUser", json=payload, headers=auth_headers)
    assert resp.status_code == 200
    assert resp.json()["status"] == "success"
    assert "auth_new_user" in resp.json()["message"]
