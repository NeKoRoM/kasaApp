#!/bin/bash
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT/repos/FastApi"

export PYTHONPATH="$PROJECT_ROOT/repos/FastApi/app:$PROJECT_ROOT/repos/FastApi:${PYTHONPATH:-}"

echo "=================================================="
echo "  Запуск FastAPI Backend Сервера (Порт 8000)"
echo "=================================================="

if [ -f ".venv/bin/uvicorn" ]; then
    .venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
else
    uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
fi
