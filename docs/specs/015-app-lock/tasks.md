# Tasks 015: App lock

Each task lists the criteria it proves and the test file that proves them. Check a box only when its tests pass.

- [x] 1. Domain: `AppLock.kt` with `LockAfter`, `shouldLock` (time since boot passed in) and `maskPix`; `Settings` gains `appLock`, `lockAfter` (5 minutes at first) and `lockOffered`. Test: `core/domain/src/test/kotlin/app/tutora/domain/AppLockRulesTest.kt`. (AC-015-4, AC-015-8, AC-015-16)
- [x] 2. Data: `SettingsRepository` keys `app_lock`, `lock_after` and `lock_offered`, inserted with their defaults; no migration. Covered by task 5's tests reading and writing through the repository.
- [x] 3. Library and adapter: `androidx.biometric` 1.1.0 in `libs.versions.toml`; the `DeviceUnlock` interface; `AndroidDeviceUnlock` (BiometricPrompt with weak biometrics or the device credential; `available()` from BiometricManager on Android 11+ and KeyguardManager below); `FakeDeviceUnlock` in `Harness`. Test: compiles; used by every test from task 5.
- [x] 4. State: `AppLock` in `AppContainer` (`locked` starting true until settings arrive, `left`/`back` from `MainActivity.onStop`/`onStart`, the prompt's own stop ignored, `unlock`, `confirm`, off while the phone has no screen lock); `MainActivity` hides the recent-apps thumbnail on Android 13+ while the lock is on and never sets `FLAG_SECURE`. Test: `app/src/test/kotlin/app/tutora/AppLockTest.kt`. (AC-015-8, AC-015-14)
- [x] 5. Gate: `LockGate` in `TutoraApp` keeps the content composed but invisible, out of accessibility and untouchable, and shows the lock screen (EmptyState with LOCK and "Desbloquear"); Back moves the task to the background; requests from notifications are handled behind it. Test: `AppLockTest.kt`. (AC-015-6, AC-015-7, AC-015-9, AC-015-10, AC-015-11)
- [x] 6. Ajustes › Privacidade, its own page with its state on Ajustes' row: "Bloquear o Tutora" (asks to turn it on and to turn it off), "Bloquear após" chips, the row for a phone with no screen lock with "Abrir configurações", and Ajustes' line "Desligado" or "Bloqueio ligado · 5 minutos". Test: `AppLockTest.kt`. (AC-015-1, AC-015-2, AC-015-3, AC-015-4)
- [x] 7. Design system: icons `EYE` and `EYE_OFF`; `TextField(secret = true)` masked with the eye to reveal and edit; README and gallery entry. Seus dados' Pix field uses it and Ajustes' line uses `maskPix`. Test: `AppLockTest.kt`. (AC-015-16)
- [x] 8. Offers: the setup's step 4 gets "Bloquear o Tutora" (`SetupChoices.appLock`, asking for the unlock when turned on); Seus dados offers it once when the Pix key is first saved (BottomSheet, Ativar / Agora não, sets `lockOffered`). Test: `app/src/test/kotlin/app/tutora/SetupScreensTest.kt` and `AppLockTest.kt`. (AC-015-5)
- [x] 9. Backup e planilha: Backup completo with a folder confirms "Salvar o backup em <pasta>?" (Salvar / Cancelar; Cancelar saves nothing); without a folder it shares as today; no unlock is asked. Test: `app/src/test/kotlin/app/tutora/BackupScreensTest.kt`. (AC-015-15)
- [x] 10. Notifications: `ReminderNotice.Class.canStart`; "Iniciar aula" on the reminder as a broadcast to `ReminderReceiver` that calls `Live.start`, cancels the reminder, and leaves it if another class is already live; Pausar, Retomar and Marcar todas keep working while locked. Test: `app/src/test/kotlin/app/tutora/RemindersTest.kt` (AC-015-13) and `AppLockTest.kt`. (AC-015-12, AC-015-13)
- [x] 11. Strings in both languages (`strings_features.xml`): lock screen, Privacidade rows, Bloquear após, the no-lock row, the Pix offer, the setup row, "Bloqueio ligado", "Iniciar aula". Test: `AppLockTest.kt` switching to English. (AC-015-17)
- [x] 12. Full `./gradlew check` once; the spec's status goes to done in the spec and `docs/specs/README.md`; device checks listed in the PR as pending.

## Pending on a device

- A real fingerprint or face, and falling back to the phone's PIN, pattern or password.
- On Android 9 or 10, the PIN screen doesn't lock Tutora again behind itself.
- Tutora's card in recent apps is blank with the lock on (Android 13+); older phones still show it.
- Screenshots work with the lock on.
- "Iniciar aula" from a real reminder with Tutora closed; the live notification takes its place.
- Coming back from the share sheet or a file picker with "Imediatamente".
- Restoring a backup with the lock on from another phone that has no screen lock: Tutora opens.
