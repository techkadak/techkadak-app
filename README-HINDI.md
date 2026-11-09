# Tech Kadak App 📱🔥 (v1.2.0)

Play Store jaisa app, apne lime + black style me. Roz website ka bot jo naya free app daalta hai, wo app me **khud** aa jaata hai.

## 1. Phone me install / update
1. `TechKadak-v1.1.apk` phone me download karo (WhatsApp / Drive se).
2. File pe tap karo → Android bolega "Install unknown apps" → **Allow**.
3. **Install** (v1.0 already hai to **Update** aayega — data/favourites safe rehte hain, same signing key).

## 2a. v1.2 me naya kya hai 🚀
- 🎒 **Kits** — Home pe "Kits" row. Kit kholo → "Sab install karo" = saare APK apps line se download/install. Kits `app/src/main/assets/kits.json` me hain; bina APK ke badalne ho to site pe `data/kits.json` daalo ya Remote Config key `kits_json` (same JSON) — Remote Config > site > app ke andar wale.
- 🖼️ **Share poster** — app ke page pe "Poster" button: Story 1080x1920 / Square 1080x1080 (lime/black/white) → share. Sample posters: `posters/`.
- ▶️ **YouTube videos** — channel UCpxdwzHB-66cPyssRH5IaTA ka RSS feed. Home pe "Videos" row; app ke page pe us app ki video (naam match ya apps.json me `"videos": ["VIDEO_ID"]`).
- ✨ **Liquid Glass theme** — Settings > Theme > GLASS (default nahi). Android 12+ pe asli blur wala wallpaper, neeche ke phones pe soft gradient. Reduce-motion ON ho to animation band.
- 🔔 **Update pop-up** — har baar app kholne pe GitHub ka latest release check. Naya ho to pop-up: "Abhi update karo" (app me download → install) ya "Baad mein" (sirf is baar ke liye band, agli baar phir aayega). Net nahi = koi pop-up nahi.
  - Emergency: Remote Config `min_version_code` ko kisi version se bada rakho to update zaroori ho jata hai (cancel nahi hota). Normally isse 0/khaali chhodo.
- 📊 **Anonymous usage stats** — Firestore (Mumbai, asia-south1) + Google Analytics. Koi naam/email/phone nahi, sirf random device id. Settings > Privacy > "Usage stats bhejo" (default ON).

## 2c. Owner dashboard 🔐 (sirf ROHIT)
- Web: https://techkadak.github.io/admin/ (kahin link nahi, Google me index nahi). Login: ridhirohit0@gmail.com + password (ya "Google se login"). Pehli baar password bhool gaye to "Password bhool gaye?" dabao — reset mail aayega.
- App me: Settings > "Admin access 🔐" — same login.
- Firestore rules: sirf ridhirohit0@gmail.com padh sakta hai; app sirf apna din ka count likh sakta hai (padh/delete nahi). Rules test: 19/19 pass.
- Dhyan do: ginti sirf v1.2 install hone ke baad se shuru hoti hai — purane v1.1 users count me tab aayenge jab update karenge.
- Google se login karne se pehle email verify kar lo (pehli password login pe verify mail apne aap jata hai).

## 2. v1.1 me naya kya hai
- **Splash**: lime screen → TK logo bounce → "TECH KADAK" type → dot → circle khul ke app (tap = skip).
- **Intro (4 screens)** pehli baar: Paid ka FREE jawab • Official links • Roz naya app • Apna look. Phir **Login (optional)** — "Abhi skip karo →" se bina account sab chalta hai.
- **Customise (Profile → ⚙️ / Customise)**: Theme **AMOLED Black (default) / Dark / Light** • Accent colour (Lime default + 7 presets + **apna colour picker**) • Font size (85%–130%) • Card style **Grid / List / Compact** • Home sections on/off + ↑↓ order • **Hinglish / English** • Website app ke andar kholo (on/off) • Notifications • **Cache saaf karo** • Look reset • Intro dobara.
- **❤️ Favourites** (app page pe dil) + **Haal me dekhe** — naya "Pasand" tab + Home pe rows.
- **Sort** (Naya pehle / A-Z) + **Filter** (Free / Freemium / Open-source + category) — category page, search results, aur "Sab apps" page.
- **Share app**, **App request karo** (Telegram t.me/techkadak), **About** (Developer: Rohit Saini, version + "Up to date ✅" badge, Version history, Privacy, Licenses, Made with 💚 in India).
- **Home-screen widget "Aaj ka App"** — long-press home screen → Widgets → Tech Kadak.
- **In-app website** (techkadak.github.io app ke andar; download links hamesha official site/app me bahar khulte hain).
- **Login (optional)**: Google se • Email + password (verification link mail, "Mail check karo" animated screen) • Phone number OTP (6-digit SMS, auto-read, animated boxes). Logout / account delete Profile me. ⚠️ Free plan pe Firebase poore project ke liye **10 OTP SMS / din** deta hai — khatam ho jaye to app bolta hai "Aaj ke OTP khatam, Google ya Email se login karo".
- **Top bar**: ☀️/🌙 quick Light/Dark toggle + ⬇️ Downloads.
- **Update**: naya version aate hi sheet — "Abhi: v1.1.0 → Naya: v1.2.0", **Kya naya hai**, **Abhi update karo** (download app ke andar, progress bar, APK verify) / **Baad me** (24 ghante baad dobara yaad). Android last me ek baar "Update" tap maangta hai — Play Store ke bahar wale har app me hota hai.

## 2b. Apps app ke andar hi download + install (Play Store jaisa) 🤖
- App page pe bada **INSTALL • xx MB** button + source chip (**Official GitHub / F-Droid / Developer site**). APK hamesha app ke **official source se tap ke time live** aata hai — hum kahin re-host nahi karte.
- Download ke time **Kadak Bot** (apna robo: black body, lime aankhen, seene pe TK) progress track pe dabba bharte hue chalta hai • complete pe uchhal + ✅ • error pe udaas • pause / net gaya to so jaata hai (zZ).
- **Background me chalta hai**: app band karo — notification me naam + % + Cancel. Net gaya to wahi se resume (HTTP Range). Phone restart ke baad bhi queue yaad.
- Complete → notification **"Download complete — Install"** → tap = seedha Android installer. Install ke baad button **OPEN** ban jaata hai; naya version aaye to **UPDATE**.
- Check: GitHub SHA-256 (jab GitHub de), package name match, file size.
- **Play-Store-only** apps → "Play Store pe kholo" (ek tap). **PC / Web** tools → "PC / Web ke liye" official link.
- **Downloads** screen (Home pe ⬇️ icon / Customise → Downloads & Updates): queue, pause / resume / cancel, **Updates** list + **Sab update karo**.
- **Customise → Apps auto-update**: Off / Sirf Wi-Fi pe (default) / Hamesha. Roz 2 baar check; Tech Kadak ka apna update sabse pehle. Android 12+ pe Tech Kadak se install kiye apps **bina tap ke** update; baaki me "Update ready — tap to install".
- Source list: `app/src/main/assets/apk_sources.json` (62 apps: 37 GitHub, 5 F-Droid, 3 Play-only, 17 PC/Web). Website ke `data/apps.json` me kisi app pe `apk_source` (`github:owner/repo` | `fdroid:package` | `url:https://…apk`) + `package_name` daal do to app wahi use karega (bundled list se upar).
- ⚠️ Pehli baar Android "Install unknown apps" permission aur har install pe ek "Install" tap maangta hai — Play Store ke bahar wale har app me hota hai. Phone pe app Play Store se pehle se hai to GitHub/F-Droid wala APK signature alag hone se update nahi hoga — tab app bata deta hai "Play Store se update karo".

## 3. Naye apps app me kaise aate hain (bot)
App `https://techkadak.github.io/data/apps.json` padhta hai. Website ka GitHub bot roz 9 baje ek app add karta hai → app me apne aap. Widget bhi update hota hai.

## 4. Firebase (connected ✅, FREE Spark plan, koi billing nahi)
Project **Tech Kadak** (`tech-kadak`), tere Gmail pe.
- **Push** topic `new_apps` (bot se push ke liye `docs/website-push/README.md`).
- **Authentication**: Email/Password ✅, Google ✅ (release SHA-1/SHA-256 add hai), Phone OTP ✅ (10 SMS/din free limit).

### 🎛️ Remote Config — bina naya APK daale app badlo
Firebase console → **Remote Config** → key pe value likho → **Publish changes**. App ~30 min me (ya dobara kholne pe) le leta hai.

| Key | Kya karta hai | Example |
|---|---|---|
| `announcement_text` | Home pe lime announcement strip (khali = chhupa) | `Diwali special: 5 naye apps 🔥` |
| `announcement_url` | Announcement pe tap → ye link (sirf https) | `https://techkadak.github.io` |
| `featured_slug` | "Aaj ka App" banner + widget me ye app (khali = sabse naya) | `vlc` |
| `home_sections_order` | Home sections ka default order (user ne khud badla ho to uska chalega) | `featured,new,top,favourites,recent,categories,chart` |
| `default_accent` | Sabka default colour (user ka apna colour upar) | `#9BFF2E` |
| `maintenance_message` | Home pe upar lime banner (khali = kuch nahi) | `Server update, 10 min me normal` |
| `min_version_code` | Isse purane versionCode wale ko **zaroori update** (Baad me ka option nahi) | v1.1 = `10100` |

⚠️ `min_version_code` tabhi badhao jab naya APK GitHub Release me daal diya ho — warna log update dhundhte reh jayenge.

## 5. App ka naya version kaise daale (auto update)
Repo: `github.com/techkadak/techkadak-app` (Releases se update aata hai).
1. Code badlo → `gradle.properties` me `VERSION_NAME` / `VERSION_CODE` badhao (1.2.0 → 10200).
2. **Same keystore** se sign karo (Tab ke paas hai) — warna update install nahi hoga.
3. GitHub → Releases → **Draft new release** → tag `v1.2.0` → "Kya naya hai" likho → APK attach → Publish.
4. Sabke app me "Naya update aaya" aa jaata hai. (`.github/workflows/release.yml` se tag push pe ye khud bhi ho sakta hai — 4 secrets: `TK_KEYSTORE_BASE64`, `TK_KEYSTORE_PASSWORD`, `TK_KEY_ALIAS`, `TK_KEY_PASSWORD`.)

## 6. Settings ek jagah
`gradle.properties`: `TK_SITE_BASE` (website), `TK_GITHUB_OWNER` / `TK_GITHUB_REPO` (update kahan se aaye), version.
