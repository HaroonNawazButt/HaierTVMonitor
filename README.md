# Haier TV Monitor

A deliberately small native Kotlin Android TV dashboard for Android 11+.

## Dashboard
- RAM: used MB / total MB, available MB, and percentage used.
- CPU: sampled overall CPU utilization percentage (best effort; `/proc/stat` access varies by firmware).
- Internal data storage: used GB / total GB, available GB, and percentage used.
- Refreshes every 3 seconds while the dashboard is in the foreground. It stops refreshing when the app is paused.

## Safety and privacy
- No root access.
- No network permission, analytics, ads, accounts, or telemetry.
- No background service, boot receiver, or persistent notification.
- Does not modify system settings, packages, launcher, or TV middleware.
- Uses Android framework APIs only; no third-party runtime libraries.

## Build
Requirements: JDK 17, Android SDK with platform 35 installed, and Gradle 8.7+.

From this directory:

```powershell
gradle assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

Install to the TV (replace the address with the current ADB target):

```powershell
adb -s 192.168.10.90:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch:

```powershell
adb -s 192.168.10.90:5555 shell monkey -p com.haroon.haiertvmonitor 1
```

## Notes
- The app is listed as an Android TV launcher app and can be opened from the Apps area. It does not replace the existing launcher.
- This first version is a live dashboard app, not a continuously updating home-screen widget/channel. Launcher channel support is a separate integration and depends on the installed Google TV Launcher accepting third-party channels.
- CPU sampling may show `Unavailable` if this TV's firmware restricts `/proc/stat` access. RAM and storage use public Android APIs.
- Display values are based on Android's reported memory and `/data` filesystem values; they may differ from vendor voice-assistant summaries.
