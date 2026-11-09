# Tech Kadak v1.2.1 (versionCode 10201), Nov 4 2026, crash fix

## Root cause of the v1.2.0 launch crash (reproduced on an Android 13 emulator, fresh install)
java.lang.RuntimeException: Cannot create an instance of class MainViewModel
Caused by: NullPointerException: Attempt to invoke virtual method 'void N.m0.setValue(java.lang.Object)' on a null object reference
  at MainViewModel.setStatsOn -> MainViewModel.loadV12 -> MainViewModel.<init>
(retraced with mapping.txt; full logcat in screens/v1.2.1/v1.2.0-crash-logcat.txt)

MainViewModel's `init {}` block (line ~84) called loadV12(), which assigned `statsOn`, `videos` and `bundledKits`.
Those `by mutableStateOf` fields were declared further down the class. Kotlin initialises class members
top to bottom, so their delegates were still null → NPE on EVERY launch, on every phone (fresh or upgrade).
It wasn't R8, Firebase, the phone, or the v1.1 data, and Clear data didn't help.
Tests missed it because no test built the real MainViewModel/MainActivity (screens were tested with fake state).

## Changes
- MainViewModel: start-up moved into start(), called from a single `init` that is the LAST member of the class.
  Each optional piece (remote config, v1.2 kits/videos, auth listener, update check) is in its own try.
  Coroutines catch non-cancellation errors (settings flow failure falls back to defaults so the splash never hangs).
- CrashLog.kt: global uncaught-exception handler that writes filesDir/last_crash.txt (version, phone, Android, stack)
  and chains to Android's own handler. On the next launch, a plain Android dialog shows the report with
  "Report copy karo" (copies it to the clipboard) and "Theek hai".
- Safe mode: a crash within 15 s of launch counts as a launch crash. The next launch skips stats, Remote Config
  and the network refresh of videos/kits, and switches Liquid Glass back to AMOLED. The counter resets after 15 s of a clean run.
- TkApp: every start-up call is wrapped (notification channel, workers, push, downloads, stats, lifecycle observer callbacks).
- Integrity.kt (anti-tamper): APK signing cert SHA-256 must equal the release cert d605…43eb. It's stored as XOR'd
  chunks, not one plain string. GET_SIGNING_CERTIFICATES on API 28+, GET_SIGNATURES below.
  Package name must be in.techkadak.app, and a release build must not be debuggable. On a mismatch MainActivity shows a
  full-screen "⚠️ Ye Tech Kadak ka modified/fake version hai…" with a button to techkadak.github.io, and TkApp starts nothing.
  Root/Frida/Xposed are only logged. Installer source isn't checked. Off in debug builds.
- R8 full mode (AGP 8 default) + obfuscation + resource shrinking were already on and stay on.
- Tests: LaunchTest (5 new: real ViewModel constructs, real MainActivity launches under Robolectric,
  fingerprint reconstructs to the release cert, crash log save/clear, guard that the start init stays last). 128/128 green.

## Verified on emulators (release APK, our keystore)
- Android 13: v1.2.0 crash reproduced. v1.1 → v1.2.1 adb upgrade launches. Offline home, kits, "Sab install karo" and the
  crash-report popup (am crash → relaunch) all work.
- Android 9: v1.1 → v1.2.1 adb upgrade, then a fresh install, then the REAL in-app update path (v1.1 popup → download from the GitHub
  release → system installer → opens as 1.2.1). Also checked: online home, kits, app page, a real APK download + system installer,
  Liquid Glass, the crash popup, and a re-signed fake copy showing the warning screen.
