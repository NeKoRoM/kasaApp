from decimal import Decimal
import pytest


def test_create_order_success(client, auth_headers, test_infrastructure):
    payload = {
        "cash_register_id": 1,
        "receipt_id": "test-uuid-receipt-001",
        "fiscal_check_number": "000123",
        "total_price": "250.00",
        "payment_type": 0,
        "items": [
            {
                "product_id": 1,
                "product_name": "Хліб Житній 500г",
                "product_price": "25.00",
                "quantity": "2.000",
                "total_price": "50.00",
                "tax_group": "А"
            },
            {
                "product_id": 2,
                "product_name": "Коньяк 0.5л",
                "product_price": "200.00",
                "quantity": "1.000",
                "total_price": "200.00",
                "excise_stamp": "AB123456CD",
                "tax_group": "А",
                "uktz_code": "2208200000"
            }
        ]
    }

    response = client.post("/orders/", json=payload, headers=auth_headers)
    assert response.status_code == 201

    data = response.json()
    assert data["id"] > 0
    assert data["receipt_id"] == "test-uuid-receipt-001"
    assert data["fiscal_check_number"] == "000123"
    assert Decimal(str(data["total_price"])) == Decimal("250.00")
    assert data["status"] == "completed"
    assert len(data["items"]) == 2

    excise_item = next(it for it in data["items"] if it["product_name"] == "Коньяк 0.5л")
    assert excise_item["excise_stamp"] == "AB123456CD"
    assert Decimal(str(excise_item["total_price"])) == Decimal("200.00")


def test_create_order_unknown_cash_register(client, auth_headers, test_infrastructure):
    payload = {
        "cash_register_id": 9999,
        "receipt_id": "invalid-reg-uuid",
        "total_price": "100.00",
        "items": []
    }

    response = client.post("/orders/", json=payload, headers=auth_headers)
    assert response.status_code == 404
    assert "не знайдено" in response.json()["detail"]


def test_get_orders_list(client, auth_headers, test_infrastructure):
    # Створюємо одне тестове замовлення
    payload = {
        "cash_register_id": 1,
        "receipt_id": "test-uuid-receipt-002",
        "fiscal_check_number": "000124",
        "total_price": "50.00",
        "payment_type": 1,
        "items": [
            {
                "product_name": "Кава Арабіка",
                "product_price": "50.00",
                "quantity": "1.000",
                "total_price": "50.00",
                "tax_group": "А"
            }
        ]
    }
    create_resp = client.post("/orders/", json=payload, headers=auth_headers)
    assert create_resp.status_code == 201

    # Запитуємо історію чеків для каси 1
    response = client.get("/orders/?cash_register_id=1", headers=auth_headers)
    assert response.status_code == 200

    data = response.json()
    assert isinstance(data, list)
    assert len(data) >= 1
    assert data[0]["receipt_id"] == "test-uuid-receipt-002"
    assert len(data[0]["items"]) == 1


def test_unauthorized_order_request_fails(client):
    response = client.get("/orders/")
    assert response.status_code in (401, 403)


def test_create_refund_order_success(client, auth_headers, test_infrastructure):
    payload = {
        "cash_register_id": 1,
        "receipt_id": "test-uuid-refund-001",
        "fiscal_check_number": "000125",
        "total_price": "50.00",
        "payment_type": 0,
        "status": "refund",
        "items": [
            {
                "product_id": 1,
                "product_name": "Хліб Житній 500г",
                "product_price": "25.00",
                "quantity": "2.000",
                "total_price": "50.00",
                "tax_group": "А"
            }
        ]
    }
    response = client.post("/orders/", json=payload, headers=auth_headers)
    assert response.status_code == 201
    data = response.json()
    assert data["status"] == "refund"
    assert data["receipt_id"] == "test-uuid-refund-001"
    assert len(data["items"]) == 1

