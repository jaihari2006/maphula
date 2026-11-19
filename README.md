# 🦚 Maphula — Offline AI Assistant

[![Android](https://img.shields.io/badge/Platform-Android_8.0%2B_(API_26--36)-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-34%2F34_Passing-success)](#testing--verification)
[![Offline](https://img.shields.io/badge/Mode-100%25_Offline-brightgreen)](#project-structure)

**Maphula** is a next-generation sovereign offline Android AI assistant powered by the **"Hari"** intelligence engine. It runs advanced AI reasoning, neural pattern classification, semantic knowledge retrieval, multi-modal creative synthesis, and full-featured utility suites entirely on-device without cloud dependencies. External information is synchronized strictly when internet connectivity is explicitly available.

---

## 🚀 Quick Download & Installation

The compiled debug APK is ready to install:

👉 **[Download maphula.apk (GitHub Release v1.0.0)](https://github.com/jaihari2006/maphula/releases/download/v1.0.0/maphula.apk)** *(~8.1 MB)*
👉 **[Direct Repository File](maphula.apk)**

```bash
# Install via ADB:
adb install -r maphula.apk
```

---

## 🛠️ Project Structure

```
offline-ai/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/offlineai/app/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   ├── engine/
│   │   │   │   ├── ui/
│   │   │   │   └── widget/
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       ├── values/
│   │   │       ├── drawable/
│   │   │       └── xml/
│   │   └── test/
│   │       └── java/com/offlineai/app/
│   │           └── FeatureVerificationTest.kt
│   └── build.gradle
├── build.gradle
├── settings.gradle
├── gradle.properties
├── maphula.apk
└── README.md
```

---

## 🧪 Testing & Verification

The test suite thoroughly verifies neural convergence, NLP accuracy, deity greetings, calendar accuracy across 10,000 years, and UI interpolators.

```bash
gradle testDebugUnitTest
```

**Results:**
- `34/34 tests passed (0 failures, 0 errors, 0 skipped)`

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
