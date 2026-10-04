#!/usr/bin/env bash
# Remove everything the lab created. Pass --uninstall-k3s to also remove k3s.
set -euo pipefail

KUBECTL="${KUBECTL:-kubectl}"

echo "[*] Deleting the attacker pod and lab namespace ..."
$KUBECTL delete pod node-pwn -n vuln-lab --ignore-not-found --wait=false 2>/dev/null || true
$KUBECTL delete namespace vuln-lab --ignore-not-found 2>/dev/null || true

echo "[*] Deleting the cluster-admin binding ..."
$KUBECTL delete clusterrolebinding postgres-sa-cluster-admin --ignore-not-found 2>/dev/null || true

echo "[*] Removing proof markers on this host ..."
rm -f /tmp/PWNED_FROM_POSTGRES_POD_* 2>/dev/null || true

if [ "${1:-}" = "--uninstall-k3s" ]; then
  if [ -x /usr/local/bin/k3s-uninstall.sh ]; then
    echo "[*] Uninstalling k3s ..."
    sudo /usr/local/bin/k3s-uninstall.sh
  else
    echo "[!] k3s-uninstall.sh not found; skipping."
  fi
fi

echo "[*] Teardown complete."
