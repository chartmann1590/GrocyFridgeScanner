<div align="center">

# Grocy Fridge Scanner

**AI-powered fridge & cupboard inventory scanner for Grocy**

Scan your fridge or cupboard with your phone camera. On-device AI detects food items and syncs inventory changes directly to your self-hosted Grocy instance.

[![Android](https://img.shields.io/badge/Android-31%2B-green?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-ff6f00?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Gemma 4 E2B](https://img.shields.io/badge/AI-Gemma%204%20E2B-4285f4?logo=google)](https://ai.google.dev/gemma)

</div>

---

## Features

- **Camera Scanning** — Point your phone at a fridge shelf or cupboard and take a photo
- **On-Device AI** — Gemma 4 E2B runs entirely on your phone. No cloud, no data leaves your device
- **Smart Detection** — Identifies food items, counts retail units (bags, boxes, cans, jars, bottles), and proposes inventory changes
- **Review Before Sync** — Every proposed change is shown for review. Edit names, adjust counts, or exclude items before syncing
- **Inventory Sync** — Pushes changes directly to your Grocy instance via the REST API
- **Scan History** — Full history of all scans with timestamps, thumbnails, and item-level details
- **Auto Product Creation** — If a detected food doesn't exist in Grocy, it's created automatically

## Screenshots

| Scanner | Review | History | Settings |
|---------|--------|---------|----------|
| Camera preview with location selector | Detected items with delta indicators | Expandable cards with thumbnails | Connection status & AI model management |

## Requirements

- **Android 12+** (API 31)
- **Grocy** — A running [Grocy](https://grocy.info) instance with API access
- **~1.5 GB storage** — For the on-device AI model (downloaded during setup)

## Setup

### 1. Clone & Build

```bash
git clone https://github.com/chartmann1590/GrocyFridgeScanner.git
cd GrocyFridgeScanner
```

Create a `local.properties` file with your Grocy server details (optional — you can also enter these in the app):

```properties
grocy.url=https://your-grocy-instance.com
grocy.apiKey=your-api-key-here
```

Build and install on a connected device:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. First Launch

1. The app opens to an onboarding screen
2. Enter your Grocy server URL and API key (or it uses `local.properties` defaults if set)
3. The AI model downloads to your device (~1.5 GB)
4. Start scanning

## How It Works

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐     ┌──────────────┐
│  Take Photo  │────▶│  Gemma 4 E2B     │────▶│  Review & Edit  │────▶│  Sync Grocy  │
│  (CameraX)   │     │  (On-Device AI)  │     │  (Proposed      │     │  (REST API)  │
│              │     │  Detects items,  │     │   changes with  │     │              │
│              │     │  counts, and     │     │   +/− deltas)   │     │              │
│              │     │  container types │     │                 │     │              │
└─────────────┘     └──────────────────┘     └─────────────────┘     └──────────────┘
```

1. **Capture** — CameraX takes a photo of your fridge or cupboard
2. **Analyze** — Gemma 4 E2B (via LiteRT) runs inference on-device, returning detected food items with counts
3. **Match** — Detected items are matched against existing Grocy products by name. Unmatched items are flagged as new products
4. **Review** — Proposed inventory changes are shown with current → photo count deltas. You can edit, toggle, or cancel before committing
5. **Sync** — Selected changes are pushed to Grocy via the inventory API. New products are created automatically

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Camera | CameraX (Preview + ImageCapture) |
| AI Inference | LiteRT LM (Gemma 4 E2B) |
| Networking | Retrofit + OkHttp + Kotlinx Serialization |
| Storage | DataStore Preferences, Encrypted SharedPreferences |
| Architecture | MVVM (ViewModel + StateFlow) |

## Configuration

All settings are accessible from the Settings tab:

- **Grocy Server URL** — Your self-hosted Grocy base URL
- **API Key** — Generated from your Grocy instance (Settings → API Keys)
- **AI Model** — View download status, model size, or re-download the model

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

<div align="center">

Built with Kotlin, Jetpack Compose, and Gemma AI

</div>
