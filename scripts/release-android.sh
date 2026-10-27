#!/usr/bin/env bash
# Builds a signed release APK: TUTORA_VERSION_CODE=<n> npm run release:android
# Needs JAVA_HOME (JDK 17), ANDROID_HOME and the four TUTORA_KEY* variables (docs/release.md).
set -euo pipefail
cd "$(dirname "$0")/.."

for var in TUTORA_VERSION_CODE TUTORA_KEYSTORE_PATH TUTORA_KEYSTORE_PASSWORD TUTORA_KEY_ALIAS TUTORA_KEY_PASSWORD; do
  if [ -z "${!var:-}" ]; then
    echo "Missing $var. See docs/release.md." >&2
    exit 1
  fi
done
if [ ! -f "$TUTORA_KEYSTORE_PATH" ]; then
  echo "Keystore not found at $TUTORA_KEYSTORE_PATH" >&2
  exit 1
fi

npm run typecheck
npm run lint
npm test

npx expo prebuild --platform android --clean --no-install
(cd android && ./gradlew --no-daemon assembleRelease)

version=$(node -p "require('./package.json').version")
mkdir -p dist
out="dist/tutora-${version}-${TUTORA_VERSION_CODE}.apk"
cp android/app/build/outputs/apk/release/app-release.apk "$out"
apksigner="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)/apksigner"
"$apksigner" verify --print-certs "$out" | grep -E "certificate (DN|SHA-256)"
echo "Signed APK: $out"
