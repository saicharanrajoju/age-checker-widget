# Age Checker — Android home-screen widget

A native Android widget showing the last age-check result directly on the home screen, plus a tap-to-open animated date-entry screen with a breathing amber dot field and a moving spider.

## Build without Android Studio
1. Upload all files and folders to a **private** GitHub repository (keep `.github/workflows/build-apk.yml`).
2. Open **Actions → Build Android APK → Run workflow**, or push to `main`.
3. Open the completed run → **Artifacts → age-checker-debug-apk**.
4. Extract the ZIP, transfer `app-debug.apk` to your Android phone, and install it. Android may ask you to allow installs from that source.
5. Long-press home screen → **Widgets → Age Checker**, then add it.

## Usage
Tap the widget to open the animated entry screen. Enter `MM/DD/YYYY` or tap the mic to invoke Android speech recognition. **Confirm the displayed date** and tap **CHECK AGE**. The result is updated on the home-screen widget. Speech input depends on the recognition services installed on the phone. For reliability, spoken dates should be verified; the parser intentionally rejects ambiguous transcriptions rather than guessing.

## Important constraints
Android home-screen widgets do not support normal editable fields or smooth continuous custom animation. The widget shows the result; input and animation run in a small companion Activity. This is a first prototype, not a production-ready alcohol ID verification system. It checks age only, not ID validity or other sale requirements. The prototype's spoken date parser handles dates recognized as month-name + numeric day + four-digit year, or numeric month/day/year; it does not yet support every possible natural-language transcription.

## Date behavior
The age check uses the phone's current local date. The widget's saved result is refreshed when opened/checked and by the Android widget update mechanism, not continuously at midnight.
