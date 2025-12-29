# ClipWhisper —  Clipboard Sync

ClipWhisper is a **Kotlin Multiplatform (KMP)** app that syncs **clipboard text** between devices on the same local network.  
It supports **Android** and **Desktop (JVM)** with **pairing + approval**, **echo suppression** (to prevent loops), and **local clipboard history**.

This project was created specifically for the **KotlinConf / Kotlin Multiplatform Contest**.

---

## Demo

- **Android demo:** `demo/android-demo.mp4`
- **Desktop demo:** `demo/desktop-demo.mp4`

> If GitHub doesn’t preview the video directly, download the file from the repo and play it locally.

---

## Features

- ✅ **Cross-platform**: Android + Desktop (JVM) using Kotlin Multiplatform
- ✅ **LAN discovery** (UDP broadcast): find nearby devices automatically
- ✅ **Pairing flow** (TCP): request/approve pairing before syncing
- ✅ **Clipboard sync** between approved devices
- ✅ **Echo suppression** to avoid feedback loops when applying remote clipboard updates
- ✅ **Local clipboard history** stored with SQLDelight and controlled by settings (size/count)

---

## How it works

1. **Discovery (UDP)**  
   Devices broadcast discovery packets and announce themselves on the LAN.

2. **Pairing (TCP)**  
   Pairing requires explicit approval. Only **approved** devices can exchange clipboard text.

3. **Clipboard sync**  
   When your clipboard changes, the text is sent to approved peers via TCP.

4. **Echo suppression**  
   Remote clipboard changes are applied safely without infinite back-and-forth updates.

5. **History (SQLDelight)**  
   Clipboard entries are saved locally and trimmed using user settings (max text size + history size).

---

## Tech stack

- **Kotlin Multiplatform**
- **Kotlin Coroutines + Flow**
- **SQLDelight** (history storage)
- **kotlinx.serialization** (protocol packets)
- **Compose Multiplatform** 
- **UDP** for discovery + **TCP** for pairing & clipboard transfer

---

## Requirements

- **JDK 17+** (JDK 21 works)
- **Android Studio + Android SDK** (for Android builds)
- A local network (same Wi-Fi/LAN) to test multi-device syncing

---

## Build & Run
> Module used in this repo: `:composeApp`  
> If a task is missing on your machine, list available tasks with:
> ```bash
> ./gradlew :composeApp:tasks --all
> ```

### Requirements
- JDK 17+ (JDK 21 is OK)
- Android Studio + Android SDK (for Android builds)

---

### Android

#### Run Debug on device/emulator
```bash
./gradlew :composeApp:installDebug
