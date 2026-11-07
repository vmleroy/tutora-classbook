# Plan 015: App lock

Architecture reference: docs/architecture.md (Architecture layers, State management and data flow rule 5, Feature libraries, Spec-driven development and testing). Mockups: none; the lock screen is the design system's EmptyState on the background, and the new rows follow Seus dados and Ajustes › Aulas.

## Approach

The lock is a gate in front of the app's content, not a separate screen in the navigation, so nothing behind it is torn down and everything is where it was once unlocked (AC-015-10).

- **Domain (`:core:domain`, `AppLock.kt`).** Plain rules with the time passed in:
  - `LockAfter` (IMMEDIATELY 0, ONE_MINUTE 1, FIVE_MINUTES 5; FIVE_MINUTES at first).
  - `shouldLock(on, after, leftAtMs, nowMs)`: true when the lock is on and the time away is at least `after` (Imediatamente: any time away). Times are the phone's time since boot, not the wall clock, so changing the phone's clock can't skip the lock.
  - `maskPix(key)`: "•••• " plus the last 4 characters; keys of 4 or fewer become "••••" only; empty stays empty (AC-015-16).
  - `Settings` gains `appLock = false`, `lockAfter = FIVE_MINUTES` and `lockOffered = false` (the Pix offer shown once).
- **Data.** `SettingsRepository` maps the three new fields to keys `app_lock`, `lock_after` and `lock_offered` in the `settings` key/value table. These are rows, not columns, so there's no schema change and no migration. `ensureDefaults` inserts them where they're missing.
- **Adapter (`app/lock/DeviceUnlock`).** A small interface so tests use a fake:
  - `available(): Boolean` says whether the phone has a screen lock.
  - `ask(onResult: (Boolean) -> Unit)` asks for the phone's unlock.
  - `openSecuritySettings()` opens the phone's security settings.

  The Android version uses `androidx.biometric` `BiometricPrompt` with `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`, so fingerprint or face come first and the phone's PIN, pattern or password is the fallback (AC-015-6). `available()` uses `BiometricManager.canAuthenticate` on Android 11+ and `KeyguardManager.isDeviceSecure` below it, because older Android can't check that combination.
- **State (`app/lock/AppLock`).** One holder per process, created in `AppContainer`:
  - `locked: StateFlow<Boolean>` starts true and stays true until the first settings arrive. A cold start therefore never shows a frame of content (AC-015-6), and the splash screen covers that moment as it already does.
  - `left(nowMs)` and `back(nowMs)` are called from `MainActivity.onStop` and `onStart`.
  - `unlock()` asks through `DeviceUnlock`.
  - `confirm(action)` asks for the unlock, then runs `action`; used only to turn the lock off (AC-015-3).

  Turning the phone never stops the activity (`configChanges`, spec 017), so it never counts as leaving (AC-015-8). While Tutora's own unlock prompt is open, `left` is ignored; otherwise the PIN screen on older phones would lock it again behind itself. A lock that is on while the phone has no screen lock (for example after restoring a backup from another phone) counts as off, and Ajustes shows AC-015-2's row.
- **Screens.**
  - `TutoraApp` wraps its content in `LockGate`. While locked, the content stays composed but is drawn invisibly, is removed from accessibility and gets no touches. The lock screen sits over it, and Back moves the task to the background. Requests from notifications are still handled behind the gate, so the app opens on them once unlocked (AC-015-9, 10).
  - `MainActivity` turns off the recent-apps thumbnail (`setRecentsScreenshotEnabled(false)`, Android 13+) while the lock is on. `FLAG_SECURE` is never used, so screenshots are allowed at all times (AC-015-14).
  - Ajustes gets its own Privacidade page; Seus dados gets the masked Pix field and the one-time offer.
  - Ajustes' line, the setup's step 4 and the backup page call `AppLock.confirm`.
- **Notifications.**
  - Pausar, Retomar and Marcar todas como dadas already go through broadcast receivers that never open the activity, so they keep working while Tutora is locked without any change (AC-015-12).
  - Encerrar opens the activity, which lands behind the gate and then opens the end sheet (AC-015-10).
  - `ReminderNotice.Class` gains `canStart` (Aula ao vivo on and no class live). `AndroidReminders` adds "Iniciar aula", a broadcast to `ReminderReceiver` (`ACTION_START_LESSON`) that calls `Live.start(id)` from the container (architecture, rule 5). That posts the live notification and cancels the reminder (AC-015-13). If another class went live in the meantime, nothing starts and the reminder stays, so tapping it opens the class (Android doesn't let a notification button open an app from the background).

## Data

No table or column changes. Three new rows in `settings`, inserted with their defaults by `ensureDefaults`:

| Key | Value | Default |
| --- | --- | --- |
| `app_lock` | `true` / `false` | `false` |
| `lock_after` | `"0"`, `"1"`, `"5"` (minutes) | `"5"` |
| `lock_offered` | `true` / `false` | `false` |

Backups carry the settings table as they already do, so a restore brings the lock choice along.

## Use cases

| Use case | Writes | Adapters called | Queries invalidated |
| --- | --- | --- | --- |
| `saveSettings` (existing) with `appLock`, `lockAfter`, `lockOffered` | `settings` rows | none | the `Settings` Flow re-emits; `AppLock` follows it |
| `Setup.finish` with `SetupChoices.appLock` | `settings.app_lock` in the setup's transaction | none | same |
| `Live.start` (existing), from `ReminderReceiver` | `lesson`, `lesson_timer` | `LiveNotifier.show`; the reminder notification is cancelled | Room Flows of lessons |

The unlock itself writes nothing: whether Tutora is locked is kept in memory only, for the process.

## Screens and components

- **Lock screen:** `EmptyState` (icon `LOCK`, title "Tutora", body "Desbloqueie para continuar", action `Button` "Desbloquear") centred on `background`. It works upright and sideways, at every text and interface size, in every theme (AC-015-11). It's built from existing components, so there's no new one.
- **Privacidade (its own page on Ajustes):**
  - A `ListGroup` holding a `ListRow` "Bloquear o Tutora" with a `Switch`.
  - Under it, "Bloquear após" uses `Chip`s, as "Pedir confirmação" does.
  - With no screen lock, the row is disabled, says AC-015-2's text and has "Abrir configurações".
- **Pix field:** `TextField` gains `secret: Boolean`. When secret, it shows `maskPix` and isn't editable until the eye `IconButton` reveals it. The reveal lasts while the page is open. This is a change to the design-system component, so it needs:
  - its README;
  - a gallery entry;
  - two new icons, `EYE` and `EYE_OFF`, added as vector drawables beside the others.
- **Pix offer:** a `BottomSheet` shown once, when the Pix field loses focus with a key, the lock off and the phone having a screen lock. It has "Ativar" (which asks for the unlock, as AC-015-1 does) and "Agora não". Either answer sets `lockOffered`.
- **Ajustes:** Seus dados' line reads the name, `maskPix`, and "Bloqueio ligado" when the lock is on.
- **Setup step 4:** a third `ListRow` with a `Switch`. Turning it on asks for the unlock; with no screen lock it is disabled and says so.
- **Backup e planilha:** Backup completo with a folder set shows a `BottomSheet` "Salvar o backup em <folder name>?" with Salvar and Cancelar before `saveToFolder`; without a folder it goes to the share sheet as today. No unlock is asked; the automatic backup never asks (AC-015-15).
- **Strings:** every string goes in `strings_features.xml` (both languages), including the reminder's "Iniciar aula" (AC-015-17).

## Test strategy

Lean, on the JVM, one test per criterion:

- **Domain:** `core/domain/src/test/.../AppLockRulesTest.kt` covers `shouldLock` for each "Bloquear após" choice (AC-015-4, 8) and `maskPix` (AC-015-16).
- **Screens:** `app/src/test/.../AppLockTest.kt` uses Robolectric and the Compose rule, with `FakeDeviceUnlock` (answer, available, a counter of asks) added to `Harness` and a fake clock for `AppLock`. It covers turning the lock on and off, no screen lock, the lock screen, locking again, cancelling, state kept after unlocking, sideways, Backup asking, the Pix mask and the offer, and the language (AC-015-1 to 11, 14 to 17).
- **Notifications:** `app/src/test/.../RemindersTest.kt` gets AC-015-13 (`canStart`, and the receiver starting the class). `AppLockTest` covers AC-015-12 by firing the receivers' intents while locked.
- **Setup:** `SetupScreensTest.kt` gets AC-015-5's switch; the Pix offer is in `AppLockTest`.
- **Device checks, listed in the PR as pending:**
  - a real fingerprint or face, and falling back to the PIN;
  - the recent-apps thumbnail on Android 13+, and what older phones show;
  - Iniciar aula from a real reminder with Tutora closed;
  - the PIN screen on an Android 9/10 phone not locking again behind itself.

## Risks

- **Leaving for a picker or the share sheet.** The folder and file pickers, the share sheet and the phone's settings all stop the activity. With "Imediatamente" they lock Tutora on return. Nothing is lost: a picker's result is still delivered behind the gate and shown after unlocking (AC-015-10). If this feels heavy on a device, we could exempt Tutora's own pickers; that would be a product decision for Victor.
- **Recent apps before Android 13.** These phones show the last screen in recent apps, because hiding it there means blocking screenshots, which Victor ruled out. AC-015-14 says "where the phone allows it".
- **A restored lock with no screen lock.** A backup restored onto a phone with no screen lock would lock Tutora with no way in. To avoid it, a lock that is on counts as off until the phone has a screen lock (see Approach).
- **`androidx.biometric`.** New dependency: `androidx.biometric:biometric` 1.1.0, the latest stable release, in `libs.versions.toml`. It works offline and needs `FragmentActivity`, which `AppCompatActivity` already is. On Android 9 and 10, BiometricPrompt with the device credential shows the system's PIN screen as a separate activity; `AppLock` ignores the stop it causes.
- **Robolectric and BiometricPrompt.** The prompt can't run under Robolectric, so tests use `FakeDeviceUnlock`. The real prompt is a pending device check.
- **Sheets over the lock.** A sheet or the date picker opens in a window of its own, above the gate. While locked they draw nothing (`LocalCovered` in the design system) and come back once unlocked; what is typed inside a sheet comes back only where the screen keeps it, as most do.
