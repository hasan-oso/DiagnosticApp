# Contributing to DiagnosticApp

> **Copyright (c) 2025 Hasan Oso. All Rights Reserved.**  
> **PROPRIETARY SOFTWARE — See [LICENSE](LICENSE) for details.**

---

## 🔒 Current Status: Private Project

DiagnosticApp is currently a **private, proprietary project** and is **not open
for external contributions**. This document exists to establish guidelines for
potential future collaboration.

---

## 🤝 Future Contribution Opportunities

This project may be opened for contributions in the future under specific terms.
If you are interested in contributing, please contact the Owner first:

- **Email**: [your-email]
- **LinkedIn**: [your-linkedin]

### Potential Areas for Future Collaboration

- Additional device-specific data collectors
- Expanded OEM serial number decoding patterns
- Battery chemistry-specific degradation models
- Localization for additional languages
- Machine learning model improvements

---

## 📝 Development Guidelines (For Authorized Contributors)

### Commit Message Convention

Follow the [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**

| Type | Description |
|------|-------------|
| `feat` | New feature or data collector |
| `fix` | Bug fix |
| `docs` | Documentation changes |
| `refactor` | Code refactoring |
| `test` | Adding or updating tests |
| `perf` | Performance improvements |
| `data` | Data model or collection changes |
| `security` | Security-related changes |

**Examples:**
```
feat(collector): add biometric hardware detection
fix(battery): handle Xiaomi current_now obfuscation
docs: update data collection methodology
data(model): add screen panel type field
```

### Code Style Rules

1. **Language**: Kotlin (no Java)
2. **Architecture**: MVVM with Repository pattern
3. **Compose**: Use Jetpack Compose for all UI
4. **Naming**: Follow Kotlin conventions (`camelCase` for functions, `PascalCase` for classes)
5. **Documentation**: KDoc for all public functions
6. **Error Handling**: Always use try-catch with proper logging
7. **Multi-Fallback**: Always provide fallback for device-specific APIs
8. **Null Safety**: Use Kotlin null safety properly, avoid `!!`

### Data Collection Rules

1. **Never crash** on data collection failure — always return a safe default
2. **Log everything** using `android.util.Log` with the collector name as tag
3. **Document manufacturer quirks** in code comments
4. **Use sysfs fallbacks** for restricted APIs
5. **Validate ranges** before storing values (e.g., temperature 10-80°C)

### Branch Naming

```
feature/short-description
fix/issue-description
refactor/component-name
docs/topic
```

---

## 📋 Pull Request Requirements

All contributions (when enabled) must:

1. Be authorized by the Owner in advance
2. Sign a Contributor License Agreement (CLA)
3. Follow the code style guidelines above
4. Include appropriate documentation
5. Not break existing data collection functionality
6. Maintain backward compatibility with Firebase schema
7. Include copyright headers in all new files

---

## ⚖️ Legal

By submitting a contribution, you agree that:

1. Your contribution becomes the property of Hasan Oso
2. You grant a perpetual, irrevocable license to use your contribution
3. You have the legal right to make the contribution
4. Your contribution does not infringe on any third-party rights

---

© 2025 Hasan Oso. All Rights Reserved.
