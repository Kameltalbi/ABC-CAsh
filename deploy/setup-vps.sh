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

echo "==> Dossier: $APP_DIR"
cd "$APP_DIR/deploy"

if [[ ! -f .env ]]; then
  JWT=$(openssl rand -hex 32)
  DB_PASS=$(openssl rand -hex 16)
  cp .env.example .env
  sed -i "s/remplacer-par-une-longue-chaine-aleatoire/$JWT/" .env
  sed -i "s/remplacer-mot-de-passe-fort/$DB_PASS/g" .env
  echo "==> .env créé avec mots de passe aléatoires"
fi

echo "==> Build Docker (compile Kotlin + démarre Postgres + API)..."
echo "    (première fois : 3–5 min)"
$DC build --no-cache
$DC up -d

sleep 5
if curl -sf http://127.0.0.1:8081/health; then
  echo ""
  echo "==> API OK sur http://127.0.0.1:8081"
else
  echo "ERREUR — logs API:"
  $DC logs abc-cash-api --tail 30
  exit 1
fi

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
echo "==> Nginx snippet: /etc/nginx/snippets/abc-cash.conf"
echo "    Ajoutez dans server { }:  include snippets/abc-cash.conf;"
echo "    Puis: nginx -t && systemctl reload nginx"
echo ""
echo "    curl http://213.130.144.183/abc-cash/health"
