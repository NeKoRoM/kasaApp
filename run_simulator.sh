#!/bin/bash
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
SIMULATOR_DIR="$PROJECT_ROOT/datecs-simulator"

echo "=================================================="
echo "  Запуск Datecs / Exellio РРО Симулятора v2.11"
echo "=================================================="
echo "  TCP Сервер:       0.0.0.0:4999"
echo "  Віртуальний PTY:  $SIMULATOR_DIR/kasa_serial_port"
echo "  Веб-дашборд:      http://localhost:8088"
echo "=================================================="

cd "$SIMULATOR_DIR"
python3 datecs_simulator_main.py "$@"
