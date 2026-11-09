package com.techkadak.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.techkadak.app.ui.MainViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config as RConfig
import java.io.File

/**
 * v1.2.1: launch regression tests. v1.2.0 crashed on every open because MainViewModel's init ran before the
 * v1.2 fields existed (NPE in setStatsOn). These tests build the REAL ViewModel and the REAL MainActivity.
 */
@RunWith(RobolectricTestRunner::class)
@RConfig(sdk = [33], application = android.app.Application::class)
class LaunchTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun mainViewModelConstructs() {
        val vm = MainViewModel(app)
        assertTrue(vm.statsOn)
        assertNotNull(vm.kits)
        assertNotNull(vm.videos)
    }

    @Test fun mainActivityLaunches() {
        val ctl = Robolectric.buildActivity(MainActivity::class.java).setup()
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertFalse(ctl.get().isFinishing)
        ctl.pause().stop().destroy()
    }

    @Test fun integrityFingerprintMatchesReleaseCert() {
        assertEquals("d60520463444ea2207a09b40ce922ee01d4c0009c115abda6f563cdce66c43eb", Integrity.expectedHex())
        // debug build: check is off, so dev/test runs are never blocked
        assertEquals(Integrity.Result.OK, Integrity.check(app))
    }

    @Test fun crashLogSavesAndShowsOnce() {
        CrashLog.clear(app)
        assertNull(CrashLog.pending(app))
        CrashLog.save(app, Thread.currentThread(), IllegalStateException("boom test"))
        val txt = CrashLog.pending(app)!!
        assertTrue(txt.contains("boom test"))
        assertTrue(txt.contains(BuildConfig.VERSION_NAME))
        CrashLog.clear(app)
        assertNull(CrashLog.pending(app))
    }

    /** Guard against the exact v1.2.0 bug coming back: the init that starts work must be the LAST member. */
    @Test fun startInitIsLastInViewModel() {
        val src = File("src/main/java/com/techkadak/app/ui/MainViewModel.kt").readText().trimEnd()
        val lastInit = src.lastIndexOf("init {")
        assertTrue(lastInit > 0)
        val after = src.substring(lastInit)
        assertTrue("start(app) must be in the last init", after.contains("start(app)"))
        assertFalse("no property may be declared after the start init", Regex("\\n    (var|val|private val|private var) ").containsMatchIn(after))
        assertEquals("only one init block", 1, Regex("\\binit \\{").findAll(src).count())
    }
}
