package com.techkadak.app

import com.techkadak.app.data.AppsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** v1.5 pass 7: apps.json "screenshots" parse — sirf https, max 6, khali = []. */
class ScreenshotCarouselDataTest {
    @Test
    fun parsesOnlyHttpsAndCapsAtSix() {
        val json = """{"apps":[{"slug":"a","name":"A","screenshots":[
            "https://x.test/1.png","http://insecure.test/2.png","https://x.test/3.png",
            "https://x.test/4.png","https://x.test/5.png","https://x.test/6.png",
            "https://x.test/7.png","https://x.test/8.png"]},
            {"slug":"b","name":"B"}]}"""
        val apps = AppsParser.parse(json)
        val a = apps.first { it.slug == "a" }
        assertEquals(6, a.screenshots.size)
        assertTrue(a.screenshots.all { it.startsWith("https://") })
        assertTrue(apps.first { it.slug == "b" }.screenshots.isEmpty())
    }

    @Test
    fun bundledCatalogHasScreenshotsOnlyFromHttps() {
        val apps = AppsParser.parse(File("src/main/assets/apps.json").readText())
        val all = apps.flatMap { it.screenshots }
        assertTrue("bundled screenshots present", all.isNotEmpty())
        assertTrue(all.all { it.startsWith("https://") })
        // v1.5 cleanup: Meta AI product-page collages and Photopea device photos are not app screens
        assertTrue(apps.filter { it.slug == "photopea" || it.slug == "meta-ai-image-generator" }.all { it.screenshots.isEmpty() })
    }
}
