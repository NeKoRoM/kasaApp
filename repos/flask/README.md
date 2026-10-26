# Flask Project

This is a Flask project structured with Docker support.

## Project Structure

```
project/
├── app/
│   ├── __init__.py          # Ініціалізація Flask-додатку
│   ├── models/              # Моделі
│   ├── routes/              # Контролери
│   ├── services/            # Бізнес-логіка
│   ├── templates/           # HTML-шаблони (за потреби)
│   ├── static/              # Статичні файли
├── config.py                # Конфігурації
├── Dockerfile               # Докерізація
├── docker-compose.yml       # Оркестрація
└── run.py                   # Точка входу
```

## Instructions

1. Navigate to the `/workspaces/flask` directory.
2. Build the Docker image:
   ```sh
   docker-compose build
   ```
3. Run the Docker container:
   ```sh
   docker-compose up
   ```

Your Flask application should now be running inside a Docker container and accessible at `http://localhost:5000`.
