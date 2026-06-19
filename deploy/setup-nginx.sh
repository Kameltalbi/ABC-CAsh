#!/usr/bin/env bash
# Configure Nginx pour exposer l'API sur http://213.130.144.183/abc-cash/
set -euo pipefail

APP_DIR="${APP_DIR:-/var/www/abc-cash}"
VPS_IP="${VPS_IP:-213.130.144.183}"

if [[ $EUID -ne 0 ]]; then
  echo "Lancez en root: sudo bash deploy/setup-nginx.sh"
  exit 1
fi

echo "==> Snippet Nginx"
mkdir -p /etc/nginx/snippets
cat > /etc/nginx/snippets/abc-cash.conf <<'EOF'
location /abc-cash/ {
    proxy_pass http://127.0.0.1:8081/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 60s;
}
EOF

echo "==> Site dédié IP $VPS_IP"
sed "s/213.130.144.183/$VPS_IP/g" "$APP_DIR/deploy/nginx-abc-cash-ip.conf" \
  > /etc/nginx/sites-available/abc-cash-api.conf

ln -sf /etc/nginx/sites-available/abc-cash-api.conf /etc/nginx/sites-enabled/abc-cash-api.conf

echo "==> Test configuration"
nginx -t

echo "==> Reload Nginx"
systemctl reload nginx

sleep 1
echo ""
echo "==> Test local"
curl -sf "http://127.0.0.1/abc-cash/health" -H "Host: $VPS_IP" && echo "" || {
  echo "Échec — vérifiez que l'API tourne: curl http://127.0.0.1:8081/health"
  exit 1
}

echo ""
echo "OK — http://$VPS_IP/abc-cash/health"
