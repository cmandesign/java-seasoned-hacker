#!/bin/bash
set -euo pipefail

if [ -z "${VM_HOST:-}" ]; then
  echo "Error: VM_HOST environment variable must be set to your domain name."
  exit 1
fi

EMAIL="${CERT_EMAIL:-}"
EMAIL_FLAG="--register-unsafely-without-email"
if [ -n "$EMAIL" ]; then
  EMAIL_FLAG="--email $EMAIL --no-eff-email"
fi

STAGING_FLAG=""
if [ "${CERT_STAGING:-0}" = "1" ]; then
  STAGING_FLAG="--staging"
fi

echo "==> Starting Nginx for ACME challenge on port 80..."
docker compose up -d frontend

echo "==> Requesting certificate for ${VM_HOST}..."
docker compose run --rm certbot certbot certonly \
  --webroot -w /var/www/certbot \
  -d "${VM_HOST}" \
  $EMAIL_FLAG \
  $STAGING_FLAG \
  --agree-tos \
  --force-renewal

echo "==> Reloading Nginx to pick up the new certificate..."
docker compose exec frontend nginx -s reload

echo "==> Done. HTTPS is now active for ${VM_HOST}."
