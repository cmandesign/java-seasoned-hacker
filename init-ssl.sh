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

CERT_DIR="/etc/letsencrypt/live/${VM_HOST}"

echo "==> Creating temporary self-signed certificate so Nginx can start..."
docker compose run --rm --entrypoint "" certbot sh -c "
  mkdir -p ${CERT_DIR} &&
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout ${CERT_DIR}/privkey.pem \
    -out ${CERT_DIR}/fullchain.pem \
    -subj '/CN=localhost'
"

echo "==> Starting Nginx for ACME challenge on port 80..."
docker compose up -d frontend

echo "==> Removing temporary self-signed certificate..."
docker compose run --rm --entrypoint "" certbot sh -c "
  rm -rf /etc/letsencrypt/live/${VM_HOST} &&
  rm -rf /etc/letsencrypt/archive/${VM_HOST} &&
  rm -rf /etc/letsencrypt/renewal/${VM_HOST}.conf
"

echo "==> Requesting certificate for ${VM_HOST}..."
docker compose run --rm --entrypoint "" certbot certbot certonly \
  --webroot -w /var/www/certbot \
  -d "${VM_HOST}" \
  $EMAIL_FLAG \
  $STAGING_FLAG \
  --agree-tos \
  --force-renewal

echo "==> Reloading Nginx to pick up the real certificate..."
docker compose exec frontend nginx -s reload

echo "==> Done. HTTPS is now active for ${VM_HOST}."
