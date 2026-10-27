import type { ConfigContext, ExpoConfig } from 'expo/config';

// versionCode must grow on every build so an APK installs over the last one.
const versionCode = Number(process.env.TUTORA_VERSION_CODE ?? '1');

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'Tutora',
  slug: 'tutora',
  scheme: 'tutora',
  version: '1.1.0',
  orientation: 'portrait',
  icon: './assets/icon.png',
  userInterfaceStyle: 'automatic',
  android: {
    package: 'com.vmleroy.tutora',
    versionCode,
    adaptiveIcon: {
      backgroundColor: '#FAF7F0',
      foregroundImage: './assets/android-icon-foreground.png',
      backgroundImage: './assets/android-icon-background.png',
      monochromeImage: './assets/android-icon-monochrome.png',
    },
    predictiveBackGestureEnabled: false,
  },
  ios: {
    bundleIdentifier: 'com.vmleroy.tutora',
    buildNumber: String(versionCode),
  },
  plugins: [
    './plugins/withReleaseSigning',
    'expo-router',
    'expo-status-bar',
    'expo-font',
    'expo-localization',
    'expo-sqlite',
    [
      'expo-splash-screen',
      {
        image: './assets/splash-icon.png',
        imageWidth: 160,
        backgroundColor: '#FAF7F0',
        dark: { backgroundColor: '#141C19' },
      },
    ],
  ],
  experiments: { typedRoutes: true },
  extra: { versionCode },
});
