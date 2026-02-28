# 🔒 Security Policy

> **DiagnosticApp** — Proprietary Software  
> Copyright (c) 2025 Hasan Oso. All Rights Reserved.

---

## ⚠️ Legal Notice

This is **proprietary software**. Unauthorized security testing, penetration testing,
vulnerability scanning, or any form of unauthorized access to this software or its
associated systems is **strictly prohibited** and may constitute a criminal offense
under applicable laws.

Only authorized security testing conducted with **explicit written consent** from the
Owner is permitted.

---

## 📋 Reporting a Vulnerability

If you discover a security vulnerability in DiagnosticApp, please report it
responsibly. **Do NOT** open a public GitHub issue for security vulnerabilities.

### How to Report

1. **Email**: Send a detailed report to [your-email]
2. **Subject Line**: `[SECURITY] DiagnosticApp - Vulnerability Report`
3. **Include**:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact assessment
   - Your contact information
   - Any suggested fixes (optional)

### What NOT to Do

- ❌ Do not publicly disclose the vulnerability before it is fixed
- ❌ Do not exploit the vulnerability beyond what is necessary to demonstrate it
- ❌ Do not access, modify, or delete data belonging to other users
- ❌ Do not perform denial-of-service attacks
- ❌ Do not conduct automated scanning without authorization

---

## 🛡️ Security Measures

### Data Protection

| Measure | Description |
|---------|-------------|
| **Transport Encryption** | All data transmitted to Firebase uses TLS 1.2+ |
| **Local Storage** | Scan data stored in app-private directory |
| **Keystore Security** | Signing credentials stored in `local.properties` (gitignored) |
| **No Hardcoded Secrets** | API keys and passwords excluded from source control |

### Application Security

| Measure | Description |
|---------|-------------|
| **Root Detection** | SecurityCollector identifies rooted devices |
| **Integrity Checks** | Build fingerprint and boot state verification |
| **Permission Model** | Runtime permissions with graceful degradation |
| **Data Validation** | ScanQualityScorer validates all collected data |

### Repository Security

| Measure | Description |
|---------|-------------|
| **Sensitive Files** | `google-services.json`, `*.jks`, `local.properties` gitignored |
| **Branch Protection** | Main branch requires review (when enabled) |
| **Dependency Audit** | Regular review of third-party dependencies |

---

## ⏱️ Response Timeline

| Phase | Timeframe |
|-------|-----------|
| **Acknowledgment** | Within 48 hours of report |
| **Initial Assessment** | Within 5 business days |
| **Fix Development** | Within 14 business days (severity dependent) |
| **Patch Release** | Within 30 days of confirmed vulnerability |
| **Public Disclosure** | Only after patch is released and deployed |

---

## 🏆 Recognition

Security researchers who responsibly report valid vulnerabilities will be:

- Acknowledged in the project's security hall of fame (if desired)
- Credited in release notes accompanying the fix
- Considered for authorized security research collaboration

---

## 📝 Scope

### In Scope

- DiagnosticApp Android application source code
- Data collection and storage mechanisms
- Firebase/Firestore integration
- Stress test engine
- Local data storage

### Out of Scope

- Third-party services (Firebase, Google Play Services)
- Android OS vulnerabilities
- Device hardware
- Network infrastructure

---

## ⚖️ Legal Framework

Security research conducted without authorization may be prosecuted under:

- **Turkish Penal Code** (Türk Ceza Kanunu) Articles 243-245
  (Unauthorized access to information systems)
- **Turkish IP Law No. 5846** (Copyright infringement)
- **Computer Fraud and Abuse Act** (if applicable jurisdiction)
- **EU Directive 2013/40/EU** on attacks against information systems

**Authorized security research** requires a signed agreement. Contact the
Owner for details.

---

## 📞 Contact

| | |
|---|---|
| **Owner** | Hasan Oso |
| **Student ID** | 230212925 |
| **Institution** | OSTİM Technical University |
| **Email** | [your-email] |
| **LinkedIn** | [your-linkedin] |

---

© 2025 Hasan Oso. All Rights Reserved.
