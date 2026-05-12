# Solo Levelling Focus Blocker (Android source)

This repository now contains a full Android app source project that helps you:

- Create a daily focus schedule from spreadsheet rows (`HH:mm,HH:mm,focus|break`)
- Block selected apps (for example Instagram) during focus slots
- Lock selected apps for 24 hours (cannot edit settings while lock is active)
- Block configured website keywords/domains (for example porn domains)
- Send repeating focus reminders
- Keep YouTube allowed by default (`com.google.android.youtube`)

## Project structure

- `app/` Android app module
- `app/src/main/java/com/sololevelling/blocker/` app logic, accessibility service, reminder worker
- `app/src/test/` unit tests for blocking logic
- `docs/screenshots/main-screen-mock.png` UI preview image

## Step-by-step: build APK and install on phone

1. Install **Android Studio (latest stable)**.
2. Open this folder in Android Studio:
   - `/home/runner/work/Solo-levelling/Solo-levelling`
3. Let Gradle sync complete.
4. Connect your Android phone with USB debugging enabled (or use wireless debugging).
5. In Android Studio choose:
   - Build Variant: `debug`
   - Menu → **Build > Build Bundle(s) / APK(s) > Build APK(s)**
6. APK output path:
   - `app/build/outputs/apk/debug/app-debug.apk`
7. Install on phone:
   - from Android Studio Run button, or
   - `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## First-time setup on phone

1. Open the app.
2. Enter blocked app package names (comma separated), e.g.:
   - `com.instagram.android, com.facebook.katana`
3. Enter blocked website keywords/domains (comma separated), e.g.:
   - `porn, xnxx, xvideos`
4. Paste schedule rows from your spreadsheet in this format:
   - `09:00,11:00,focus`
   - `11:00,11:30,break`
   - `11:30,14:00,focus`
5. Set reminder interval in minutes (minimum 15).
6. Tap **Save configuration**.
7. Tap **Open accessibility settings** and enable the service for this app.
8. Tap **Open usage access settings** and allow usage access.
9. (Optional) Tap **Lock selected apps for 24h** for strict day lock.

## Important behavior notes

- The app uses Android Accessibility-based enforcement.
- Blocking strength depends on manufacturer restrictions/background policies.
- During focus windows, configured apps are blocked.
- During break windows, focus-window blocking is not active.
- 24-hour lock prevents settings edits until lock expires.
- Configured website keywords are blocked whenever detected in browser UI text.
