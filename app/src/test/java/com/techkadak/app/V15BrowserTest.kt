package com.techkadak.app

import com.techkadak.app.browser.*
import org.junit.Assert.*
import org.junit.Test

class V15BrowserTest {
    private val g = SearchEngine.GOOGLE
    private val d = SearchEngine.DUCKDUCKGO

    @Test fun urlsVersusSearch() {
        assertEquals(Resolved.Url("https://github.com"), UrlInput.resolve("github.com", g))
        assertEquals(Resolved.Url("https://example.org/a?b=1"), UrlInput.resolve(" example.org/a?b=1 ", g))
        assertEquals(Resolved.Url("https://sub.domain.co.in/path"), UrlInput.resolve("https://sub.domain.co.in/path", g))
        assertEquals(Resolved.Url("http://example.com"), UrlInput.resolve("http://example.com", g))
        assertEquals(Resolved.Url("http://192.168.1.1:8080/x"), UrlInput.resolve("192.168.1.1:8080/x", g))
        assertEquals(Resolved.Url("http://localhost:3000"), UrlInput.resolve("localhost:3000", g))
        assertEquals(Resolved.Url("https://site.com:8443/x"), UrlInput.resolve("site.com:8443/x", g))
        assertTrue(UrlInput.resolve("best free apps", g) is Resolved.Search)
        assertTrue(UrlInput.resolve("kotlin", g) is Resolved.Search)
        assertTrue(UrlInput.resolve("what is 2.5 times 3", g) is Resolved.Search)
        assertTrue(UrlInput.resolve("hello.", g) is Resolved.Search)
        val s = UrlInput.resolve("tech kadak & more", d) as Resolved.Search
        assertEquals("https://duckduckgo.com/?q=tech+kadak+%26+more", s.url)
        assertEquals("https://www.google.com/search?q=cats", (UrlInput.resolve("cats", g) as Resolved.Search).url)
    }

    @Test fun dangerousSchemesBlocked() {
        listOf("javascript:alert(1)", "file:///sdcard/x", "content://x/y", "intent://x#Intent;end", "data:text/html,hi", "view-source:https://a.com", "JAVASCRIPT:1", "market://details?id=a").forEach {
            assertEquals(it, Resolved.Blocked, UrlInput.resolve(it, g))
        }
        assertEquals(Resolved.Blocked, UrlInput.resolve("   ", g))
        assertEquals(Resolved.Url("about:blank"), UrlInput.resolve("about:blank", g))
    }

    @Test fun adBlock() {
        assertTrue(AdBlock.isBlocked("doubleclick.net"))
        assertTrue(AdBlock.isBlocked("ad.doubleclick.net"))
        assertTrue(AdBlock.isBlocked("x.y.googlesyndication.com"))
        assertFalse(AdBlock.isBlocked("example.com"))
        assertFalse(AdBlock.isBlocked("notdoubleclick.net"))
        assertFalse(AdBlock.isBlocked("github.com"))
        assertFalse(AdBlock.isBlocked("ad.doubleclick.net", enabled = false))
        assertFalse(AdBlock.isBlocked(""))
    }

    @Test fun historyDedupeCapAndSearch() {
        var h = emptyList<HistoryEntry>()
        h = BrowserLists.addHistory(h, "https://a.com/", "A", 1)
        h = BrowserLists.addHistory(h, "https://a.com/", "A2", 2)
        assertEquals(1, h.size); assertEquals("A2", h[0].title)
        h = BrowserLists.addHistory(h, "https://b.com/", "", 3)
        assertEquals("b.com", h[0].title)
        h = BrowserLists.addHistory(h, "javascript:1", "x", 4)
        h = BrowserLists.addHistory(h, "about:blank", "x", 5)
        assertEquals(2, h.size)
        assertEquals(1, BrowserLists.searchHistory(h, "A2").size)
        for (i in 1..700) h = BrowserLists.addHistory(h, "https://x$i.com/", "t$i", 10L + i)
        assertEquals(BrowserLists.HISTORY_MAX, h.size)
        assertEquals("https://x700.com/", h[0].url)
        val back = BrowserLists.decodeHistory(BrowserLists.encodeHistory(h))
        assertEquals(h, back)
        assertEquals(h.size - 1, BrowserLists.removeHistory(h, h[0].url, h[0].at).size)
    }

    @Test fun bookmarksToggleAndRoundTrip() {
        var b = emptyList<Bookmark>()
        b = BrowserLists.toggleBookmark(b, "https://a.com/", "A\tTab\nNew", 1)
        b = BrowserLists.toggleBookmark(b, "https://b.com/", "", 2)
        assertTrue(BrowserLists.isBookmarked(b, "https://a.com/"))
        assertEquals("b.com", b[0].title)
        val back = BrowserLists.decodeBookmarks(BrowserLists.encodeBookmarks(b))
        assertEquals(2, back.size); assertEquals("A Tab New", back[1].title)
        b = BrowserLists.toggleBookmark(b, "https://a.com/", "", 3)
        assertFalse(BrowserLists.isBookmarked(b, "https://a.com/"))
        assertEquals(emptyList<Bookmark>(), BrowserLists.decodeBookmarks(""))
        assertEquals(emptyList<Bookmark>(), BrowserLists.decodeBookmarks("garbage"))
    }

    @Test fun incognitoCleanup() {
        assertEquals(listOf("a.b.example.com", "b.example.com", "example.com"), IncognitoCleaner.parentDomains("a.b.example.com"))
        val c = IncognitoCleaner.expiryCookies("www.site.com", "sid=1; theme=dark")
        assertTrue(c.any { it.startsWith("sid=; Max-Age=0") && it.contains("Domain=site.com") })
        assertTrue(c.any { it.startsWith("theme=; Max-Age=0") })
        assertEquals(emptyList<String>(), IncognitoCleaner.expiryCookies("a.com", null))
        assertEquals(setOf("b.com"), IncognitoCleaner.hostsToClear(listOf("a.com", "b.com"), listOf("a.com")))
    }

    @Test fun downloadNames() {
        assertEquals("app.apk", DownloadNames.fileName("https://x.com/dl/app.apk?x=1", null, null))
        assertEquals("my file.pdf", DownloadNames.fileName("https://x.com/get", "attachment; filename=\"my file.pdf\"", null))
        assertEquals("a_b.zip", DownloadNames.fileName("https://x.com/a%2Fb.zip", null, null).replace("/", "_"))
        assertEquals("download.pdf", DownloadNames.fileName("https://x.com/", null, "application/pdf"))
        assertFalse(DownloadNames.fileName("https://x.com/", "attachment; filename=\"../../etc/passwd\"", null).contains("/"))
    }

    @Test fun desktopUa() {
        assertTrue(BrowserUa.ua(true, "mobile").contains("X11; Linux x86_64"))
        assertEquals("mobile", BrowserUa.ua(false, "mobile"))
    }
}
