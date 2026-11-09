package com.techkadak.app

import com.techkadak.app.data.AppsParser
import com.techkadak.app.data.Catalog
import com.techkadak.app.update.ReleaseParser
import com.techkadak.app.update.Versions
import org.junit.Assert.*
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class DataTest {
    private fun bundled() = File("src/main/assets/apps.json").readText()

    @Test fun parsesBundledCopy() {
        val apps = AppsParser.parse(bundled())
        assertTrue("apps: ${apps.size}", apps.size >= 60)
        apps.forEach { a ->
            assertTrue(a.slug, a.name.isNotBlank())
            assertTrue(a.slug, a.categories.isNotEmpty())
            assertTrue("${a.slug} has no https link", a.links.isNotEmpty() && a.links.all { it.url.startsWith("https://") })
        }
        // naye pehle
        assertEquals(apps.map { it.added }.sortedDescending(), apps.map { it.added })
    }

    @Test fun parsesLiveWebsiteJson() {
        val text = runCatching {
            val c = URL("https://techkadak.github.io/data/apps.json").openConnection() as HttpURLConnection
            c.connectTimeout = 10000; c.readTimeout = 15000
            c.inputStream.bufferedReader().use { it.readText() }
        }.getOrNull()
        Assume.assumeTrue("offline", text != null)
        val apps = AppsParser.parse(text!!)
        println("LIVE apps.json: ${apps.size} apps, newest=${apps.first().name} (${apps.first().added})")
        assertTrue(apps.size >= 60)
        val cats = Catalog.categories(apps, Config.CATEGORIES)
        println("LIVE categories: $cats")
        assertTrue(cats.map { it.first }.containsAll(listOf("Android Apps", "AI Tools", "Student Tech")))
        assertTrue(apps.count { it.icon != null } >= 50)
        apps.forEach { assertNotNull(it.downloadLinks.firstOrNull()) }
    }

    @Test fun downloadPrefersOfficialStores() {
        val apps = AppsParser.parse(bundled())
        val vlc = apps.first { it.slug == "vlc" }
        assertEquals("F-Droid", vlc.downloadLinks.first().label)
        assertEquals("videolan.org", Catalog.developer(vlc))
    }

    @Test fun ignoresNonHttpsLinksAndBadEntries() {
        val json = """{"apps":[{"slug":"x","name":"X","category":"AI Tools","links":[{"label":"Website","url":"http://evil"},{"label":"GitHub","url":"https://github.com/a/b"}],"added":"2026-11-01"},{"name":"no slug"}]}"""
        val apps = AppsParser.parse(json)
        assertEquals(1, apps.size)
        assertEquals(listOf("AI Tools"), apps[0].categories)
        assertEquals(1, apps[0].links.size)
        assertEquals("github.com/a", Catalog.developer(apps[0]))
        assertTrue(Catalog.isOpenSource(apps[0]))
    }

    @Test fun versionCompare() {
        assertTrue(Versions.isNewer("v1.0.1", "1.0.0"))
        assertTrue(Versions.isNewer("1.1", "1.0.9"))
        assertTrue(Versions.isNewer("2.0.0", "1.9.9"))
        assertFalse(Versions.isNewer("v1.0.0", "1.0.0"))
        assertFalse(Versions.isNewer("1.0.0-beta", "1.0.0"))
        assertFalse(Versions.isNewer("0.9", "1.0.0"))
    }

    @Test fun parsesGithubRelease() {
        val json = """{"tag_name":"v1.2.0","draft":false,"prerelease":false,"body":"Naye features","html_url":"https://github.com/techkadak/techkadak-app/releases/tag/v1.2.0",
          "assets":[{"name":"checksums.txt","browser_download_url":"https://github.com/x/c.txt","size":10},
                    {"name":"TechKadak-v1.2.0.apk","browser_download_url":"https://github.com/techkadak/techkadak-app/releases/download/v1.2.0/TechKadak-v1.2.0.apk","size":5242880}]}"""
        val r = ReleaseParser.parse(json)!!
        assertEquals("1.2.0", r.version)
        assertTrue(r.apkUrl.endsWith(".apk"))
        assertEquals(5242880L, r.apkSize)
        assertNull(ReleaseParser.parse("""{"tag_name":"v9","draft":true,"assets":[]}"""))
        assertNull(ReleaseParser.parse("""{"tag_name":"v9","assets":[{"name":"src.zip","browser_download_url":"https://x/y.zip"}]}"""))
    }

    @Test fun catalogRowsAndSearch() {
        val apps = AppsParser.parse(bundled())
        assertEquals(apps.first(), Catalog.featured(apps, null))
        assertEquals("vlc", Catalog.featured(apps, "vlc")!!.slug)
        assertEquals(apps.first(), Catalog.featured(apps, "does-not-exist"))
        assertEquals(12, Catalog.topPicks(apps).size)
        assertTrue(Catalog.suggestions(apps, "vl").any { it.slug == "vlc" })
        assertTrue(Catalog.byDay(apps).first().first >= Catalog.byDay(apps).last().first)
        assertEquals(apps.size, Catalog.byDay(apps).sumOf { it.second.size })
    }
}
