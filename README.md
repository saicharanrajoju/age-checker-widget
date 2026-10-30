# Age Checker — Abstract Flow Widget V2

A compact native 4×2 Android home-screen widget with translucent static Abstract Flow artwork and a matching animated voice-input panel. It supports widget-native DOB entry, spoken DOB entry, accurate completed-year calculation, eight presets, custom two-color gradients, random themes, and automatic theme changes.

## Build without Android Studio

1. Push the project to GitHub.
2. Open **Actions → Build Android APK → Run workflow**, or push to `main`.
3. Download **age-checker-debug-apk** from the completed run's artifacts.
4. Install `app-debug.apk`, then add **Age Checker** from the Samsung widget picker.

Locally, when Gradle and the Android SDK are available:

```text
gradle :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Usage

Tap the DOB field, arrow, or **NEW** to enter eight digits directly inside the widget. **REFRESH** recalculates the saved DOB using today’s date across all installed widget instances. **VOICE** opens the animated panel and immediately starts Android speech recognition. Use **COLORS** in the panel for presets, custom colors, Surprise Me, or automatic changes.

Android RemoteViews widgets cannot host editable fields or arbitrary 60 FPS custom animation. The widget therefore uses static Canvas artwork; the matching floating Activity animates only while visible. This utility checks an entered age, not identity or legal ID validity.

The age check uses the phone's local date and `java.time`. Daily color changes use an inexact, battery-friendly alarm and may be batched by Android; exact midnight is intentionally not promised.

## Automated verification

Unit tests cover manual formatting and validation, spoken-date examples, leap dates, future dates, and the inclusive 21-year boundary. GitHub Actions runs the tests and debug build on pushes to `main`/`master` and manual dispatch, then uploads the APK.
