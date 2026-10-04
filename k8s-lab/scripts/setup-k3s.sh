#!/usr/bin/env bash
# Install a single-node k3s cluster on this server.
# k3s runs the control plane as ordinary host processes, so escaping a pod onto
# the node === root on this server -- which is exactly what the lab demonstrates.
set -euo pipefail

if command -v k3s >/dev/null 2>&1; then
  echo "[*] k3s already installed: $(k3s --version | head -n1)"
else
  echo "[*] Installing k3s (single node)..."
  curl -sfL https://get.k3s.io | sh -
fi

echo "[*] Waiting for the node to become Ready..."
for _ in $(seq 1 60); do
  if sudo k3s kubectl get node 2>/dev/null | grep -q ' Ready '; then
    break
  fi
  sleep 2
done

sudo k3s kubectl get node

# Make kubectl usable without sudo for the rest of the lab.
echo
echo "[*] k3s is up. To use the regular 'kubectl' binary against it, either run:"
echo "      export KUBECONFIG=/etc/rancher/k3s/k3s.yaml   (may need sudo to read)"
echo "    or just use:  sudo k3s kubectl ..."
echo
echo "[*] The lab scripts call 'kubectl'; if you only have 'k3s kubectl', run:"
echo "      alias kubectl='sudo k3s kubectl'"
