package com.techkadak.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.techkadak.app.data.UserSettings
import com.techkadak.app.ui.*
import com.techkadak.app.update.DlState
import com.techkadak.app.update.Release
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config as RConfig
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** v1.5 screenshots: opening animation frames, About QR card, Support card, credit line. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RConfig(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi", application = android.app.Application::class)
class V15ScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private val out = System.getProperty("tk.shotsDir") ?: "build/screens"

    private fun shot(name: String, body: @Composable () -> Unit) {
        rule.setContent {
            TkTheme(UserSettings()) {
                Box(Modifier.fillMaxSize().background(Tk.Black), contentAlignment = Alignment.Center) { body() }
            }
        }
        rule.waitForIdle()
        captureScreenRoboImage("$out/$name.png")
    }

    @Test fun s01_splash_0_10() = shot("v15-splash-01") { OpeningLayer(0.10f) {} }
    @Test fun s02_splash_0_35() = shot("v15-splash-02") { OpeningLayer(0.35f) {} }
    @Test fun s03_splash_0_55() = shot("v15-splash-03") { OpeningLayer(0.55f) {} }
    @Test fun s04_splash_0_70() = shot("v15-splash-04") { OpeningLayer(0.70f) {} }
    @Test fun s05_splash_end() = shot("v15-splash-05") { OpeningLayer(1.00f) {} }

    @Test fun q01_about_qr_card() = shot("v15-about-qr-card") {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ReleaseQrCard(Modifier.padding(horizontal = 6.dp))
            Spacer(Modifier.height(18.dp))
            SupportCard(Modifier.padding(horizontal = 6.dp))
            Spacer(Modifier.height(18.dp))
            CreditLine()
        }
    }

    private val rel = Release("1.5.0", "Opening logo intro + Rs 50 support card\n- About QR code\n- Animated update card\n- Welcome member card\n- In-app browser", "https://example.invalid/app.apk", 6_400_000L, "https://github.com/techkadak/techkadak-app/releases/latest")

    @Test fun u01_update_idle() = shot("v15-update-01-idle") { UpdateDialog(rel, DlState.Idle, { true }, {}, {}, {}, {}, {}) }
    @Test fun u02_update_running() = shot("v15-update-02-downloading") { UpdateDialog(rel, DlState.Running(0.42f), { true }, {}, {}, {}, {}, {}) }
    @Test fun u03_update_done() = shot("v15-update-03-done") { UpdateDialog(rel, DlState.Done(File("/tmp/none.apk")), { true }, {}, {}, {}, {}, {}) }
    @Test fun u04_update_failed() = shot("v15-update-04-failed") { UpdateDialog(rel, DlState.Failed("Network nahi mila"), { true }, {}, {}, {}, {}, {}) }

    @Test fun notes_are_plain_short_lines() {
        val lines = updateNoteLines("**Bold** line\n- item two\n\n• item three\n`code`")
        assertEquals(listOf("Bold line", "item two", "item three", "code"), lines)
        assertEquals(6, updateNoteLines((1..9).joinToString("\n") { "n$it" }).size)
    }

    @Test fun w01_member_card_front() = shot("v15-member-front-n42") { WelcomeMemberCard("Rohit", 42L, "05 Nov 2026", {}, autoFlip = false) }
    @Test fun w03_member_card_other_colours() = shot("v15-member-colours") {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row { listOf(1L, 2L, 3L).forEach { Box(Modifier.size(96.dp).padding(3.dp)) { MemberMini(it) } } }
            Row { listOf(7L, 19L, 500L).forEach { Box(Modifier.size(96.dp).padding(3.dp)) { MemberMini(it) } } }
        }
    }
}

@Composable
private fun MemberMini(n: Long) {
    val s = com.techkadak.app.member.MemberColor.of(n)
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(s.colorA), androidx.compose.ui.graphics.Color(s.colorB)))), contentAlignment = Alignment.Center) {
        Text("#$n", color = androidx.compose.ui.graphics.Color(s.textOn))
    }
}
