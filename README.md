# ⚡ MRX BatteryGuard

<div align="center">
  <img src="res/drawable/icon.jpg" width="120" alt="MRX BatteryGuard Icon"/>
  
  **Hacker Edition** — Real-time battery monitoring for Android
  
  [![Android](https://img.shields.io/badge/Android-6.0%2B-green)](https://www.android.com)
  [![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
  [![Built With](https://img.shields.io/badge/Built%20With-Java%20%7C%20Android%20SDK-orange)]()
  [![Release](https://img.shields.io/badge/Release-v2.0-brightgreen)](../../releases)
</div>

---

## 📱 About

MRX BatteryGuard is a lightweight, zero-bloat Android application that monitors your device battery in real-time. Built entirely with native Java and Android SDK command-line tools — no frameworks, no Gradle dependencies, no external libraries.

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔋 **Real-Time Monitoring** | Battery percentage, temperature, and charging status |
| 🔔 **Smart Alarm** | Configurable target with sound, vibration, and system notification |
| 🎯 **Quick Presets** | One-tap targets: 50% · 60% · 70% · 80% |
| 📊 **History Graph** | Visual bar chart of charge level changes over time |
| 🎨 **Hacker Theme** | Dark monospace aesthetic with green-on-black design |
| 🛑 **One-Tap Stop** | Instantly silence alarms from the home screen |
| 📱 **Wide Compatibility** | Supports Android 6.0 (Marshmallow) through Android 16+ |

---

## 📥 Download & Install

[![Download APK](https://img.shields.io/badge/Download-Latest%20APK-success?style=for-the-badge&logo=android)](../../releases/latest)

> Or go to the [Releases](../../releases) page to download the latest APK directly.

### Install on Android
1. Download `MRX_BatteryGuard.apk` from the releases page
2. Open the file on your Android device
3. Enable **"Install from unknown sources"** if prompted
4. Tap **Install**

---

## 🚀 Build from Source

```bash
git clone https://github.com/YOUR_USERNAME/MRX_BatteryGuard.git
cd MRX_BatteryGuard
./build.sh
# Output: build/MRX_BatteryGuard.apk
```

### Requirements
- Java 8 JDK
- Android SDK (platforms: android-23, build-tools: 29.0.3)
- Bash shell

---

## 🛠️ Tech Stack

| Tool | Purpose |
|------|---------|
| **Java 8** | Application logic |
| **Android SDK** | Platform APIs (BatteryManager, Notifications, Vibrator) |
| **AAPT** | Resource packaging & R.java generation |
| **Dalvik Exchange (dx)** | DEX bytecode conversion |
| **Jarsigner** | APK signing |
| **Zipalign** | APK optimization |

---

## 📂 Project Structure

```
MRX_BatteryGuard/
├── src/main/
│   ├── java/com/mrx/batteryguard/
│   │   └── MainActivity.java      # Core application
│   ├── res/
│   │   ├── drawable/icon.jpg       # App icon
│   │   └── values/strings.xml      # Resources
│   └── AndroidManifest.xml         # App configuration
├── build.sh                        # Build script
├── build.gradle                    # Gradle config (legacy)
├── settings.gradle                 # Gradle settings
└── build/                          # Output directory
    └── MRX_BatteryGuard.apk
```

---

## 👤 Author

**Sajad Chehrazi**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-blue?style=for-the-badge&logo=linkedin)](https://www.linkedin.com/in/sajad-chehrazi)

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
