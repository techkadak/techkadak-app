package com.techkadak.app

import com.techkadak.app.stats.DeviceRecord
import org.junit.Assert.*
import org.junit.Test

class V15DeviceRecordTest {
    private fun doc(apps: Map<String, String> = emptyMap(), name: String? = null, model: String = "Oppo CPH2371") =
        DeviceRecord.buildDoc("2026-11-05", "2026-11-06", 10500, "1.5.0", 33, "google", model, "13", apps, name)

    @Test fun onlyAllowedKeys() {
        val d = doc(mapOf("newpipe" to "i"), "Rohit")
        assertTrue(d.keys.all { it in setOf("first", "last", "v", "vn", "sdk", "lt", "model", "os", "apps", "nm") })
        assertEquals("Oppo CPH2371", d["model"]); assertEquals("13", d["os"]); assertEquals("Rohit", d["nm"])
    }

    @Test fun anonymousWhenNoName() {
        val d = doc()
        assertFalse(d.containsKey("nm")); assertFalse(d.containsKey("apps"))
        assertEquals("Anonymous device #ab12", DeviceRecord.label("ab12cd34ef56ab12cd34ef56ab12cd34", null))
        assertEquals("Anonymous device #ab12", DeviceRecord.label("ab12cd34ef56ab12cd34ef56ab12cd34", "  "))
        assertEquals("Rohit", DeviceRecord.label("ab12", "Rohit"))
        assertFalse(doc(name = "   ").containsKey("nm"))
    }

    @Test fun trimsAndCleans() {
        val d = doc(name = "A".repeat(100) + "\n\t", model = "X\u0000Y".repeat(30))
        assertTrue((d["nm"] as String).length <= 40); assertTrue((d["model"] as String).length <= 40)
        assertFalse((d["model"] as String).contains('\u0000'))
    }

    @Test fun appsOnlyInstalledOrOpenedAndCapped() {
        var m = emptyMap<String, String>()
        m = DeviceRecord.mark(m, "a", "o"); assertEquals("o", m["a"])
        m = DeviceRecord.mark(m, "a", "i"); assertEquals("i", m["a"])     // installed beats opened
        m = DeviceRecord.mark(m, "a", "o"); assertEquals("i", m["a"])     // never downgraded
        assertEquals(m, DeviceRecord.mark(m, "b", "x"))                    // unknown kind ignored
        assertEquals(m, DeviceRecord.mark(m, "", "o"))
        for (i in 1..100) m = DeviceRecord.mark(m, "app$i", "o")
        assertEquals(DeviceRecord.MAX_APPS, m.size)
        assertEquals(DeviceRecord.MAX_APPS, (doc(m)["apps"] as Map<*, *>).size)
        // junk values never leave the device
        assertFalse(doc(mapOf("zzz" to "secret")).containsKey("apps"))
    }
}
