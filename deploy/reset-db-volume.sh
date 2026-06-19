#!/usr/bin/env bash
# Réinitialise le volume Postgres (efface toutes les données API).
# Nécessaire si deploy/.env a été régénéré : le volume garde l'ancien POSTGRES_PASSWORD.
set -euo pipefail

if docker compose version &>/dev/null; then DC="docker compose"; else DC="docker-compose"; fi

cd "${APP_DIR:-/var/www/abc-cash}/deploy"

echo "ATTENTION: suppression du volume PostgreSQL (données API perdues)."
echo "Appuyez Entrée pour continuer, Ctrl+C pour annuler."
read -r

$DC down -v
$DC up -d --build

echo "Attente démarrage (jusqu'à 90 s)..."
for i in $(seq 1 45); do
  if curl -sf http://127.0.0.1:8081/health >/dev/null 2>&1; then
    curl -sf http://127.0.0.1:8081/health
    echo ""
    echo "API OK"
    exit 0
  fi
  sleep 2
done

echo "Échec — logs:"
$DC logs abc-cash-api --tail 30
exit 1
