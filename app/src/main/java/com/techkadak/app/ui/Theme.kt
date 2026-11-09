package com.techkadak.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.techkadak.app.R
import com.techkadak.app.data.CardStyle
import com.techkadak.app.data.ThemeMode
import com.techkadak.app.data.UserSettings

/** Brand ka default: LIME + BLACK + WHITE. User Settings me theme / colour badal sakta hai. */
data class Palette(
    val mode: ThemeMode,
    val accent: Color,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val dim: Color,
) {
    val isLight get() = mode == ThemeMode.LIGHT
    val isGlass get() = mode == ThemeMode.GLASS
    /** Light theme me bahut halka accent padhne me nahi aata — thoda gehra kar dete hain. */
    val ink: Color = if (isLight && accent.luminance() > 0.35f) lerp(accent, Color.Black, 0.38f) else accent
    val onAccent: Color = if (ink.luminance() > 0.4f) Color(0xFF0A0A0A) else Color.White

    companion object {
        val LIME = Color(0xFF9BFF2E)
        fun of(mode: ThemeMode, accent: Color): Palette = when (mode) {
            ThemeMode.AMOLED -> Palette(mode, accent, Color(0xFF000000), Color(0xFF0E0E0E), Color(0xFF1A1A1A), Color(0xFF282828), Color.White, Color(0xFFA3A3A3), Color(0xFF6B6B6B))
            ThemeMode.DARK -> Palette(mode, accent, Color(0xFF141614), Color(0xFF1D201D), Color(0xFF272A27), Color(0xFF353935), Color.White, Color(0xFFADB2AD), Color(0xFF7A7F7A))
            ThemeMode.LIGHT -> Palette(mode, accent, Color(0xFFF5F6F1), Color(0xFFFFFFFF), Color(0xFFEAECE4), Color(0xFFD6D9CE), Color(0xFF0A0A0A), Color(0xFF52574D), Color(0xFF878C80))
            // Liquid Glass: background transparent (peeche animated wallpaper), surfaces = frosted white tint, rim = bright line
            ThemeMode.GLASS -> Palette(mode, accent, Color.Transparent, Color(0x1FFFFFFF), Color(0x2BFFFFFF), Color(0x3DFFFFFF), Color.White, Color(0xD9FFFFFF), Color(0x99FFFFFF))
        }
    }
}

/** Saare screens ye tokens use karte hain (purane naam rakhe: Black = background, White = text). */
object Tk {
    var palette by mutableStateOf(Palette.of(ThemeMode.AMOLED, Palette.LIME))
    var english by mutableStateOf(false)
    var cardStyle by mutableStateOf(CardStyle.GRID)
    var refreshAnim by mutableStateOf(true)

    val Lime get() = palette.ink
    val LimeDeep get() = lerp(palette.ink, Color.Black, 0.22f)
    val LimeLight get() = lerp(palette.ink, Color.White, 0.25f)
    val LimeSoft get() = palette.ink.copy(alpha = if (palette.isLight) 0.14f else 0.15f)
    val LimeBorder get() = palette.ink.copy(alpha = 0.35f)
    val Glow get() = palette.ink.copy(alpha = if (palette.isLight) 0.25f else 0.40f)
    val OnLime get() = palette.onAccent
    val OnLimeMuted get() = palette.onAccent.copy(alpha = 0.7f)
    val Black get() = palette.bg
    val Surface get() = palette.surface
    val Surface2 get() = palette.surface2
    val Line get() = palette.line
    val White get() = palette.text
    val TextSoft get() = palette.text.copy(alpha = 0.82f)
    val Muted get() = palette.muted
    val Dim get() = palette.dim
    val Shimmer get() = lerp(palette.surface2, palette.text, 0.07f)
    /** Hero/featured card ka gradient start: background me thoda accent. */
    val HeroStart get() = palette.ink.copy(alpha = if (palette.isLight) 0.22f else 0.16f).compositeOver(palette.surface)
    val isLight get() = palette.isLight
    val isGlass get() = palette.isGlass
    /** Dialog / sheet ka background — glass me bhi padhne layak (almost opaque). */
    val Sheet get() = if (palette.isGlass) Color(0xF2121A14) else palette.surface
}

/** Hinglish / English: tr("Hinglish", "English"). */
fun tr(hi: String, en: String): String = if (Tk.english) en else hi

val Display = FontFamily(Font(R.font.unbounded_900, FontWeight.Black), Font(R.font.unbounded_700, FontWeight.Bold))
val Body = FontFamily(Font(R.font.dmsans_400, FontWeight.Normal), Font(R.font.dmsans_700, FontWeight.Bold))

private val type = Typography(
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 27.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 23.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 11.sp),
)

val LocalSettings = staticCompositionLocalOf { UserSettings() }

/** Settings ke hisaab se poora theme: colours, font size, card style, language. */
@Composable
fun TkTheme(settings: UserSettings = UserSettings(), remoteAccent: Color? = null, content: @Composable () -> Unit) {
    val accent = settings.accent?.let { Color(it) } ?: remoteAccent ?: Palette.LIME
    val p = Palette.of(settings.theme, accent)
    Tk.palette = p
    Tk.english = settings.english
    Tk.cardStyle = settings.cardStyle
    Tk.refreshAnim = settings.refreshAnim
    val scheme = if (p.isGlass) darkColorScheme(
        primary = p.ink, onPrimary = p.onAccent, secondary = p.ink, onSecondary = p.onAccent,
        background = Color(0xFF0B100D), onBackground = p.text, surface = Color(0xFF111813), onSurface = p.text,
        surfaceVariant = Color(0xFF1C241E), onSurfaceVariant = p.muted,
        surfaceContainer = Color(0xF2121A14), surfaceContainerHigh = Color(0xF2172019), surfaceContainerHighest = Color(0xF21C261F),
        surfaceContainerLow = Color(0xF2101612), surfaceContainerLowest = Color(0xFF0B100D),
        outline = p.line, outlineVariant = p.line,
        secondaryContainer = Tk.LimeSoft, onSecondaryContainer = p.ink,
        primaryContainer = p.ink, onPrimaryContainer = p.onAccent, error = Color(0xFFFF5A4E),
    ) else if (p.isLight) lightColorScheme(
        primary = p.ink, onPrimary = p.onAccent, secondary = p.ink, onSecondary = p.onAccent,
        background = p.bg, onBackground = p.text, surface = p.bg, onSurface = p.text,
        surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
        surfaceContainer = p.surface, surfaceContainerHigh = p.surface2, surfaceContainerHighest = p.surface2,
        surfaceContainerLow = p.surface, surfaceContainerLowest = p.bg,
        outline = p.line, outlineVariant = p.line,
        secondaryContainer = Tk.LimeSoft, onSecondaryContainer = p.ink,
        primaryContainer = p.ink, onPrimaryContainer = p.onAccent, error = Color(0xFFD93025),
    ) else darkColorScheme(
        primary = p.ink, onPrimary = p.onAccent, secondary = p.ink, onSecondary = p.onAccent,
        background = p.bg, onBackground = p.text, surface = p.bg, onSurface = p.text,
        surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
        surfaceContainer = p.surface, surfaceContainerHigh = p.surface2, surfaceContainerHighest = p.surface2,
        surfaceContainerLow = p.surface, surfaceContainerLowest = p.bg,
        outline = p.line, outlineVariant = p.line,
        secondaryContainer = Tk.LimeSoft, onSecondaryContainer = p.ink,
        primaryContainer = p.ink, onPrimaryContainer = p.onAccent, error = Color(0xFFFF5A4E),
    )
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(d.density, d.fontScale * settings.fontScale),
        LocalSettings provides settings,
    ) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}

/** Accent presets (Settings me). Pehla = brand lime. */
val AccentPresets: List<Pair<String, Long>> = listOf(
    "Lime" to 0xFF9BFF2E, "Neon" to 0xFFC6FF00, "Mint" to 0xFF2EFFB0, "Orange" to 0xFFFF8A1F,
    "Yellow" to 0xFFFFD60A, "Red" to 0xFFFF4D4D, "Gold" to 0xFFE8B931, "White" to 0xFFF2F2F2,
)
