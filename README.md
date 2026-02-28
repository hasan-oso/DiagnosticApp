<div align="center">

# 📱 DiagnosticApp

### AI-Powered Device Health Analysis

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-green)
![License](https://img.shields.io/badge/license-Proprietary-red)
![Status](https://img.shields.io/badge/status-Active%20Development-orange)
![Kotlin](https://img.shields.io/badge/kotlin-1.9%2B-purple)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-teal)

</div>

---

> ## 🔒 PROPRIETARY SOFTWARE — ALL RIGHTS RESERVED
>
> **Copyright (c) 2025 Hasan Oso. Unauthorized use is strictly prohibited.**
>
> This software is the exclusive intellectual property of **Hasan Oso**
> (Student ID: **230212925**, OSTİM Technical University — AI Engineering Department).
>
> ⚠️ **DO NOT** copy, distribute, modify, or use this software without explicit written permission.
>
> See [LICENSE](LICENSE) | [COPYRIGHT](COPYRIGHT) | [NOTICE](NOTICE) | [SECURITY](SECURITY.md)

---

> **Contact for licensing inquiries:**
> - 📧 Email: [your-email]
> - 🔗 LinkedIn: [your-linkedin]
> - 🏫 OSTİM Technical University — AI Engineering Department

---

A comprehensive Android diagnostic tool that collects 82+ device data points, performs intensive stress tests, and enables expert labeling of device condition with cloud storage via Firebase.

## 🌟 Features

- **82+ Data Points Collection** across 9 categories (Identity, Security, CPU/GPU, Memory, Display, Camera, Battery, Connectivity, Performance)
- **3 Adaptive Stress Tests**: CPU (dynamic), Storage (dynamic), Battery (4 min)
- **Advanced Battery Analysis**: Multi-source capacity estimation, hybrid voltage monitoring, internal resistance
- **Foreground Service**: Prevents system kill during stress tests
- **Quality Scoring**: 10-dimension scan quality evaluation
- **Multi-language Support**: Arabic (RTL), Turkish, English
- **Safety Checks**: Battery level, storage space, CPU temperature before tests
- **Expert Labeling**: Comprehensive device condition assessment form
- **Cloud Integration**: Firebase Firestore for data storage

## 📋 Prerequisites

- **Development**: Android Studio Hedgehog 2023.1.1+
- **Testing**: Physical Android device (SDK 26+/Android 8.0+)
- **Build**: JDK 17
- **Cloud**: Firebase account

⚠️ **Important**: This app must be tested on a real device, not an emulator.

## 🚀 Quick Start

### 1. Clone the Project (Authorized Users Only)

```bash
cd doctor/DiagnosticApp
```

### 2. Configure Firebase

1. Set up a Firebase project
2. Download `google-services.json` and place in `app/`
3. Enable Firestore Database

### 3. Build the App

```bash
# Open in Android Studio or use command line:
./gradlew assembleDebug
```

### 4. Install on Device

Connect your Android device via USB and run:

```bash
./gradlew installDebug
```

Or use Android Studio's Run button.

## 🔐 Permissions

The app requires these permissions:
- **Camera**: For camera hardware detection
- **Storage**: For stress test file operations
- **Phone State**: For device identity data
- **Internet**: For Firebase uploads
- **Foreground Service**: For stress test persistence
- **Notifications**: For stress test progress

All permissions are requested at runtime with clear explanations.

## 🧪 Safety Features

Before starting stress tests, the app checks:
- ✅ Battery level ≥ 50%
- ✅ Free storage ≥ 1GB
- ✅ CPU temperature < 45°C

This protects your device from damage during intensive tests.

## 🏗️ Project Structure

```
DiagnosticApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/diagnostics/app/
│   │   │   ├── data/
│   │   │   │   ├── models/          # Data classes
│   │   │   │   ├── collectors/      # 18 data collectors
│   │   │   │   ├── repository/      # Firebase integration
│   │   │   │   └── quality/         # Scan quality scoring
│   │   │   ├── services/            # Foreground services
│   │   │   ├── ui/
│   │   │   │   ├── screens/         # Compose screens
│   │   │   │   └── theme/           # Material 3 theme
│   │   │   ├── viewmodel/           # State management
│   │   │   ├── domain/models/       # Domain models
│   │   │   ├── utils/               # Utilities
│   │   │   └── MainActivity.kt
│   │   └── res/
│   │       ├── values/              # English strings
│   │       ├── values-ar/           # Arabic strings
│   │       └── values-tr/           # Turkish strings
│   └── build.gradle.kts
├── docs/legal/                      # Legal documents
├── LICENSE                          # Proprietary license
├── COPYRIGHT                        # IP declaration
├── NOTICE                           # Usage restrictions
├── SECURITY.md                      # Security policy
├── CHANGELOG.md                     # Version history
└── build.gradle.kts
```

## 📊 Data Collection Categories

1. **Identity** (12): Brand, model, build info, Android version
2. **Security** (9): Root detection, developer options, ROM type
3. **CPU/GPU** (10+): Architecture, cores, GPU info
4. **Memory/Storage** (8): RAM, internal/external storage
5. **Display** (8): Resolution, refresh rate, HDR
6. **Camera/Sensors** (7): Camera specs, sensor count
7. **Battery** (7): Capacity, health, temperature
8. **Connectivity** (7): WiFi, Bluetooth, NFC, SIM
9. **Performance/Thermal** (13): Uptime, processes, temperatures

## 🔥 Stress Tests

### CPU Test (Adaptive Duration)
- Runs intensive math operations on all CPU cores
- Monitors temperature and frequency every 5 seconds
- Detects thermal throttling
- Dynamic iterations based on device benchmark score
- 10-minute timeout protection

### Storage Test (Adaptive)
- Writes multiple large files
- Measures sequential read/write speeds
- Tests random 4K IOPS
- Detects storage type (UFS/eMMC)
- 5-minute timeout protection

### Battery Test (4 minutes)
- Full load simulation
- Voltage sag measurement
- Internal resistance calculation via Ohm's Law
- Time-series data collection
- Hybrid voltage monitoring
- 10-minute timeout protection

## 🌍 Supported Languages

- **العربية** (Arabic) - Default, with full RTL support
- **Türkçe** (Turkish)
- **English**

Switch languages anytime from the home screen.

## 🛠️ Build APK

### Debug Build
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## 🐛 Known Limitations

- **Root Detection**: May have false positives on some custom ROMs
- **Temperature Reading**: Not available on all devices
- **Storage Type Detection**: Heuristic-based, may be inaccurate
- **Real Device Only**: Cannot test on emulator

## 🔒 Security & Privacy

- All data stored locally until explicitly uploaded
- Foreground service for stress test reliability
- Firebase credentials secured via `google-services.json`
- Keystore passwords in `local.properties` (gitignored)
- No analytics or tracking
- See [SECURITY.md](SECURITY.md) for security policy

## 📄 License

This is **proprietary software**. All rights reserved.
See [LICENSE](LICENSE) for the full license text.

## 🤝 Contributing

This project is **not open for contributions** at this time.
See [CONTRIBUTING.md](CONTRIBUTING.md) for future guidelines.

## 📧 Contact

| | |
|---|---|
| **Owner** | Hasan Oso |
| **Student ID** | 230212925 |
| **Institution** | OSTİM Technical University |
| **Department** | AI Engineering |
| **Email** | [your-email] |
| **LinkedIn** | [your-linkedin] |

---

<div align="center">

**© 2025 Hasan Oso. All Rights Reserved.**

*Built with ❤️ using Jetpack Compose and Kotlin*

**🔒 PROPRIETARY SOFTWARE — Unauthorized use is strictly prohibited**

</div>

