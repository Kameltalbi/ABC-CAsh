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
echo "=== Variables dans le conteneur API ==="
$DC exec abc-cash-api printenv DATABASE_URL 2>/dev/null || echo "DATABASE_URL: (conteneur arrêté)"
$DC exec abc-cash-api printenv DATABASE_USER 2>/dev/null || true
$DC exec abc-cash-api sh -c 'test -n "$DATABASE_PASSWORD" && echo DATABASE_PASSWORD=OK || echo DATABASE_PASSWORD=MANQUANT' 2>/dev/null || true

echo ""
echo "=== Logs API (30 dernières lignes) ==="
$DC logs abc-cash-api --tail 30 2>/dev/null || echo "Conteneur abc-cash-api absent"

echo ""
echo "=== Logs DB (10 dernières lignes) ==="
$DC logs abc-cash-db --tail 10 2>/dev/null || echo "Conteneur abc-cash-db absent"

echo ""
echo "=== Test mot de passe PostgreSQL (.env → DB) ==="
if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
  if $DC exec -T abc-cash-db pg_isready -U "${POSTGRES_USER:-abc_cash}" -d "${POSTGRES_DB:-abc_cash}" >/dev/null 2>&1; then
    if PGPASSWORD="$POSTGRES_PASSWORD" $DC exec -T -e PGPASSWORD="$POSTGRES_PASSWORD" abc-cash-db \
      psql -h localhost -U "${POSTGRES_USER:-abc_cash}" -d "${POSTGRES_DB:-abc_cash}" -c 'SELECT 1' >/dev/null 2>&1; then
      echo "Mot de passe OK"
    else
      echo "ERREUR: mot de passe .env refusé par Postgres (volume créé avec un autre mot de passe)"
      echo "  → bash deploy/reset-db-volume.sh"
    fi
  else
    echo "Postgres pas prêt"
  fi
else
  echo "deploy/.env absent"
fi

echo ""
echo "=== Relancer ==="
echo "  cd $APP_DIR && git pull && bash deploy/setup-vps.sh"
echo "  Mot de passe DB incohérent → bash deploy/reset-db-volume.sh"
