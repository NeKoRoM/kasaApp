"""
Ініціалізація пакета Flask-застосунку та фабрика додатків (Application Factory).
Шлях у системі: flask/app/__init__.py

Архітектурна роль:
- Реалізує патерн Application Factory (`create_app()`), що дозволяє створювати ізольовані екземпляри застосунку для тестування або різних середовищ.
- Завантажує конфігурацію з модуля config.Config.
- Працює в контексті застосунку (`app.app_context()`) для імпорту та реєстрації блюпринтів (Blueprints).
- Реєструє маршрути `main_bp` з модуля `.routes.main`.
"""

from flask import Flask

def create_app():
    """
    Фабрика створення екземпляра Flask-додатка.
    1. Ініціалізує WSGI-об'єкт Flask.
    2. Завантажує параметри конфігурації.
    3. У контексті додатку реєструє компонентні блюпринти (Blueprints).
    4. Повертає готовий до роботи об'єкт app.
    """
    app = Flask(__name__)
    app.config.from_object('config.Config')

    with app.app_context():
        # Відкладений імпорт блюпринтів для запобігання циклічним залежностям
        from .routes import main

        # Реєстрація головного блюпринта маршрутизації
        app.register_blueprint(main.main_bp)

        return app

