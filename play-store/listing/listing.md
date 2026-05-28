# Google Play Store Listing — Grocy Fridge Scanner

## App identity
- **Package name:** `com.charleshartmann.grocyfridge`
- **Category:** Food & Drink (primary), Productivity (secondary)
- **Content rating:** Everyone
- **Pricing:** Free (with ads — banner + occasional interstitial)
- **Target audience:** Adults — home cooks, self-hosting enthusiasts, people who already run Grocy
- **Supported Android version:** Android 12 (API 31)+
- **Languages at launch:** English (US)

---

## App title (max 30 chars)

```
Grocy Fridge Scanner
```
(20 / 30)

### Alternative title options (in case the above conflicts)
- `Grocy Scanner — Fridge AI`  (25)
- `Fridge Scanner for Grocy`   (24)

---

## Short description (max 80 chars)

```
Snap your fridge. On-device AI updates your Grocy stock in seconds. No cloud.
```
(76 / 80)

### Alternates
- `Point your camera at your fridge. AI counts items and syncs to Grocy locally.` (78)
- `Photo-based pantry tracking for Grocy. 100% on-device AI. No data leaves you.` (78)

---

## Full description (max 4000 chars)

```
Grocy Fridge Scanner turns your phone into an AI inventory clerk for your self-hosted Grocy server. Open your fridge, take a photo, and watch the app detect food items, count retail units, and propose stock updates — all entirely on your device.

Built for people who already love Grocy and want to spend less time on data entry.


WHAT IT DOES

• Camera scanning — Point at a fridge shelf or a cupboard, tap the shutter, get a list of detected items in seconds.
• On-device AI — Powered by Gemma 4 E2B running through LiteRT with GPU acceleration. The photo never leaves your phone.
• Smart unit detection — Recognizes bags, boxes, cans, jars, and bottles, then maps them to the right Grocy quantity unit (Pack or Piece).
• Fuzzy product matching — "chips", "Chips", and "chip" are merged into one product so your stock stays clean.
• Auto product + location creation — Items the AI sees that don't exist in Grocy yet are created automatically with the right unit and storage location.
• Two-tap review — Every proposed change is shown with a +/- delta. Edit names, adjust counts, or exclude items before syncing.
• Zero-delta skip — Items already at the right count are skipped so syncing is fast and obvious.


INVENTORY MANAGEMENT

• Full stock view — Browse every Grocy product, sorted alphabetically, with the location and unit at a glance.
• Quick actions — Use 1, Add 1, Use All, or set an arbitrary amount with a single tap.
• Delete products — Remove products from Grocy with a confirmation prompt.


SCAN HISTORY

• Every scan recorded with photo thumbnail, timestamp, and item-level details.
• Filter by Fridge, Cupboards, or All.
• Smart retry on failed scans — re-sync existing data or re-run the AI from scratch.
• 25-record rolling cap so storage stays small.


PRIVACY AND SECURITY

• 100% on-device AI inference. Photos never leave your phone.
• Your Grocy API key is stored with AES256-GCM encryption.
• HTTP and HTTPS Grocy instances both supported — perfect for self-hosted local network setups.


WHAT YOU NEED

• Android 12 (API 31) or newer.
• A running Grocy instance with API access.
• About 1.5 GB of free storage for the AI model (downloaded once during setup).


GETTING STARTED

1. Install and open the app.
2. Enter your Grocy URL and API key (or set them in local.properties before building).
3. Test the connection and download the AI model — both happen inside the app.
4. Open the Scanner tab, point at your fridge, and tap the shutter.


WHY GROCY FRIDGE SCANNER

Manual Grocy entry is tedious. Scanning barcodes one at a time is slower than just looking at the shelf. This app uses a real on-device language model to do the looking for you, then hands you a review screen so you stay in control of every change. Nothing leaves your device unless you choose to sync it.

Open source on GitHub. MIT licensed. Built with Kotlin, Jetpack Compose, and Gemma.

Not affiliated with Grocy or Berrybase. Grocy is a trademark of its owner.
```

(approximate length: ~2,750 characters)

---

## Keywords / tags

Food inventory, fridge scanner, pantry tracker, Grocy companion, on-device AI, self-hosted, home inventory, kitchen organizer, food management, AI scanner, Gemma, LiteRT, smart fridge, no cloud, privacy-first, household, food waste, expiry tracker, smart pantry, cupboard scanner

---

## Contact and links

- **Developer name:** Charles Hartmann
- **Developer email:** *(fill in before submission)*
- **Privacy policy URL:** *(host a privacy policy at e.g. your-domain.example/privacy — required by Play Store)*
- **Website:** https://github.com/chartmann1590/GrocyFridgeScanner
- **Source code:** https://github.com/chartmann1590/GrocyFridgeScanner

---

## Required artwork (this folder)

| Asset                          | File                                              | Spec              |
| ------------------------------ | ------------------------------------------------- | ----------------- |
| App icon                       | `icon/play_store_icon_512.png`                    | 512×512 PNG       |
| Feature graphic                | `feature-graphic/feature_graphic_1024x500.png`    | 1024×500 PNG      |
| Phone screenshots (≥ 2)        | `screenshots/phone/*.png`                         | portrait, real device captures |
| 7-inch tablet screenshots      | `screenshots/tablet-7in/*.png`                    | landscape 16:10   |
| 10-inch tablet screenshots     | `screenshots/tablet-10in/*.png`                   | landscape 16:10   |
| Promo video                    | `video/promo_with_captions.mp4`                   | Upload to YouTube, link the URL in Play Console |
| Voiceover script + captions    | `video/voiceover_script.txt`, `video/captions.srt`| —                 |

---

## Release notes for v1.10

```
• Brand-new app icon with a teal scanner look
• Better camera capture flow and faster review screen
• Bug fixes around AI output parsing (bare JSON arrays now handled)
• Banner / interstitial ad polish
```
