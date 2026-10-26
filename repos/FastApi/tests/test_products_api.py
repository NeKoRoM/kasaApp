from decimal import Decimal
import pytest


def test_create_and_get_product(client, auth_headers, test_infrastructure):
    create_payload = {
        "name": "Чай Чорний Цейлон",
        "price": "65.50",
        "barcode_1": "482000000099",
        "weight_or_unit": "0",
        "unit_of_measurement": "шт",
        "cash_register_ids": [1]
    }

    create_resp = client.post("/products/", json=create_payload, headers=auth_headers)
    assert create_resp.status_code == 200

    created = create_resp.json()
    assert created["id"] > 0
    assert created["name"] == "Чай Чорний Цейлон"
    assert created["barcode_1"] == "482000000099"
    product_id = created["id"]

    # Отримуємо один товар за ID
    get_resp = client.get(f"/products/{product_id}", headers=auth_headers)
    assert get_resp.status_code == 200
    assert get_resp.json()["id"] == product_id


def test_search_product_by_barcode_and_name(client, auth_headers, test_infrastructure):
    # Створюємо товар для пошуку
    payload = {
        "name": "Вода Артезіанська 2л",
        "price": "22.00",
        "barcode_1": "482000111222",
        "weight_or_unit": "0",
        "cash_register_ids": [1]
    }
    client.post("/products/", json=payload, headers=auth_headers)

    # 1. Пошук за штрих-кодом
    resp_barcode = client.get("/products/search?q=482000111222", headers=auth_headers)
    assert resp_barcode.status_code == 200
    results = resp_barcode.json()
    assert len(results) >= 1
    assert any(p["barcode_1"] == "482000111222" for p in results)

    # 2. Пошук за фрагментом назви
    resp_name = client.get("/products/search?q=артезіанська", headers=auth_headers)
    assert resp_name.status_code == 200
    results_name = resp_name.json()
    assert len(results_name) >= 1
    assert any("Артезіанська" in p["name"] for p in results_name)


def test_update_product(client, auth_headers, test_infrastructure):
    payload = {
        "name": "Печиво Вівсяне 300г",
        "price": "35.00",
        "cash_register_ids": [1]
    }
    create_resp = client.post("/products/", json=payload, headers=auth_headers)
    product_id = create_resp.json()["id"]

    update_payload = {
        "name": "Печиво Вівсяне з родзинками 300г",
        "price": "42.00"
    }
    update_resp = client.put(f"/products/{product_id}", json=update_payload, headers=auth_headers)
    assert update_resp.status_code == 200

    updated = update_resp.json()
    assert updated["name"] == "Печиво Вівсяне з родзинками 300г"
    assert Decimal(str(updated["price"])) == Decimal("42.00")


def test_delete_product(client, auth_headers, test_infrastructure):
    payload = {
        "name": "Тимчасовий товар",
        "price": "10.00",
        "cash_register_ids": [1]
    }
    create_resp = client.post("/products/", json=payload, headers=auth_headers)
    product_id = create_resp.json()["id"]

    del_resp = client.delete(f"/products/{product_id}", headers=auth_headers)
    assert del_resp.status_code == 200
    assert "успішно видалено" in del_resp.json()["detail"]

    # Переконуємось, що товар більше не доступний
    get_resp = client.get(f"/products/{product_id}", headers=auth_headers)
    assert get_resp.status_code == 404
