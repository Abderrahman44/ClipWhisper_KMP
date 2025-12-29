# ClipWhisper — Clipboard Sync

## Table of Contents

* [Motivation](#motivation)
* [Demo](#demo)
* [Features](#features)
* [How It Works](#how-it-works)
* [Tech Stack](#tech-stack)
* [Requirements](#requirements)
* [Build & Run](#build--run)
* [Notes](#notes)

ClipWhisper is a **Kotlin Multiplatform (KMP)** application that seamlessly syncs **clipboard text** across devices on the same local network.

It currently supports **Android** and **Desktop (JVM)**, featuring a secure **pairing & approval flow**, **echo suppression** (to prevent sync loops), and a configurable **local clipboard history**.

> 🏆 This project was built for the **KotlinConf / Kotlin Multiplatform Contest**.

---

## Motivation

The idea was inspired by a real use case: a friend used Telegram’s **“Saved Messages”** to move copied text between his phone and PC and needed reliable **clipboard history with pinned items**. Existing tools like **Microsoft Clipboard** didn’t fully meet this need, especially when it came to keeping important text easily accessible.

This motivated me to build a **local clipboard sync app** that works seamlessly across devices and includes **built-in clipboard history management**, without relying on cloud services.

---

## Demo

### Android

[https://github.com/user-attachments/assets/128c7671-8502-4845-b4ac-21dfdf39e4a5](https://github.com/user-attachments/assets/128c7671-8502-4845-b4ac-21dfdf39e4a5)

### Desktop

[https://github.com/user-attachments/assets/7e612b28-965d-4d62-b975-dce413e43874](https://github.com/user-attachments/assets/7e612b28-965d-4d62-b975-dce413e43874)

> ℹ️ If GitHub doesn’t preview the video inline, download it from the repository and play it locally.

---

## Features

*  **Cross-platform** — Android + Desktop (JVM) via Kotlin Multiplatform
* **Automatic LAN discovery** — devices find each other using UDP broadcast
*  **Secure pairing flow** — explicit request/approval before any data sync
*  **Clipboard synchronization** — text clipboard shared across approved devices
* **Echo suppression** — prevents infinite clipboard feedback loops
* **Local clipboard history** — persisted with SQLDelight and configurable limits

---

## How It Works

1. **Discovery (UDP)**
   Devices periodically broadcast discovery packets on the local network to announce their presence.

2. **Pairing (TCP)**
   When a device is discovered, pairing must be explicitly approved. Only approved peers are allowed to exchange data.

3. **Clipboard Sync (TCP)**
   Local clipboard changes are detected and transmitted to paired devices.

4. **Echo Suppression**
   Remote clipboard updates are applied safely without re-triggering outbound sync events.

5. **Clipboard History (SQLDelight)**
   Clipboard entries are stored locally and trimmed based on user-defined limits (maximum text length and history size).

---

## Tech Stack

* **Kotlin Multiplatform**
* **Kotlin Coroutines & Flow**
* **Compose Multiplatform**
* **SQLDelight** — local clipboard history storage
* **kotlinx.serialization** — network protocol packets
* **UDP** — device discovery
* **TCP** — pairing and clipboard data transfer

---

## Requirements

* **JDK 17+** (JDK 21 supported)
* **Android Studio + Android SDK** (for Android builds)
* Devices connected to the **same local network** (Wi‑Fi or LAN)

---

## Build & Run

> 📦 Main module used in this repository: `:composeApp`
>
> If a task appears to be missing, list all available tasks with:
>
> ```bash
> ./gradlew :composeApp:tasks --all
> ```

### Android

Install debug build on a connected device or emulator:

```bash
./gradlew :composeApp:installDebug
```

Build debug APK only:

```bash
./gradlew :composeApp:assembleDebug
```

### Desktop (Run Locally)

```bash
./gradlew :composeApp:run
```

### Linux

**Fedora / RPM**

```bash
./gradlew :composeApp:packageRpm
```

**Ubuntu / Debian (DEB)**

```bash
./gradlew :composeApp:packageDeb
```

### Windows (MSI)

```bash
./gradlew :composeApp:packageMsi
```

### macOS (DMG)

```bash
./gradlew :composeApp:packageDmg
```

---

## Notes

* Clipboard syncing currently supports **text only**.
* All communication happens **locally** — no cloud or external servers involved.
* Designed as a reference-quality **Kotlin Multiplatform networking + Compose** project.
