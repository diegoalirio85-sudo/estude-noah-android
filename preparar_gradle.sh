#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$PROJECT_DIR/gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"
URL="https://github.com/gradle/gradle/raw/refs/tags/v9.4.1/gradle/wrapper/gradle-wrapper.jar"
EXPECTED="55243ef57851f12b070ad14f7f5bb8302daceeebc5bce5ece5fa6edb23e1145c"

mkdir -p "$WRAPPER_DIR"
echo "Baixando Gradle Wrapper 9.4.1 do repositório oficial do Gradle..."
curl -L "$URL" -o "$WRAPPER_JAR"
ACTUAL=$(sha256sum "$WRAPPER_JAR" | awk '{print $1}')

if [ "$ACTUAL" != "$EXPECTED" ]; then
  rm -f "$WRAPPER_JAR"
  echo "Checksum do Gradle Wrapper não confere. Arquivo removido por segurança." >&2
  exit 1
fi

echo "Gradle Wrapper verificado com sucesso."
