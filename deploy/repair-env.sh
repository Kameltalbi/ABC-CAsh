#!/usr/bin/env bash
# Répare deploy/.env et redémarre les conteneurs (sans rebuild)
set -euo pipefail

if docker compose version &>/dev/null; then DC="docker compose"; else DC="docker-compose"; fi

cd "${APP_DIR:-/var/www/abc-cash}/deploy"

JWT=$(openssl rand -hex 32)
DB_PASS=$(openssl rand -hex 16)

cat > .env <<EOF
PORT=8081
HOST=0.0.0.0
JWT_SECRET=${JWT}

DATABASE_URL=jdbc:postgresql://abc-cash-db:5432/abc_cash
DATABASE_USER=abc_cash
DATABASE_PASSWORD=${DB_PASS}

POSTGRES_DB=abc_cash
POSTGRES_USER=abc_cash
POSTGRES_PASSWORD=${DB_PASS}
EOF

echo "==> .env régénéré"
$DC down
$DC up -d --force-recreate

sleep 5
curl -sf http://127.0.0.1:8081/health && echo "" || $DC logs abc-cash-api --tail 20
