#!/usr/bin/env bash
# Build the vulnerable app image from this repo and import it into k3s's
# containerd so the Deployment (imagePullPolicy: Never) can run it offline.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
IMAGE="vuln-lab/owasp-app:latest"
TAR="/tmp/owasp-app.tar"

cd "$REPO_ROOT"

echo "[*] Building ${IMAGE} from ${REPO_ROOT}/Dockerfile ..."
if command -v docker >/dev/null 2>&1; then
  docker build -t "${IMAGE}" .
  echo "[*] Exporting image to ${TAR} ..."
  docker save "${IMAGE}" -o "${TAR}"
elif command -v nerdctl >/dev/null 2>&1; then
  nerdctl build -t "${IMAGE}" .
  nerdctl save "${IMAGE}" -o "${TAR}"
else
  echo "ERROR: need either 'docker' or 'nerdctl' to build the image." >&2
  exit 1
fi

echo "[*] Importing image into k3s containerd ..."
sudo k3s ctr images import "${TAR}"
rm -f "${TAR}"

echo "[*] Done. Image available in-cluster as ${IMAGE}"
sudo k3s ctr images ls | grep owasp-app || true
