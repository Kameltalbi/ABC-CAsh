#!/usr/bin/env bash
# Déploie ABC Cash API depuis votre Mac vers le VPS
set -euo pipefail

VPS="${VPS:-root@213.130.144.183}"
APP_DIR="/var/www/abc-cash"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> Build JAR Kotlin..."
cd "$ROOT"
./gradlew :server:jar --quiet

JAR="$ROOT/server/build/libs/server-1.0.0.jar"
if [[ ! -f "$JAR" ]]; then
  echo "ERREUR: JAR introuvable: $JAR"
  exit 1
fi

echo "==> Envoi vers $VPS:$APP_DIR ..."
ssh "$VPS" "mkdir -p $APP_DIR/server/build/libs $APP_DIR/deploy"

rsync -avz "$JAR" "$VPS:$APP_DIR/server/build/libs/"
rsync -avz \
  "$ROOT/deploy/docker-compose.yml" \
  "$ROOT/deploy/Dockerfile" \
  "$ROOT/deploy/.env.example" \
  "$ROOT/deploy/setup-vps.sh" \
  "$ROOT/deploy/nginx-abc-cash.conf.example" \
  "$VPS:$APP_DIR/deploy/"

echo "==> Installation sur le VPS..."
ssh "$VPS" "chmod +x $APP_DIR/deploy/setup-vps.sh && APP_DIR=$APP_DIR bash $APP_DIR/deploy/setup-vps.sh"

echo ""
echo "Sur le VPS, configurez Nginx si pas déjà fait:"
echo "  include snippets/abc-cash.conf;  # dans votre bloc server { }"
echo "  nginx -t && systemctl reload nginx"
echo ""
echo "Test:"
echo "  curl http://127.0.0.1:8081/health"
echo "  curl http://213.130.144.183/abc-cash/health"
