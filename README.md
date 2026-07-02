# Receipt Scanner

An Android app that scans paper receipts and converts them to digital e-receipts. Built with Kotlin + Jetpack Compose.

## Features

- **ML Kit Document Scanner** — auto-crop and scan receipts without needing camera permissions
- **OCR Parsing** — on-device text recognition with regex-based extraction (offline)
- **Gemini AI Mode** — optional online mode for higher accuracy using Google Gemini 2.0 Flash (requires API key)
- **Multi-Page PDF Export** — export single or multiple receipts into one PDF file
- **Gallery Import** — scan existing receipt photos from your gallery
- **Receipt Editing** — edit merchant, category, items, and notes after saving
- **Search & Filter** — search by merchant name, filter by category
- **Data Persistence** — Room database with local storage

## Screenshots

| Home | Scan | Receipt List | Receipt Detail | Settings |
|---|---|---|---|---|
| | | | | |

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Database:** Room + KSP
- **Scanner:** ML Kit Document Scanner (`play-services-mlkit-document-scanner`)
- **OCR:** ML Kit Text Recognition (`com.google.mlkit:text-recognition`)
- **AI:** Google Generative AI (`com.google.ai.client.generativeai`)
- **PDF:** Android Canvas PDF API
- **Build:** Gradle 9.6.1, AGP 8.7.3, Kotlin 2.0.21
- **Min SDK:** 26
- **Target SDK:** 36

## Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or later
- JDK 21
- Android SDK 36

### Build
```bash
git clone https://github.com/welbert23/ReceiptScanner.git
cd ReceiptScanner
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Gemini AI Setup (Optional)
1. Get an API key from [Google AI Studio](https://aistudio.google.com/apikey)
2. Open the app → Settings
3. Enter your API key and enable **AI Gemini** scan mode

## Usage

1. Tap **Scan Receipt** to open the document scanner
2. Capture or select a receipt photo
3. Review the parsed information (merchant, date, total, items, etc.)
4. Edit fields if needed and save
5. View all receipts in the **Receipt List**
6. Export receipts as PDF — single or batch

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
