# Releasing an Android APK

Tutora ships as a signed APK, sideloaded onto the phone. An update installs over the previous APK and keeps every lesson only when the new APK is signed with **the same key** and has a **higher `versionCode`**. A different key forces an uninstall, and an uninstall deletes the database.

## The release key (once, and never lose it)

Constitution rule 19: the key lives outside the repo, in a password manager plus an offline copy.

```bash
keytool -genkeypair -v -storetype PKCS12 \
  -keystore ~/secure/tutora-release.p12 \
  -alias tutora -keyalg RSA -keysize 4096 -validity 36500
```

1. Store the `.p12` file and both passwords in the password manager.
2. Copy the `.p12` to an offline drive.
3. Never put it under this repo. `.gitignore` blocks `*.p12`, `*.jks` and `*.keystore` as a second line of defense.

## Tools

- A JDK 17 or newer to run Gradle (`brew install openjdk@17`). The build compiles and tests with JDK 21, which Gradle downloads by itself (toolchain resolver).
- The Android SDK (`brew install --cask android-commandlinetools`, then `sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"`).
- Environment:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

## Build (one command)

```bash
export TUTORA_KEYSTORE_PATH=~/secure/tutora-release.p12
export TUTORA_KEY_ALIAS=tutora
export TUTORA_KEYSTORE_PASSWORD=…   # from the password manager
export TUTORA_KEY_PASSWORD=…
TUTORA_VERSION_CODE=2 ./gradlew check assembleRelease
```

- `check` runs Lint, ktlint and every test first; drop it for a quick rebuild.
- `app/build.gradle.kts` signs the release build from those variables; without `TUTORA_KEYSTORE_PATH` the release APK is left unsigned and cannot be installed, so the debug key is never used by mistake.
- The APK is `app/build/outputs/apk/release/app-release.apk`. Copy it to `dist/tutora-<version>-<versionCode>.apk` and compare the certificate with the last release: `$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs <apk>`.

**Versioning.** `versionCode` must be higher than the APK on the phone, every build. `versionName` in `app/build.gradle.kts` follows the last spec shipped (`1.1.0` = spec 001).

**Application id.** The Kotlin app is `com.vmleroy.tutora.app`. The archived Expo app was `com.vmleroy.tutora`, so both can be installed side by side; the Expo app's data is not carried over.

## Install on the phone

- USB: `adb install -r dist/tutora-1.1.0-2.apk` (`-r` replaces the app and keeps its data).
- Or send the APK to the phone (Drive, a cable) and open it. Allow "install unknown apps" for the app that opens it.

Before installing a new APK, check the date of the last automatic backup (spec 011). The app also copies the database to `tutora-pre-v{n}.db` before any migration.

## Check an update keeps data (AC-001-20)

1. Install build N. Open it and let it create its database. From spec 004 on, add a student.
2. Build N+1 with `TUTORA_VERSION_CODE` one higher and install it with `adb install -r`. Don't uninstall in between.
3. Open the app. It starts normally and the data from step 1 is still there.
4. To confirm on the device itself: `adb shell run-as com.vmleroy.tutora.app ls databases` lists `tutora.db`, plus a `tutora-pre-v{n}.db` copy when the update included a migration.

**Android developer verification.** From September 2026, certified Android phones in Brazil may require apps from registered developers, sideloading included. `adb install` is exempt. Check the current rule before the first release (see `docs/architecture.md`, "Build and sideload").
