# Setup

Requirements: Java 17, Android SDK platform 34 and build tools 34.0.0, Android Studio or Gradle wrapper, Android device / emulator API 26+. Initial dependency installation requires Internet; installed scans do not.

```sh
cd android-app
./gradlew :app:assembleDebug :demo-sender:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r demo-sender/build/outputs/apk/debug/demo-sender-debug.apk
```

Open SafeX AI and continue past optional setup. Manual checks need no permissions. Enable notification warnings and notification access in Settings to demonstrate passive checks. If a physical Android device marks notification access as a restricted setting for sideloaded apps, allow restricted settings from its app-info menu before granting access.

No backend, API key, network feed subscription, broad file-storage grant, SMS access, microphone or overlay permission is needed. Use the system picker for local images and files.

For testing:

```sh
./gradlew :core:testDebugUnitTest :agents:testDebugUnitTest :ui:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

`local.properties` contains a machine-local SDK path and is not committed. Use `sdk.dir=/your/android/sdk` if necessary.
