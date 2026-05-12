# Solo Levelling Focus Blocker (Android source)

This repository now contains a full Android app source project that helps you:

- Create a daily focus schedule with focus and break windows
- Add daily tasks with reminder times
- Block selected apps (for example Instagram) outside break windows
- Lock selected apps for 24 hours (cannot edit settings while lock is active)
- Block configured website keywords/domains (for example porn domains)
- Send repeating focus check-in reminders
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

### Alternate install path (without Android Studio)

1. Build the debug APK with Gradle:
   - `./gradlew assembleDebug`
2. Connect your phone with USB debugging enabled.
3. Install the APK with adb:
   - `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### Direct APK install on device

1. Copy `app/build/outputs/apk/debug/app-debug.apk` to your phone.
2. Enable **Install unknown apps** for your file manager.
3. Tap the APK and follow the prompts to install.

## First-time setup on phone

1. Open the app.
2. Add focus and break windows (blocked apps are only available during break windows).
3. Add your tasks with reminder times for today.
4. Set the check-in reminder cadence in minutes.
5. Select the apps to block.
6. Enter blocked website keywords/domains (comma separated), e.g.:
   - `porn, xnxx, xvideos`
7. Tap **Save configuration**.
8. Tap **Open accessibility settings** and enable the service for this app.
9. Tap **Open usage access settings** and allow usage access.
10. (Optional) Tap **Lock selected apps for 24h** for strict day lock.

## Important behavior notes

- The app uses Android Accessibility-based enforcement.
- Blocking strength depends on manufacturer restrictions/background policies.
- During focus windows, configured apps are blocked.
- During break windows, blocked apps are available.
- 24-hour lock prevents settings edits until lock expires.
- Configured website keywords are blocked whenever detected in browser UI text.
