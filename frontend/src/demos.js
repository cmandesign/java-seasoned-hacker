export const demos = [
  {
    id: 'a01',
    title: 'A01 — Broken Access Control (IDOR)',
    description: 'Access any user profile by changing the ID. Vulnerable exposes SSN, secure requires ownership.',
    actions: {
      vulnerable: [
        {
          label: 'Get admin profile (with SSN)',
          method: 'GET',
          path: '/api/v1/vulnerable/accounts/1',
        },
        {
          label: 'Get alice profile',
          method: 'GET',
          path: '/api/v1/vulnerable/accounts/2',
        },
        {
          label: 'Change alice email to hacked@evil.com',
          method: 'PUT',
          path: '/api/v1/vulnerable/accounts/2',
          body: { email: 'hacked@evil.com' },
        },
      ],
      secure: [
        {
          label: 'alice reads own profile (id=2)',
          method: 'GET',
          path: '/api/v1/secure/accounts/2',
          auth: { username: 'alice', password: 'Alice123!' },
        },
        {
          label: 'alice tries admin profile (id=1) → 403',
          method: 'GET',
          path: '/api/v1/secure/accounts/1',
          auth: { username: 'alice', password: 'Alice123!' },
        },
        {
          label: 'admin reads alice profile (id=2)',
          method: 'GET',
          path: '/api/v1/secure/accounts/2',
          auth: { username: 'admin', password: 'Admin123!' },
        },
      ],
    },
  },
  {
    id: 'a02',
    title: 'A02 — Security Misconfiguration',
    description: 'Stack traces, actuator secrets, H2 console. Run with --spring.profiles.active=vulnerable to see the difference.',
    actions: {
      vulnerable: [
        {
          label: 'Trigger error (stack trace leak)',
          method: 'GET',
          path: '/api/v1/vulnerable/debug/error',
        },
        {
          label: 'Check actuator/env',
          method: 'GET',
          path: '/actuator/env',
        },
      ],
      secure: [
        {
          label: 'Trigger error (generic message)',
          method: 'GET',
          path: '/api/v1/vulnerable/debug/error',
        },
        {
          label: 'Actuator/env (should be locked)',
          method: 'GET',
          path: '/actuator/env',
        },
      ],
    },
  },
  {
    id: 'a04',
    title: 'A04 — Cryptographic Failures (Magic Links)',
    description: 'Weak JWT secret crackable with hashcat vs strong 256-bit random key with expiry and single-use.',
    actions: {
      vulnerable: [
        {
          label: 'Generate crackable magic link',
          method: 'POST',
          path: '/api/v1/vulnerable/magic-link?email=alice@company.com',
          saveAs: 'vulnToken',
        },
        {
          label: 'Verify token (replayable)',
          method: 'GET',
          pathTemplate: '/api/v1/vulnerable/magic-link/verify?token={vulnToken}',
          useSaved: 'vulnToken',
        },
      ],
      secure: [
        {
          label: 'Generate secure magic link',
          method: 'POST',
          path: '/api/v1/secure/magic-link?email=alice@company.com',
          saveAs: 'secureToken',
        },
        {
          label: 'Verify token (single-use)',
          method: 'GET',
          pathTemplate: '/api/v1/secure/magic-link/verify?token={secureToken}',
          useSaved: 'secureToken',
        },
        {
          label: 'Replay same token → rejected',
          method: 'GET',
          pathTemplate: '/api/v1/secure/magic-link/verify?token={secureToken}',
          useSaved: 'secureToken',
        },
      ],
    },
  },
  {
    id: 'a05',
    title: 'A05 — Injection (SQL + XXE)',
    description: 'SQL injection via string concatenation and XXE via unsafe XML parsing.',
    actions: {
      vulnerable: [
        {
          label: "SQL injection: ' OR '1'='1",
          method: 'GET',
          path: "/api/v1/vulnerable/products?search=' OR '1'='1",
        },
        {
          label: 'XXE: read /etc/passwd',
          method: 'POST',
          path: '/api/v1/vulnerable/products/import',
          body: '<?xml version="1.0"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><products><product><name>&xxe;</name></product></products>',
          contentType: 'application/xml',
        },
      ],
      secure: [
        {
          label: "Same SQL payload → 0 results",
          method: 'GET',
          path: "/api/v1/secure/products?search=' OR '1'='1",
        },
        {
          label: 'Same XXE payload → blocked',
          method: 'POST',
          path: '/api/v1/secure/products/import',
          body: '<?xml version="1.0"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><products><product><name>&xxe;</name></product></products>',
          contentType: 'application/xml',
        },
      ],
    },
  },
  {
    id: 'a06',
    title: 'A06 — Insecure Design (OTP Brute Force)',
    description: '4-digit OTP with no rate limit vs 6-digit OTP with 5-attempt lockout.',
    actions: {
      vulnerable: [
        {
          label: 'Generate 4-digit OTP',
          method: 'POST',
          path: '/api/v1/vulnerable/otp/generate',
          saveAs: 'vulnOtpSession',
          saveField: 'sessionId',
        },
        {
          label: 'Guess OTP (try 0000)',
          method: 'POST',
          pathTemplate: '/api/v1/vulnerable/otp/verify?sessionId={vulnOtpSession}&otp=0000',
          useSaved: 'vulnOtpSession',
        },
      ],
      secure: [
        {
          label: 'Generate 6-digit OTP',
          method: 'POST',
          path: '/api/v1/secure/otp/generate',
          saveAs: 'secureOtpSession',
          saveField: 'sessionId',
        },
        {
          label: 'Guess OTP (try 000000)',
          method: 'POST',
          pathTemplate: '/api/v1/secure/otp/verify?sessionId={secureOtpSession}&otp=000000',
          useSaved: 'secureOtpSession',
        },
      ],
    },
  },
  {
    id: 'a07',
    title: 'A07 — Authentication Failures (JWT)',
    description: 'Weak HMAC secret (crackable) vs RSA-2048 with issuer/audience/expiry validation.',
    actions: {
      vulnerable: [
        {
          label: 'Login as alice (get weak JWT)',
          method: 'POST',
          path: '/api/v1/vulnerable/auth/login',
          body: { username: 'alice', role: 'USER' },
          saveAs: 'vulnJwt',
          saveField: 'token',
        },
        {
          label: 'Access /me with token',
          method: 'GET',
          pathTemplate: '/api/v1/vulnerable/auth/me?token={vulnJwt}',
          useSaved: 'vulnJwt',
        },
      ],
      secure: [
        {
          label: 'Login as alice (get RSA JWT)',
          method: 'POST',
          path: '/api/v1/secure/auth/login',
          body: { username: 'alice', role: 'USER' },
          saveAs: 'secureJwt',
          saveField: 'token',
        },
        {
          label: 'Access /me with token',
          method: 'GET',
          pathTemplate: '/api/v1/secure/auth/me?token={secureJwt}',
          useSaved: 'secureJwt',
        },
      ],
    },
  },
  {
    id: 'a08',
    title: 'A08 — Integrity Failures',
    description: 'Unsafe Java deserialization + unverified webhooks vs JSON DTOs + HMAC signature verification.',
    actions: {
      vulnerable: [
        {
          label: 'Deserialize arbitrary object',
          method: 'POST',
          path: '/api/v1/vulnerable/import',
          body: { data: 'rO0ABXQADUhlbGxvIFdvcmxkIQ==' },
        },
        {
          label: 'Send forged webhook (no sig)',
          method: 'POST',
          path: '/api/v1/vulnerable/webhook',
          body: { event: 'payment.completed', orderId: '12345' },
        },
      ],
      secure: [
        {
          label: 'Import via JSON DTO',
          method: 'POST',
          path: '/api/v1/secure/import',
          body: [{ name: 'Widget', category: 'Tools', price: 9.99 }],
        },
        {
          label: 'Send webhook without signature → 401',
          method: 'POST',
          path: '/api/v1/secure/webhook',
          body: { event: 'payment.completed', orderId: '12345' },
        },
      ],
    },
  },
  {
    id: 'a09',
    title: 'A09 — Logging Failures',
    description: 'Silent login attempts vs structured audit logging with brute-force alerting.',
    actions: {
      vulnerable: [
        {
          label: 'Failed login (no log)',
          method: 'POST',
          path: '/api/v1/vulnerable/login',
          body: { username: 'admin', password: 'wrong' },
        },
      ],
      secure: [
        {
          label: 'Failed login (audit logged)',
          method: 'POST',
          path: '/api/v1/secure/login',
          body: { username: 'admin', password: 'wrong' },
        },
        {
          label: 'Send 3 more → triggers alert',
          method: 'POST',
          path: '/api/v1/secure/login',
          body: { username: 'admin', password: 'wrong' },
        },
      ],
    },
  },
  {
    id: 'a10',
    title: 'A10 — Exceptional Conditions',
    description: 'Exception swallowing marks order PAID without charging vs proper error handling with correlation IDs.',
    actions: {
      vulnerable: [
        {
          label: 'Pay $999 (gateway times out → still says PAID)',
          method: 'POST',
          path: '/api/v1/vulnerable/payments',
          body: { userId: 1, productId: 1, amount: 999.99 },
        },
      ],
      secure: [
        {
          label: 'Pay $999 (gateway times out → FAILED + 502)',
          method: 'POST',
          path: '/api/v1/secure/payments',
          body: { userId: 1, productId: 1, amount: 999.99 },
        },
        {
          label: 'Pay $99 (succeeds normally)',
          method: 'POST',
          path: '/api/v1/secure/payments',
          body: { userId: 1, productId: 1, amount: 99.99 },
        },
      ],
    },
  },
]
