# Changelog

> **Copyright (c) 2025 Hasan Oso. All Rights Reserved.**  
> **PROPRIETARY SOFTWARE — See [LICENSE](LICENSE) for details.**

All notable changes to DiagnosticApp are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2025-02-28

### 🎉 Initial Release — Data Collection Phase

#### Added
- **82+ data point collection** across 9 categories
  - Identity & Authentication (12 points)
  - Security & Integrity (9 points)
  - CPU/GPU (10+ points)
  - Memory/Storage (8 points)
  - Display (8 points)
  - Camera/Sensors (7 points)
  - Battery (7 points)
  - Connectivity (7 points)
  - Performance & Thermal (13 points)

- **3 Adaptive Stress Tests**
  - CPU stress test (dynamic iterations based on device benchmark)
  - Storage stress test (sequential + random 4K IOPS)
  - Battery stress test (voltage sag + internal resistance)

- **Advanced Battery Analysis**
  - Multi-source capacity estimation (sysfs, charge_counter, voltage curve)
  - Hybrid voltage monitoring with synthetic fallback
  - Dual-cell battery detection (Samsung flagships)
  - Internal resistance calculation via Ohm's Law
  - Time-series data collection

- **Device Age Forensic Estimation**
  - 5-vector analysis (sysfs, system properties, OEM serial, vendor partition, Build.TIME)
  - Samsung serial number year/month decoding

- **Firebase Integration**
  - Complete Firestore upload of ALL collected data
  - WorkManager-based background upload with retry
  - Local JSON backup

- **Foreground Service**
  - Persistent notification during stress tests
  - Prevents system from killing the process
  - Real-time progress updates

- **Safety Features**
  - Preflight checker (battery ≥50%, storage ≥1GB, temp <50°C)
  - Charging detection during tests
  - Timeout protection (CPU: 10min, Storage: 5min, Battery: 10min)

- **Quality Scoring**
  - 10-dimension scan quality evaluation
  - EXCELLENT/GOOD/FAIR/POOR/INVALID classification
  - Voltage glitch detection
  - PMIC stall detection

- **Multi-Language Support**
  - Arabic, Turkish, English

- **Legal Protection**
  - Proprietary LICENSE
  - COPYRIGHT declaration
  - Security policy

#### Security
- Keystore passwords moved to `local.properties` (gitignored)
- No hardcoded credentials in source code
- Root detection and device integrity checks

---

## [Unreleased]

### Planned
- Screen touch test (dead zone detection)
- Biometric type detection
- Hardware fingerprint for deduplication
- Enhanced thermal monitoring (PowerManager API)
- Room database for local storage

---

© 2025 Hasan Oso. All Rights Reserved.
