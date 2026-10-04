#!/usr/bin/env bash
# Deploy the lab into k3s and wait for everything to be ready.
set -euo pipefail

MANIFESTS="$(cd "$(dirname "${BASH_SOURCE[0]}")/../manifests" && pwd)"
KUBECTL="${KUBECTL:-kubectl}"

echo "[*] Applying manifests from ${MANIFESTS} ..."
$KUBECTL apply -f "${MANIFESTS}/00-namespace.yaml"
$KUBECTL apply -f "${MANIFESTS}/10-postgres-rbac.yaml"
$KUBECTL apply -f "${MANIFESTS}/20-postgres.yaml"
$KUBECTL apply -f "${MANIFESTS}/30-redis.yaml"
$KUBECTL apply -f "${MANIFESTS}/40-app.yaml"

echo "[*] Waiting for PostgreSQL ..."
$KUBECTL -n vuln-lab rollout status deploy/postgres --timeout=180s
echo "[*] Waiting for Redis ..."
$KUBECTL -n vuln-lab rollout status deploy/redis --timeout=120s
echo "[*] Waiting for the app (first boot compiles nothing but Spring takes a moment) ..."
$KUBECTL -n vuln-lab rollout status deploy/owasp-app --timeout=240s

echo
echo "[*] Lab is up. The vulnerable app is reachable at:"
echo "      http://<server-ip>:30080/swagger-ui.html"
echo "      SQLi->RCE sink: http://<server-ip>:30080/api/v1/lab/products?search=..."
echo
echo "[*] Run the end-to-end attack with:  k8s-lab/scripts/attack-demo.sh"
