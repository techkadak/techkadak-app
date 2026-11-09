# Tech Kadak v1.5.0 (versionCode 10500)

APK: dist/TechKadak-v1.5.0.apk (5.16 MB), signed with the same release key as v1.3.0, minSdk 24 (Android 7+), targetSdk 34.

## New
- Opening logo-reveal animation (lime/black/white, tap to skip, respects "remove animations", Settings switch "Opening animation"), credit "Created by Rohit Saini".
- "Support Tech Kadak" Rs 50 UPI card in Settings and About (optional, no pop-up, no payment-success claims).
- About: QR card "App download karo" (opens github.com/techkadak/techkadak-app/releases/latest), Link copy + Share.
- Animated update card (spring-up, staggered notes, percent/MB, success + retry states), cancellable, shown after the opening animation.
- Welcome member card (3D spin-in, gloss, flip), unique colour per member number: once for every NEW account (email, Google or phone) AND once on the first launch after updating to v1.5.0 for existing users (signed-in users by uid, guests by install id; 'since' = first-seen date if known, else today). Queued after the opening animation and behind the update card; never twice. Guests / offline get an install-id number (7 digits), signed-in users get the sequential counter number. Also on Profile with tap-to-replay.
- Browser tab: search bar (Google or DuckDuckGo), tabs, history, bookmarks, downloads, incognito, desktop mode, find in page, dark mode, ad-block toggle, share / open in other browser. INTERNET permission only.
- Screenshot carousel on app pages (official screenshots, 50 of 62 apps).
- Reviews on app pages (real reviews only, one per user per app, login needed to write), own star-bubble icon.
- Suggestion screen "Aap kya feature chahte ho?" from the lightbulb in the top bar (max 500 chars, 3 per day).
- Privacy-safe per-device record for the owner dashboard (model, Android and app version, first/last seen; apps only if installed/opened through Tech Kadak; name only if logged in or typed; "Usage stats bhejo" off = nothing sent and the record is deleted).

## Needs the Firestore rules (firebase/firestore.rules) published in the console before these work
member counter, device record fields, reviews, feedback. Until then the app falls back safely (install-id member number, "reviews load nahi hue", feedback shows a retry message).

## Tests
- 180 unit tests (155 earlier + 25 new for welcome policy, browser logic, device record, reviews/feedback).
- Firestore rules: 68 emulator checks pass (firebase/rulestest).
- Android 9 emulator (software mode): fresh install, upgrade v1.3.0 -> v1.5.0 (settings kept, welcome card shown once, not on the 2nd launch), browser page load, real file download (sha256 identical to the GitHub asset), history/bookmarks/tabs persist, find in page, dark, desktop UA, incognito tab, update card + real in-app update flow (test build 1.2.5 -> real v1.3.0 asset, installer accepted as an update).
- NOT yet run: the real in-app update v1.3.0 -> v1.5.0 (needs the v1.5.0 release to exist), real-account welcome card (Robolectric screenshots only).
