#!/usr/bin/env bash
# Diagnostic rapide sur le VPS
set -euo pipefail

if docker compose version &>/dev/null; then
  DC="docker compose"
else
  DC="docker-compose"
fi

APP_DIR="${APP_DIR:-/var/www/abc-cash}"
cd "$APP_DIR/deploy"

echo "=== Docker ==="
docker --version
$DC version 2>/dev/null || true

echo ""
echo "=== Conteneurs ==="
$DC ps -a 2>/dev/null || true
docker ps -a --filter name=abc-cash 2>/dev/null || true

echo ""
echo "=== Port 8081 ==="
ss -tlnp | grep 8081 || echo "Rien n'écoute sur 8081"

echo ""
echo "=== .env (sans mots de passe) ==="
grep -E '^(PORT|HOST|DATABASE_URL|POSTGRES_DB)=' .env 2>/dev/null || echo ".env manquant"

echo ""
echo "=== Logs API (30 dernières lignes) ==="
$DC logs abc-cash-api --tail 30 2>/dev/null || echo "Conteneur abc-cash-api absent"

echo ""
echo "=== Logs DB (10 dernières lignes) ==="
$DC logs abc-cash-db --tail 10 2>/dev/null || echo "Conteneur abc-cash-db absent"

echo ""
echo "=== Relancer ==="
echo "  cd $APP_DIR && git pull && bash deploy/setup-vps.sh"
