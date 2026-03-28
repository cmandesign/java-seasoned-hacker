# OWASP Top 10 (2025) — Java Demo

A Spring Boot application demonstrating **9 of the OWASP Top 10 2025** vulnerabilities side-by-side: each risk has a **vulnerable** endpoint and a **secure** endpoint so you can see the difference in real time.

> A03 (Software Supply Chain Failures) is not included — separate example available.

## Tech Stack

- Java 21, Spring Boot 3.3.5, Maven
- Spring Security 6, Spring Data JPA, H2 (in-memory)
- JJWT (JWT handling), Jackson XML

## Quick Start

```bash
mvn clean package -DskipTests
java -jar target/owasp-top10-demo-0.0.1-SNAPSHOT.jar
```

App runs on `http://localhost:8080`. H2 seeds 4 users and 8 products automatically.

**Users (HTTP Basic for secure endpoints):**

| Username | Password   | Role    |
|----------|-----------|---------|
| admin    | Admin123! | ADMIN   |
| alice    | Alice123! | USER    |
| bob      | Bob12345! | USER    |
| manager  | Mgr12345! | MANAGER |

---

## Demos & Curl Examples

### A01 — Broken Access Control (IDOR)

```bash
# VULNERABLE: Anyone can read any user's profile (including SSN!)
curl http://localhost:8080/api/v1/vulnerable/accounts/1
curl http://localhost:8080/api/v1/vulnerable/accounts/2

# VULNERABLE: Anyone can change anyone's email
curl -X PUT http://localhost:8080/api/v1/vulnerable/accounts/2 \
  -H "Content-Type: application/json" \
  -d '{"email":"hacked@evil.com"}'

# SECURE: alice can only access her own profile (id=2)
curl -u alice:Alice123! http://localhost:8080/api/v1/secure/accounts/2   # 200 OK
curl -u alice:Alice123! http://localhost:8080/api/v1/secure/accounts/1   # 403 Forbidden

# SECURE: admin can access any profile
curl -u admin:Admin123! http://localhost:8080/api/v1/secure/accounts/2   # 200 OK
```

### A02 — Security Misconfiguration

```bash
# Default (secure): actuator locked down, no stack traces
curl http://localhost:8080/actuator/env                          # 401
curl http://localhost:8080/api/v1/vulnerable/debug/error         # Generic error message

# Run with vulnerable profile to see the difference:
# java -jar target/owasp-top10-demo-*.jar --spring.profiles.active=vulnerable
curl http://localhost:8080/actuator/env                          # 200 — secrets exposed!
curl http://localhost:8080/api/v1/vulnerable/debug/error         # Full stack trace + internal paths
curl http://localhost:8080/h2-console                            # Database console wide open
```

### A04 — Cryptographic Failures (Crackable Magic Links)

```bash
# VULNERABLE: Weak HMAC secret ("password123"), no expiry, replayable
curl -X POST "http://localhost:8080/api/v1/vulnerable/magic-link?email=alice@company.com"
# Copy the token, then verify:
curl "http://localhost:8080/api/v1/vulnerable/magic-link/verify?token=<TOKEN>"
# Replay the same token again — still works!
# Crack the JWT secret with: hashcat -m 16500 <jwt> /path/to/wordlist.txt

# SECURE: Strong 256-bit key, 5-min expiry, single-use
curl -X POST "http://localhost:8080/api/v1/secure/magic-link?email=alice@company.com"
# Verify once:
curl "http://localhost:8080/api/v1/secure/magic-link/verify?token=<TOKEN>"     # 200
# Verify again (replay):
curl "http://localhost:8080/api/v1/secure/magic-link/verify?token=<TOKEN>"     # "Token already used"
```

### A05 — Injection (SQL + XXE)

```bash
# VULNERABLE: SQL injection dumps all products
curl "http://localhost:8080/api/v1/vulnerable/products?search=' OR '1'='1"

# SECURE: Same payload treated as literal string — 0 results
curl "http://localhost:8080/api/v1/secure/products?search=' OR '1'='1"

# VULNERABLE: XXE reads local files
curl -X POST http://localhost:8080/api/v1/vulnerable/products/import \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><products><product><name>&xxe;</name></product></products>'

# SECURE: XXE blocked — DTDs disabled
curl -X POST http://localhost:8080/api/v1/secure/products/import \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><products><product><name>&xxe;</name></product></products>'
```

### A06 — Insecure Design (OTP Brute Force)

```bash
# VULNERABLE: 4-digit OTP, no rate limit
curl -X POST http://localhost:8080/api/v1/vulnerable/otp/generate
# Brute force it (try 0000-9999):
for i in $(seq -w 0 9999); do
  result=$(curl -s -X POST "http://localhost:8080/api/v1/vulnerable/otp/verify?sessionId=<SESSION_ID>&otp=$i")
  echo "$i: $result"
  echo "$result" | grep -q '"valid":true' && break
done

# SECURE: 6-digit OTP, max 5 attempts then lockout (429)
curl -X POST http://localhost:8080/api/v1/secure/otp/generate
# After 5 wrong guesses:
curl -X POST "http://localhost:8080/api/v1/secure/otp/verify?sessionId=<SESSION_ID>&otp=000000"
# ... 5 times -> 429 Too Many Requests, session invalidated
```

### A07 — Authentication Failures (JWT alg:none)

```bash
# VULNERABLE: Get a JWT with weak secret
curl -X POST http://localhost:8080/api/v1/vulnerable/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","role":"USER"}'

# Decode at jwt.io, change role to "ADMIN", re-sign with the leaked secret
# Or craft an alg:none token
curl "http://localhost:8080/api/v1/vulnerable/auth/me?token=<FORGED_TOKEN>"

# SECURE: RSA-256 signed, validates issuer/audience/expiry
curl -X POST http://localhost:8080/api/v1/secure/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","role":"USER"}'

# Tamper with the token and try:
curl "http://localhost:8080/api/v1/secure/auth/me?token=<TAMPERED_TOKEN>"  # Rejected
```

### A08 — Software/Data Integrity Failures

```bash
# VULNERABLE: Unsafe deserialization (accepts any serialized Java object)
# In a real attack, use ysoserial to craft an RCE payload
curl -X POST http://localhost:8080/api/v1/vulnerable/import \
  -H "Content-Type: application/json" \
  -d '{"data":"rO0ABXQADUhlbGxvIFdvcmxkIQ=="}'

# SECURE: JSON DTOs only — no ObjectInputStream
curl -X POST http://localhost:8080/api/v1/secure/import \
  -H "Content-Type: application/json" \
  -d '[{"name":"Widget","category":"Tools","price":9.99}]'

# VULNERABLE: Webhook with no signature check — accepts anything
curl -X POST http://localhost:8080/api/v1/vulnerable/webhook \
  -H "Content-Type: application/json" \
  -d '{"event":"payment.completed","orderId":"12345"}'

# SECURE: Requires valid HMAC-SHA256 signature
# First, get a valid signature:
SIG=$(curl -s -X POST http://localhost:8080/api/v1/secure/webhook/sign \
  -H "Content-Type: application/json" \
  -d '{"event":"payment.completed","orderId":"12345"}' | jq -r .signature)

# Send with valid signature:
curl -X POST http://localhost:8080/api/v1/secure/webhook \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Signature: $SIG" \
  -d '{"event":"payment.completed","orderId":"12345"}'

# Send with tampered body (signature won't match):
curl -X POST http://localhost:8080/api/v1/secure/webhook \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Signature: $SIG" \
  -d '{"event":"payment.completed","orderId":"99999"}'  # 401 — tampered!
```

### A09 — Security Logging & Alerting Failures

```bash
# VULNERABLE: Failed logins produce no logs
curl -X POST http://localhost:8080/api/v1/vulnerable/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}'
# Check terminal output — nothing logged about the failed attempt

# SECURE: Every attempt logged with structured context
curl -X POST http://localhost:8080/api/v1/secure/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}'

# After 3 failed attempts from the same IP, check logs:
# tail -f logs/security-audit.log
# You'll see: ALERT: 3 failed login attempts from IP 127.0.0.1
```

### A10 — Mishandling of Exceptional Conditions

```bash
# VULNERABLE: Payment > $500 triggers gateway timeout, but returns 200 and marks order PAID
curl -X POST http://localhost:8080/api/v1/vulnerable/payments \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":1,"amount":999.99}'
# Response: {"status":"PAID"} — but the charge never went through!

# Check the order — it says PAID even though no money was collected:
curl http://localhost:8080/api/v1/vulnerable/payments/1

# SECURE: Same request returns 502, order stays FAILED, includes correlation ID
curl -X POST http://localhost:8080/api/v1/secure/payments \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":1,"amount":999.99}'
# Response: {"status":"FAILED","correlationId":"...","retryable":true}

# Payments under $500 succeed normally:
curl -X POST http://localhost:8080/api/v1/secure/payments \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":1,"amount":99.99}'
# Response: {"status":"PAID","chargeId":"ch_..."}
```

---

## Project Structure

```
src/main/java/com/owaspdemo/
├── config/                          SecurityConfig, DataInitializer, UserDetailsService
├── common/model/                    AppUser, Product, Order, Role
├── common/repository/               JPA repositories
├── a01_broken_access_control/       IDOR demo
├── a02_security_misconfiguration/   Actuator + error exposure demo
├── a04_cryptographic_failures/      Crackable magic link demo
├── a05_injection/                   SQL injection + XXE demo
├── a06_insecure_design/             OTP brute force demo
├── a07_authentication_failures/     JWT alg:none demo
├── a08_integrity_failures/          Deserialization + webhook demo
├── a09_logging_failures/            Audit logging demo
└── a10_exceptional_conditions/      Exception swallowing demo
```

## OWASP Top 10 (2025) Reference

| # | Category | Demo |
|---|----------|------|
| A01 | Broken Access Control | IDOR on user profiles |
| A02 | Security Misconfiguration | Exposed actuator + stack traces |
| A03 | Software Supply Chain Failures | *(separate repo)* |
| A04 | Cryptographic Failures | Crackable magic link JWT |
| A05 | Injection | SQL injection + XXE |
| A06 | Insecure Design | Brute-forceable OTP |
| A07 | Authentication Failures | JWT alg:none + weak secret |
| A08 | Software/Data Integrity Failures | Unsafe deserialization + unverified webhooks |
| A09 | Security Logging & Alerting Failures | Silent vs. structured audit logging |
| A10 | Mishandling of Exceptional Conditions | Exception swallowing in payment flow |
