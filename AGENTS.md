# Receipt Scanner APK - Build Instructions

## Build Command
```cmd
cmd /c "set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot && cd /d "C:\Users\Admin\1 Code program\receiptscanner" && gradlew.bat assembleDebug --no-daemon"
```

## Output APK
`C:\Users\Admin\1 Code program\receiptscanner\ReceiptScanner.apk`

## Key Notes
- JDK 21 at `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot`
- Android SDK at `C:\Users\Admin\AppData\Local\Android\Sdk`
- App: **Receipt Scanner**
- Package: `com.receiptscanner.app`
- **Modern toolchain**: Gradle 9.6.1, AGP 8.7.3, Kotlin 2.0.21
- Uses Kotlin Compose compiler plugin (not legacy composeOptions)
- Uses KSP (not kapt) for Room annotation processing
- Room database + ML Kit Text Recognition + Google Gemini AI
- Compose BOM 2024.10.01, Navigation Compose 2.8.5
- Min SDK 26 (Android 8.0+)
