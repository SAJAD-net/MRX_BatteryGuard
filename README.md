# ⚡ MRX BatteryGuard

<div align="center">
  <img src="res/drawable/icon.jpg" width="120" alt="MRX BatteryGuard Icon"/>
  
  **Hacker Edition** — Real-time battery monitoring for Android
  
  [![Android](https://img.shields.io/badge/Android-6.0%2B-green)](https://www.android.com)
  [![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
  [![Built With](https://img.shields.io/badge/Built%20With-Java%20%7C%20Android%20SDK-orange)]()
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

## 🚀 Quick Start

### Download APK
Get the latest release from the [Releases](../../releases) page or build from source:

### Build from Source
```bash
git clone https://github.com/YOUR_USERNAME/MRX_BatteryGuard.git
cd MRX_BatteryGuard
./build.sh
# Output: build/MRX_BatteryGuard.apk
```
## Install

Transfer the APK to your Android device and install. Make sure "Install from unknown sources" is enabled.

## 🛠️ Tech Stack
Tool	Purpose
Java 8	Application logic
Android SDK	Platform APIs (BatteryManager, Notifications, Vibrator)
AAPT	Resource packaging & R.java generation
Dalvik Exchange (dx)	DEX bytecode conversion
Jarsigner	APK signing
Zipalign	APK optimization
