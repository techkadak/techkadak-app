# Tech Kadak v1.3.0 (versionCode 10300), Nov 4 2026, pull-to-refresh with the mascot hand

Asked by ROHIT (2:20 PM IST): "Aisa animation mere app mein add kar sakti hai?" (reference video: a character paw reaching down from the top while the list refreshes).
Flow and feel only borrowed; the artwork is ours (Canvas-drawn: black hoodie sleeve with lime stripe + lime ribbed cuff + white glove with a lime TK dot). No other brand's art.

## What's new
- Pull down at the top of Home, Naye apps, Sab apps, Kits, Videos: the hand reaches down from the top edge, the content follows with a rubber band stretch,
  fingers close in as you pull, a haptic tick at the threshold, "Neeche kheencho..." -> "Chhod do, ho jayega!" -> lime spinner + "Naye apps dhoondh rahi hun..." while it refreshes,
  then the hand lets go and the list springs back. Shown at least 1.3 s so a fast/offline refresh is still seen.
- It runs the same refresh as before (fetch apps.json) plus the Kits/Videos feeds (MainViewModel.pullRefresh).
- Settings -> Animation -> "Refresh animation" switch (on by default). Off = plain Material pull-to-refresh.
- Colours follow the theme (AMOLED / Dark / Light / Liquid Glass): lime = accent, sleeve rim glows lime on dark, text = theme text.
- Respects the system "remove animations" setting (hand does not swing).
- New code: ui/RefreshAnim.kt (TkRefreshBox, PullGeometry), Settings.refreshAnim, tests/RefreshAnimTest.kt. No new dependency.
- Unchanged: in-app updater (cancellable pop-up, no min_version_code), minSdk 24, anti-tamper, R8 + resource shrinking, crash recorder + safe mode, init order.

## Tests
- Unit tests: 131/131 green (128 + 3 new).
- Emulator (Android 9 / API 28, software mode, release APK, our keystore): v1.2.1 -> v1.3.0 adb upgrade launches; fresh install of v1.3.0 launches;
  pull gesture via adb swipe shows hand, status text, spinner (screens/v1.3/pull-to-refresh-emulator.mp4 + pull-to-refresh-strip.jpg);
  REAL in-app update: v1.2.1 -> pop-up (plain text notes) -> downloaded the GitHub release asset -> system installer -> opened as 1.3.0 (10300), no crash.
- Not tested: API 33 (image deleted), a real phone (Oppo/Reno7), frame rate on real hardware (emulator is software-rendered and choppy).

## Release
https://github.com/techkadak/techkadak-app/releases/tag/v1.3.0 (Latest). APK TechKadak-v1.3.0.apk 5,057,310 bytes,
sha256 e5f5b7c4b5365b7cba70644301dc1d70064c214d2558ce99fec8a71e6e61aa26 (GitHub's asset digest and a re-download match the local build).
Release notes are plain text Hinglish (no markdown).
