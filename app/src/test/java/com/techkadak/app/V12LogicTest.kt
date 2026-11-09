package com.techkadak.app

import com.techkadak.app.data.*
import com.techkadak.app.stats.OwnerStats
import com.techkadak.app.stats.Stats
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class V12LogicTest {
    private val apps = AppsParser.parse(File("src/main/assets/apps.json").readText())

    @Test fun bundledKitsAreValidAndOnlyCatalogApps() {
        val kits = Kits.parse(File("src/main/assets/kits.json").readText(), apps)
        assertTrue(kits.size >= 5)
        val slugs = apps.map { it.slug }.toSet()
        kits.forEach { k ->
            assertTrue(k.name, k.slugs.size in 4..6)
            k.slugs.forEach { assertTrue("${k.id}:$it", it in slugs) }
        }
        assertTrue(kits.any { it.name == "Student Kit" })
    }

    @Test fun kitsDropUnknownAppsAndBadEntries() {
        val json = """{"kits":[{"id":"xx","name":"X","apps":["vlc","nope","seal"]},{"id":"BAD ID","name":"Y","apps":["vlc","seal"]},{"id":"one","name":"One","apps":["vlc"]}]}"""
        val k = Kits.parse(json, apps)
        assertEquals(1, k.size)
        assertEquals(listOf("vlc", "seal"), k[0].slugs)
        assertTrue(Kits.parse("not json", apps).isEmpty())
        assertEquals(k, Kits.choose(k, emptyList()))
        assertEquals(k, Kits.choose(null, k))
    }

    @Test fun remoteKitsJsonKey() {
        val r = RemoteUi.from { if (it == "kits_json") " {\"kits\":[]} " else "" }
        assertEquals("{\"kits\":[]}", r.kitsJson)
        assertNull(RemoteUi.from { "" }.kitsJson)
    }

    @Test fun parsesYoutubeFeedAndMatchesApps() {
        val v = Videos.parseFeed(File("src/main/assets/videos.xml").readText())
        assertTrue(v.isNotEmpty())
        v.forEach { assertTrue(Regex("^[A-Za-z0-9_-]{11}$").matches(it.id)); assertTrue(it.url.startsWith("https://www.youtube.com/watch?v=")) }
        val tb = apps.first { it.slug == "image-toolbox" }
        assertTrue(Videos.forApp(tb, v).isNotEmpty())
        val obt = apps.first { it.slug == "obtainium" }
        assertTrue(Videos.forApp(obt, v).isNotEmpty())
        // "Jan" jaisa chhota naam galti se match na ho
        val jan = apps.first { it.slug == "jan" }
        val fake = listOf(TkVideo("abcdefghijk", "January me 5 apps", "jan bhi dekho", "2026-01-01"))
        assertTrue(Videos.forApp(jan, fake).isEmpty())
        assertEquals("DaVinci Resolve", Videos.matchName("DaVinci Resolve (free version)"))
        assertEquals("Goodtime", Videos.matchName("Goodtime: Pomodoro Timer"))
    }

    @Test fun explicitVideoField() {
        val json = """{"apps":[{"slug":"a","name":"Zzzz","what":"x","links":[],"videos":["rkRyoY8qHzg","bad"]}]}"""
        val a = AppsParser.parse(json).first()
        assertEquals(listOf("rkRyoY8qHzg"), a.videoIds)
        val v = Videos.parseFeed(File("src/main/assets/videos.xml").readText())
        assertEquals("rkRyoY8qHzg", Videos.forApp(a, v).first().id)
    }

    @Test fun statsDayDocMatchesRules() {
        val d = Stats.Day("2026-11-04")
        d.ev["app_open"] = 3; d.ic["vlc"] = 1; d.kits["student"] = 1
        val doc = d.toDoc(10200, true)
        assertEquals(setOf("v", "n", "ev", "ic", "kits"), doc.keys)
        assertTrue(doc.keys.all { it in setOf("v", "n", "ev", "ic", "id", "kits", "ps", "up") })
        assertEquals(10200L, doc["v"])
        val back = Stats.Day.fromJson(d.toJson())!!
        assertEquals(3L, back.ev["app_open"])
        assertTrue(Regex("^20[0-9]{2}-[01][0-9]-[0-3][0-9]$").matches(Stats.today()))
        assertTrue(Stats.validId("0123456789abcdef0123456789abcdef"))
        assertFalse(Stats.validId("ridhirohit0@gmail.com"))
        assertEquals("email", Stats.normLogin("password")); assertEquals("google", Stats.normLogin("google.com")); assertEquals("none", Stats.normLogin(null))
    }

    @Test fun ownerCheckAndDays() {
        assertTrue(OwnerStats.isOwner("RidhiRohit0@gmail.com "))
        assertFalse(OwnerStats.isOwner("punjabistar123@gmail.com"))
        assertFalse(OwnerStats.isOwner(null))
        val d = OwnerStats.lastDays(30)
        assertEquals(30, d.size); assertEquals(Stats.today(), d.last()); assertTrue(d.first() < d.last())
    }

    @Test fun posterOneLiner() {
        val a = apps.first { it.slug == "vlc" }
        val s = com.techkadak.app.ui.Poster.oneLiner(a)
        assertTrue(s.isNotBlank() && s.length <= 130)
    }

    @Test fun versionIs150() {
        assertEquals(10501, BuildConfig.VERSION_CODE)
        assertEquals("1.5.1", BuildConfig.VERSION_NAME)
    }
}
