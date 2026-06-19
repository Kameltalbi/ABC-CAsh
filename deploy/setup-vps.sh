#!/usr/bin/env bash
# À lancer SUR LE VPS après git clone (Java non requis — build dans Docker)
# Usage: bash deploy/setup-vps.sh

set -euo pipefail

if docker compose version &>/dev/null; then
  DC="docker compose"
else
  DC="docker-compose"
fi

APP_DIR="${APP_DIR:-/var/www/abc-cash}"

write_env() {
  local jwt db_pass
  jwt=$(openssl rand -hex 32)
  db_pass=$(openssl rand -hex 16)
  cat > .env <<EOF
PORT=8081
HOST=0.0.0.0
JWT_SECRET=${jwt}

DATABASE_URL=jdbc:postgresql://abc-cash-db:5432/abc_cash
DATABASE_USER=abc_cash
DATABASE_PASSWORD=${db_pass}

POSTGRES_DB=abc_cash
POSTGRES_USER=abc_cash
POSTGRES_PASSWORD=${db_pass}
EOF
  echo "==> deploy/.env créé (DATABASE_PASSWORD = POSTGRES_PASSWORD)"
}

env_ok() {
  [[ -f .env ]] || return 1
  grep -q '^DATABASE_URL=' .env || return 1
  grep -q '^DATABASE_PASSWORD=' .env || return 1
  grep -q '^JWT_SECRET=' .env || return 1
  grep -q '^POSTGRES_PASSWORD=' .env || return 1
  local db_api db_pg
  db_api=$(grep '^DATABASE_PASSWORD=' .env | cut -d= -f2-)
  db_pg=$(grep '^POSTGRES_PASSWORD=' .env | cut -d= -f2-)
  [[ "$db_api" == "$db_pg" ]] || return 1
  [[ "$db_api" != "remplacer-mot-de-passe-fort" ]] || return 1
  [[ "$db_api" != "change-me" ]] || return 1
  return 0
}

echo "==> Dossier: $APP_DIR"
cd "$APP_DIR/deploy"

if ! env_ok; then
  echo "==> .env absent ou incomplet — régénération..."
  write_env
fi

echo "==> Variables .env :"
grep -E '^(DATABASE_URL|DATABASE_USER|POSTGRES_DB|JWT_SECRET)=' .env | sed 's/JWT_SECRET=.*/JWT_SECRET=***/'

echo ""
echo "==> Build Docker (3–5 min la première fois)..."
$DC build
$DC up -d --force-recreate

echo "==> Attente démarrage..."
for i in $(seq 1 60); do
  if curl -sf http://127.0.0.1:8081/health >/dev/null 2>&1; then
    echo ""
    curl -sf http://127.0.0.1:8081/health
    echo ""
    echo "==> API OK sur http://127.0.0.1:8081"
    break
  fi
  if [[ $i -eq 60 ]]; then
    echo "ERREUR — logs API:"
    $DC logs abc-cash-api --tail 40
    echo ""
    echo "Si 'password authentication failed' → bash deploy/reset-db-volume.sh"
    echo ""
    echo "Variables dans le conteneur API:"
    $DC exec abc-cash-api printenv DATABASE_URL 2>/dev/null || true
    $DC exec abc-cash-api printenv DATABASE_USER 2>/dev/null || true
    exit 1
  fi
  sleep 2
done

cat > /etc/nginx/snippets/abc-cash.conf <<'EOF'
location /abc-cash/ {
    proxy_pass http://127.0.0.1:8081/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
EOF

echo ""
echo "==> Nginx: include snippets/abc-cash.conf; puis nginx -t && systemctl reload nginx"
