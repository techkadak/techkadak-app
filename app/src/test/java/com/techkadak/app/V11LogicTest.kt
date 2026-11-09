package com.techkadak.app

import com.techkadak.app.data.AppsParser
import com.techkadak.app.data.Catalog
import com.techkadak.app.data.HomeSection
import com.techkadak.app.data.PriceFilter
import com.techkadak.app.data.RemoteUi
import com.techkadak.app.data.SectionPref
import com.techkadak.app.data.SettingsStore
import com.techkadak.app.data.SortMode
import com.techkadak.app.data.UserSettings
import com.techkadak.app.auth.AuthManager
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** v1.1: Remote Config parsing, home sections, sort/filter, favourites list helpers. */
class V11LogicTest {
    private val apps = AppsParser.parse(File("src/main/assets/apps.json").readText())

    @Test fun remoteUiParsesSafely() {
        val m = mapOf(
            "announcement_text" to "  Diwali special 🔥 ", "announcement_url" to "http://not-https.com", "featured_slug" to "vlc",
            "home_sections_order" to "top,featured", "default_accent" to "#FF8A1F", "maintenance_message" to "", "min_version_code" to "10200",
        )
        val r = RemoteUi.from { m[it] ?: "" }
        assertEquals("Diwali special 🔥", r.announcementText)
        assertNull("non-https url ignore", r.announcementUrl)
        assertEquals("vlc", r.featuredSlug)
        assertEquals(0xFFFF8A1FL, r.defaultAccent)
        assertNull(r.maintenanceMessage)
        assertTrue(r.needsForceUpdate(10100))
        assertFalse(r.needsForceUpdate(10200))
        // Number type "10200.0" bhi chale
        assertEquals(10200L, RemoteUi.from { if (it == "min_version_code") "10200.0" else "" }.minVersionCode)
        // defaults = kuch nahi badalta
        val d = RemoteUi.from { RemoteUi.KEYS[it] ?: "" }
        assertFalse(d.hasAnnouncement); assertNull(d.defaultAccent); assertFalse(d.needsForceUpdate(10100)); assertNull(d.homeSectionsOrder)
    }

    @Test fun colorParsing() {
        assertEquals(0xFF9BFF2EL, RemoteUi.parseColor("9bff2e"))
        assertEquals(0x809BFF2EL, RemoteUi.parseColor("#809BFF2E"))
        assertNull(RemoteUi.parseColor("lime"))
        assertNull(RemoteUi.parseColor("#12345"))
        assertNull(RemoteUi.parseColor(null))
    }

    @Test fun homeSectionOrder() {
        val o = HomeSection.parseOrder("chart, TOP ,bogus,featured,top")
        assertEquals(listOf(HomeSection.CHART, HomeSection.TOP, HomeSection.FEATURED), o.take(3))
        assertEquals(HomeSection.entries.size, o.size)
        assertEquals(HomeSection.DEFAULT, HomeSection.parseOrder(null))
        // user ka order Remote Config se upar
        val user = listOf(SectionPref(HomeSection.NEW, false), SectionPref(HomeSection.FEATURED, true))
        val enc = UserSettings.encodeSections(user)
        assertEquals("new:0,featured:1", enc)
        val dec = UserSettings.decodeSections(enc)!!
        assertEquals(HomeSection.NEW, dec[0].section); assertFalse(dec[0].visible)
        assertEquals(HomeSection.entries.size, dec.size)
        assertEquals(dec, UserSettings(sections = dec).sectionPrefs("chart"))
        assertEquals(HomeSection.CHART, UserSettings().sectionPrefs("chart").first().section)
    }

    @Test fun sortAndFilter() {
        val az = Catalog.sort(apps, SortMode.AZ)
        assertEquals(az.map { it.name.lowercase() }.sorted(), az.map { it.name.lowercase() })
        val newest = Catalog.sort(az, SortMode.NEW)
        assertEquals(newest.map { it.added.take(10) }.sortedDescending(), newest.map { it.added.take(10) })
        val free = Catalog.filter(apps, null, PriceFilter.FREE)
        assertTrue(free.isNotEmpty() && free.all { it.pricing.equals("Free", true) })
        val ai = Catalog.filter(apps, "AI Tools", PriceFilter.ALL)
        assertTrue(ai.isNotEmpty() && ai.all { "AI Tools" in it.categories })
        val os = Catalog.filter(apps, null, PriceFilter.OPEN_SOURCE)
        assertTrue(os.all { Catalog.isOpenSource(it) })
        assertEquals(apps.size, Catalog.filter(apps, null, PriceFilter.ALL).size)
    }

    @Test fun favouritesKeepOrderAndDropMissing() {
        val slugs = listOf(apps[3].slug, "gayab-app", apps[0].slug)
        val got = Catalog.bySlugs(apps, slugs)
        assertEquals(listOf(apps[3], apps[0]), got)
        assertEquals(listOf("a", "b"), SettingsStore.split(SettingsStore.join(listOf("a", "b"))))
        assertEquals(emptyList<String>(), SettingsStore.split(null))
    }

    @Test fun emailValidation() {
        assertTrue(AuthManager.validEmail("ridhi.rohit@gmail.com"))
        assertFalse(AuthManager.validEmail("rohit@gmail"))
        assertFalse(AuthManager.validEmail("rohit gmail.com"))
    }

    @Test fun versionIsHigherThanV10() {
        assertTrue(BuildConfig.VERSION_CODE > 10000)
        assertTrue(com.techkadak.app.update.Versions.isNewer(BuildConfig.VERSION_NAME, "1.0.9"))
    }

    @Test fun phoneNormalize() {
        assertEquals("+919876543210", AuthManager.normalizeIndianPhone("98765 43210"))
        assertEquals("+919876543210", AuthManager.normalizeIndianPhone("+91 98765-43210"))
        assertEquals("+919876543210", AuthManager.normalizeIndianPhone("09876543210"))
        assertEquals("+919876543210", AuthManager.normalizeIndianPhone("919876543210"))
        assertNull(AuthManager.normalizeIndianPhone("12345"))
        assertNull(AuthManager.normalizeIndianPhone("1234567890"))
    }

    @Test fun releaseHistoryParsesAndSorts() {
        val json = """[{"tag_name":"v1.0.0","published_at":"2026-11-04T01:00:00Z","body":"pehla","html_url":"https://github.com/x/1"},
            {"tag_name":"v1.1.0","published_at":"2026-11-04T03:00:00Z","body":"customise","html_url":"https://github.com/x/2"},
            {"tag_name":"v2.0.0-beta","prerelease":true}]"""
        val l = com.techkadak.app.update.ReleaseListParser.parse(json)
        assertEquals(listOf("1.1.0", "1.0.0"), l.map { it.version })
        assertEquals("2026-11-04", l[0].date)
    }

    @Test fun githubAssetPick() {
        val rel = org.json.JSONObject("""{"tag_name":"v2.3.1","assets":[
            {"name":"app-x86_64-release.apk","browser_download_url":"https://github.com/a/b/x86.apk","size":10},
            {"name":"app-armeabi-v7a-release.apk","browser_download_url":"https://github.com/a/b/v7.apk","size":11},
            {"name":"app-arm64-v8a-release.apk","browser_download_url":"https://github.com/a/b/arm64.apk","size":12,"digest":"sha256:abc123"},
            {"name":"app-universal-release.apk","browser_download_url":"https://github.com/a/b/uni.apk","size":30},
            {"name":"notes.txt","browser_download_url":"https://github.com/a/b/n.txt","size":1}]}""")
        val r = com.techkadak.app.install.ApkResolver.pickGithubAsset(rel, null)!!
        assertEquals("https://github.com/a/b/uni.apk", r.url); assertEquals("2.3.1", r.versionName)
        val h = com.techkadak.app.install.ApkResolver.pickGithubAsset(rel, "arm64")!!
        assertEquals(12L, h.size); assertEquals("abc123", h.sha256)
        assertNull(com.techkadak.app.install.ApkResolver.pickGithubAsset(rel, "^nomatch$", strict = true))
    }

    @Test fun fdroidUrl() {
        val (u, c) = com.techkadak.app.install.ApkResolver.fdroidUrl("org.videolan.vlc", org.json.JSONObject("""{"packageName":"org.videolan.vlc","suggestedVersionCode":13070208}"""))!!
        assertEquals("https://f-droid.org/repo/org.videolan.vlc_13070208.apk", u); assertEquals(13070208L, c)
    }

    @Test fun sourceValidation() {
        val S = com.techkadak.app.install.ApkSource
        assertTrue(S.valid("github:bitwarden/android")); assertTrue(S.valid("fdroid:org.videolan.vlc")); assertTrue(S.valid("url:https://example.org/a.apk"))
        assertFalse(S.valid("url:http://insecure/a.apk")); assertFalse(S.valid("github:../etc")); assertFalse(S.valid("mirror:apkpure"))
    }

    @Test fun bundledMappingCoversCatalog() {
        val root = java.io.File("src/main/assets")
        val map = org.json.JSONObject(java.io.File(root, "apk_sources.json").readText()).getJSONObject("apps")
        val apps = com.techkadak.app.data.AppsParser.parse(java.io.File(root, "apps.json").readText())
        val missing = apps.filter { !map.has(it.slug) }.map { it.slug }
        assertTrue("missing: $missing", missing.isEmpty())
        var apk = 0
        map.keys().forEach { k ->
            val s = com.techkadak.app.install.ApkSource.fromJson(map.getJSONObject(k))!!
            if (s.kind == com.techkadak.app.install.ApkSource.Kind.APK) { apk++; assertTrue(k, com.techkadak.app.install.ApkSource.valid(s.source!!)); assertNotNull(k, s.packageName) }
        }
        assertTrue(apk >= 40)
    }

    @Test fun updateDetection() {
        val R = com.techkadak.app.install.ResolvedApk("https://x/a.apk", 1, "1.5.0", null, null, "a.apk")
        assertTrue(com.techkadak.app.install.ApkResolver.isUpdate("1.4.9", 10, R))
        assertFalse(com.techkadak.app.install.ApkResolver.isUpdate("1.5.0", 10, R))
        assertTrue(com.techkadak.app.install.ApkResolver.isUpdate("x", 100, R.copy(versionCode = 101)))
    }

    @Test fun dlItemRoundTrip() {
        val d = com.techkadak.app.install.DlItem("vlc", "VLC", null, "F-Droid", "fdroid:org.videolan.vlc", "org.videolan.vlc", null, com.techkadak.app.install.DlStatus.PAUSED, 10, 100, auto = true)
        val back = com.techkadak.app.install.DlItem.fromJson(d.toJson())
        assertEquals(d.copy(updatedAt = back.updatedAt), back); assertEquals(0.1f, back.progress, 0.001f)
    }

    @Test fun onboardingNeverNamesBrands() {
        val apps = com.techkadak.app.data.AppsParser.parse(java.io.File("src/main/assets/apps.json").readText())
        apps.forEach { a ->
            val l = com.techkadak.app.ui.paidLine(a)
            a.replaces?.split(' ', ',', '/')?.filter { it.isNotEmpty() && it[0].isUpperCase() && it.length > 3 }?.forEach { w -> assertFalse("${a.slug}: $l", l.contains(w) && w !in listOf("PAID", "PDF", "VPN")) }
        }
        val scan = apps.first { it.replaces?.contains("scanner", true) == true }
        assertEquals("PAID scanner apps ka free jawab ✅", com.techkadak.app.ui.paidLine(scan))
    }

    @Test fun realGithubReleaseParses() {
        val json = javaClass.classLoader!!.getResource("github-latest-v1.1.0.json")!!.readText()
        val r = com.techkadak.app.update.ReleaseParser.parse(json)!!
        assertEquals("1.1.0", r.version)
        assertTrue(r.apkUrl.startsWith("https://github.com/techkadak/techkadak-app/releases/download/v1.1.0/") && r.apkUrl.endsWith(".apk"))
        assertTrue(r.apkSize > 3_000_000)
        // v1.0 wale phone ko ye update dikhega, v1.1 wale ko nahi
        assertTrue(com.techkadak.app.update.Versions.isNewer(r.version, "1.0.0"))
        assertFalse(com.techkadak.app.update.Versions.isNewer(r.version, "1.1.0"))
    }
}
