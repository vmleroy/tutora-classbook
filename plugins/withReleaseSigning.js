// Signs release builds with the keystore named by env vars, never with the debug key.
// The keystore and its passwords live outside the repo (constitution rule 19, docs/release.md).
const { withAppBuildGradle } = require('expo/config-plugins');

const releaseConfig = `
        release {
            def tutoraKeystore = System.getenv('TUTORA_KEYSTORE_PATH')
            if (tutoraKeystore) {
                storeFile file(tutoraKeystore)
                storePassword System.getenv('TUTORA_KEYSTORE_PASSWORD')
                keyAlias System.getenv('TUTORA_KEY_ALIAS')
                keyPassword System.getenv('TUTORA_KEY_PASSWORD')
            }
        }`;

module.exports = function withReleaseSigning(config) {
  return withAppBuildGradle(config, (mod) => {
    let gradle = mod.modResults.contents;
    if (gradle.includes('TUTORA_KEYSTORE_PATH')) return mod;

    const debugBlock = /(signingConfigs \{\n\s+debug \{[\s\S]*?\n\s{8}\})/;
    if (!debugBlock.test(gradle))
      throw new Error('withReleaseSigning: signingConfigs block not found');
    gradle = gradle.replace(debugBlock, `$1${releaseConfig}`);

    const releaseUsesDebug = /(release \{\n(?:\s*\/\/.*\n)*\s*)signingConfig signingConfigs\.debug/;
    if (!releaseUsesDebug.test(gradle))
      throw new Error('withReleaseSigning: release buildType not found');
    gradle = gradle.replace(releaseUsesDebug, '$1signingConfig signingConfigs.release');

    mod.modResults.contents = gradle;
    return mod;
  });
};
