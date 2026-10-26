#!/bin/bash
set -e

# Автоматичне визначення кореневої папки проекту
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="$PROJECT_ROOT/.jdk17/Contents/Home"

echo "=================================================="
echo "  Запуск KasaApp-v5 (Compose Desktop POS Terminal)"
echo "  Java Home: $JAVA_HOME"
echo "=================================================="

cd "$PROJECT_ROOT/kasaApp-v5"
./gradlew :desktopApp:run --quiet
