#!/usr/bin/env bash
# Commandes à lancer SUR LE VPS après rsync depuis le Mac
# Usage: bash setup-vps.sh

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
  echo "==> .env créé"
fi

if [[ ! -f ../server/build/libs/server-1.0.0.jar ]]; then
  echo "ERREUR: JAR manquant. Depuis votre Mac, lancez:"
  echo "  cd \"/Users/kameltalbi/MyFiveApps/ABC CASH\""
  echo "  ./deploy/deploy-from-mac.sh"
  exit 1
fi

$DC build --no-cache
$DC up -d

sleep 4
curl -sf http://127.0.0.1:8081/health && echo "" || echo "ERREUR: vérifiez: $DC logs abc-cash-api"

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
echo "==> OK. Ajoutez dans votre site Nginx (server { }):"
echo "    include snippets/abc-cash.conf;"
echo "    nginx -t && systemctl reload nginx"
echo ""
echo "    curl http://127.0.0.1:8081/health"
