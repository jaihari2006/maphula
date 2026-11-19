# 🦚 Maphula — Offline AI Assistant

[![Android](https://img.shields.io/badge/Platform-Android_8.0%2B_(API_26--36)-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-34%2F34_Passing-success)](#testing--verification)
[![Offline](https://img.shields.io/badge/Mode-100%25_Offline-brightgreen)](#core-architecture)

**Maphula** is a next-generation sovereign offline Android AI assistant powered by the **"Hari"** intelligence engine. It runs advanced AI reasoning, neural pattern classification, semantic knowledge retrieval, multi-modal creative synthesis, and full-featured utility suites entirely on-device without cloud dependencies. External information is synchronized strictly when internet connectivity is explicitly available.

---

## 🌟 Key Highlights

### 🕉️ 1. Ten Hindu Deity AI Models ("Hari")
Choose from ten distinct on-device persona models, each fine-tuned with unique philosophical wisdom, domain specialization, voice characteristics, and system prompts:
- **Krishna (Hari)** — *Supreme Wisdom, Cosmic Counsel & Strategic Compassion*
- **Shiva (Transformer)** — *Deep Technical Analysis, Architectural Synthesis & Renunciation*
- **Brahma (Creator)** — *Creative Generative Synthesis, Ideation & Universal Law*
- **Shani (Discipline)** — *Rigorous Code Verification, Logic, Karma & Focus*
- **Ganesha (Remover of Obstacles)** — *Algorithmic Problem Solving, Beginning of Tasks & Prudence*
- **Muruga (Tactical)** — *Strategy, Decisiveness, Speed & Execution*
- **Hanuman (Guardian)** — *Safety, Health, Unyielding Devotion & Protection*
- **Lakshmi (Flourishing)** — *Prosperity, Wealth Management, Grace & Abundance*
- **Parvathi (Shakti)** — *Holistic Life Advice, Energy & Balance*
- **Narasimha (Defense)** — *Cybersecurity, Critical Defense & Integrity Shielding*

---

### 🎨 2. Adaptive Fluid UI & Buttery Physics
- **3 Finish Types**:
  - **Matte Finish**: Soft, glare-free, non-reflective minimalist surface.
  - **Solid Color**: Vibrant, high-contrast, bold visual design.
  - **Glassy**: Premium frosted glassmorphism with dynamic lighting and backdrop translucent blur.
- **3 Theme Modes**:
  - **Light Mode**: Crisp, high-readability daytime layout.
  - **Semi-Dark Mode**: Muted slate/slate-grey tone for low-strain transitions.
  - **Dark Mode**: True deep-black AMOLED layout for maximum power efficiency.
- **Buttery Smooth Physics**: Custom cubic-bezier spring easing interpolators for natural touch response and momentum gestures.
- **In-App Quick Settings**: Switch between finishes and themes seamlessly on the fly.

---

### 📅 3. Universal Calendar (1–9999 AD) & 8-Bit Retro Clock
- **Top-Left Universal Calendar Tab**: Navigates from **1 AD to 9999 AD**. Every day is mathematically synchronized with Hindu festival celebrations (*Janmashtami, Ganesh Chaturthi, Pongal/Makar Sankranti, Diwali, Maha Shivaratri, etc.*) mapped to deity lore, rituals, and auspicious timings.
- **Top-Right 8-Bit Digital Clock**: Retro pixelated digital clock displaying real-time days, hours, minutes, and seconds.
- **Interactive Analog Clock**: Tapping the 8-bit clock opens an animated analog clock dial with hour, minute, and sweeping second hands.

---

### 🎨 4. Creative Studio (On-Device Generation)
- **Image Generation**: Local procedural image synthesis across varied aesthetics (*Cyberpunk, Vedic Art, Minimalist, Watercolor*).
- **Video Motion Generation**: Keyframe procedural interpolation with camera pan, tilt, and zoom easing.
- **Voice Duplication & Synthesis**: 10 customized audio frequency profiles matching deity personas with pitch modulation and automatic markdown-to-speech sanitization.

---

### 🧠 5. Neural Lab & NLP Intelligence
- **Neural Network**: On-device Multi-Layer Perceptron (MLP) supporting backpropagation, momentum training, XOR convergence, and 2D pattern classification.
- **NLP Toolkit**: Native text sentiment analysis, extractive multi-sentence summarization, readability scoring (Flesch-Kincaid), and keyword/entity extraction.
- **Semantic Knowledge Engine**: Offline TF-IDF vector retrieval spanning survival protocols, science, mathematics, scriptures, and health.

---

### ⚔️ 6. Code Studio & Ravana Battle Arena
- **Code Studio**: Code execution, linting, syntax analysis, and algorithmic optimization.
- **Ravana Skill Battle Arena**: Challenge ideas, stress-test concepts against AI personas, and track survival scorecards.

---

### 📱 7. Home Screen App Widget
- **Maphula Widget**: Daily deity wisdom, festival celebrations, and 1-tap quick chat activation right from the Android launcher.

---

## 🚀 Quick Download & Installation

The compiled debug APK is ready to install:

👉 **[Download maphula.apk](maphula.apk)** *(~8.1 MB)*

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
