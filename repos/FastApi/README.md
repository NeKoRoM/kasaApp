


# Запуск додатку




## open 2 diferent console
for first console:
### docker compose run
for second console:
### docker run -ti -p 8080:80 -v ./app:/app -v .env:/app/.env --network app_network nekorom/docker-web:15 uvicorn --reload main:app --host 0.0.0.0 --port 80
# database
 database role superAdmin is in id 100 its used for all credentials


.

├── .venv/
├── app/
│   ├── api/

│   │   ├── routes/

│   │   │   ├── __init__.py

│   │   │   └── user.py

│   │   └── __init__.py

│   ├── core/

│   │   ├── __init__.py

│   │   └── config.py

│   ├── db/

│   │   ├── models/

│   │   │   ├── __init__.py

│   │   │   └── user.py

│   │   ├── __init__.py

│   │   ├── init_db.py

│   │   └── session.py

│   └── __init__.py

├── .env

├── .gitignore

├── Dockerfile

├── docker-compose.yml

├── main.py

├── README.md

└── requirements.txt