# Device Controller – Phase 1 prototype

Android app that becomes the **device owner** of a test phone and can lock/unlock it.
Phase 1 only: no server, no QR enrollment, no push commands yet.

## What it does
- Full-screen lock screen (kiosk / lock-task mode) with an **Emergency call** button (dialer stays allowed)
- Unlock by entering a code (stand-in for the server command in phase 3). Code is in `Config.kt`.
- Re-locks after reboot if it was locked
- Optional tamper protection: blocks factory reset, safe mode, adding users, date/time change, uninstall
- **Release** button removes all of it so the test phone returns to normal

## Build
1. Open this folder in Android Studio (Hedgehog or newer). Let Gradle sync (it downloads Gradle 8.2).
2. Build > Build APK(s). APK: `app/build/outputs/apk/debug/app-debug.apk`

## Test (use a SPARE phone, Android 9+)
1. Factory reset the phone. At setup, **skip adding a Google account** (set-device-owner fails if accounts exist).
2. Enable Developer options > USB debugging. Connect to your computer.
3. `adb install app-debug.apk`
4. `adb shell dpm set-device-owner com.emmanuel.devicecontroller/.AdminReceiver`
   (should print "Success")
5. Open the app: it should show **Device owner: YES**.
6. Tap **Lock phone now**. Try Home, Back, Recents, pulling the notification bar: all blocked.
   Tap **Emergency call** to confirm the dialer opens. Enter the code to unlock.
7. Turn tamper protection ON and confirm factory reset is greyed out in Settings.
8. Reboot while locked: the lock screen should return.

## IMPORTANT
- While tamper protection is ON, you cannot factory reset the phone. Always tap **Release phone** first.
- If you get stuck: `adb shell dpm remove-active-admin com.emmanuel.devicecontroller/.AdminReceiver`
  (works on debug builds/test phones).
- Not yet built: QR provisioning, FCM remote commands, offline due-date lock, Paystack link (phases 2-3).
- Real customers need a signed agreement, working emergency calls at all times and no hidden tracking.
