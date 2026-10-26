import * as Application from 'expo-application';
import Constants from 'expo-constants';

import { useSettingsStore } from './settingsStore';

/** Version and build from the installed APK, plus the database schema version (AC-001-22). */
export function useAppVersion() {
  const schemaVersion = useSettingsStore((s) => s.schemaVersion);
  return {
    version: Application.nativeApplicationVersion ?? Constants.expoConfig?.version ?? '?',
    build:
      Application.nativeBuildVersion ?? String(Constants.expoConfig?.extra?.versionCode ?? '?'),
    schemaVersion,
  };
}
