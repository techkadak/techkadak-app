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

/** v1.8: 30 look theme — colours palette ke 7 slots me (bg, surface, surface2, line, text, muted, dim). */
private fun look(mode: ThemeMode, accent: Color, bg: Long, surface: Long, surface2: Long, line: Long, text: Long, muted: Long, dim: Long) =
    Palette(mode, accent, Color(bg.toInt()), Color(surface.toInt()), Color(surface2.toInt()), Color(line.toInt()), Color(text.toInt()), Color(muted.toInt()), Color(dim.toInt()))

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
    val isLight get() = mode.isLightLook
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
            ThemeMode.GLOSSY_DARK -> look(mode, accent, 0xFF000000, 0xFF14181D, 0xFF1E242B, 0x66C0C0C0, 0xFFFFFFFF, 0xFFC0C6CC, 0xFF7F8790)
            ThemeMode.IOS_LIGHT -> look(mode, accent, 0xFFFFFFFF, 0xFFF7F7F7, 0xFFD3D3D3, 0xFFD3D3D3, 0xFF000000, 0xFF3C3C43, 0xFF8E8E93)
            ThemeMode.IOS_DARK -> look(mode, accent, 0xFF1C1C1E, 0xFF2C2C2E, 0xFF2C2C2E, 0xFF2C2C2E, 0xFFFFFFFF, 0xFF8E8E93, 0xFF636366)
            ThemeMode.MATTE_BLACK -> look(mode, accent, 0xFF000000, 0xFF36454F, 0xFF36454F, 0xFF36454F, 0xFFFFFFFF, 0xFFC8C8C8, 0xFF8A8A8A)
            ThemeMode.MATTE_WHITE -> look(mode, accent, 0xFFF7F7F2, 0xFFE5E5E5, 0xFFE5E5E5, 0xFFD0D0D0, 0xFF000000, 0xFF4A4A4A, 0xFF7A7A7A)
            ThemeMode.GLOSSY_PURPLE -> look(mode, accent, 0xFF4B0082, 0xFF3E006B, 0xFF5E1A8C, 0x88FF6EC7, 0xFFFFFFFF, 0xFFF5D0FF, 0xFFC9A6E0)
            ThemeMode.LIQUID_GLASS -> look(mode, accent, 0xFF0B1620, 0x1FF0F8FF, 0x33F0F8FF, 0x66F0F8FF, 0xFFFFFFFF, 0xD9F0F8FF, 0x99F0F8FF)
            ThemeMode.NEON_GLOW -> look(mode, accent, 0xFF000000, 0xFF0A0F12, 0x2600E5FF, 0x8800E5FF, 0xFFFFFFFF, 0xFF8FF3FF, 0xFFFF2BD6)
            ThemeMode.AURORA_GRADIENT -> look(mode, accent, 0xFF1A0B2E, 0xFF2A1248, 0x6600FFFF, 0x88FFC0CB, 0xFFFFFFFF, 0xFF00FFFF, 0xFFFFC0CB)
            ThemeMode.SUNSET_GRADIENT -> look(mode, accent, 0xFF3A0E0E, 0xFF5C1A12, 0x66FF4500, 0x88FFA500, 0xFFFFF4E8, 0xFFFFC0CB, 0xFFFF4D4D)
            ThemeMode.OCEAN_BLUE -> look(mode, accent, 0xFF06224A, 0xFF0B3D91, 0xFF0E4F9F, 0x6600FFFF, 0xFFFFFFFF, 0xFF9FEFFF, 0xFF00FFFF)
            ThemeMode.FOREST_GREEN -> look(mode, accent, 0xFF013220, 0xFF0C4A2E, 0xFF1F6B45, 0x9998FF98, 0xFFF5F5DC, 0xFFF5F5DC, 0xFF98FF98)
            ThemeMode.ROYAL_GOLD -> look(mode, accent, 0xFF000000, 0xFF1A1608, 0xFF2A2410, 0xFFD4AF37, 0xFFFFFDD0, 0xFFD4AF37, 0xFF9C8145)
            ThemeMode.ROSE_PASTEL -> look(mode, accent, 0xFFFFFFFF, 0xFFFFF0F5, 0xFFE6E6FA, 0xFFC8B6E2, 0xFF000000, 0xFF5B4B63, 0xFF8E7D96)
            ThemeMode.MINT_FRESH -> look(mode, accent, 0xFFFFFFFF, 0xFFF0FFF8, 0xFF98FF98, 0x66008080, 0xFF0B2E2A, 0xFF2F5E58, 0xFF6B8F8A)
            ThemeMode.PEACH_SOFT -> look(mode, accent, 0xFFFFF5EE, 0xFFFFDAB9, 0xFFFFDAB9, 0x66FF7F50, 0xFF3A1F14, 0xFF7A4A3A, 0xFFB08070)
            ThemeMode.CYBERPUNK -> look(mode, accent, 0xFF000000, 0xFF0A0A14, 0xFF1A0A1F, 0x8800F0FF, 0xFFFFFFFF, 0xFF00F0FF, 0xFFFF10F0)
            ThemeMode.RETRO_TERMINAL -> look(mode, accent, 0xFF000000, 0xFF0A120A, 0xFF0F1F0F, 0x6639FF14, 0xFF39FF14, 0xFF2BB30F, 0xFF1E7A0C)
            ThemeMode.PAPER_INK -> look(mode, accent, 0xFFFFFDF0, 0xFFF2EEDC, 0xFFEFE9D6, 0x33000000, 0xFF000000, 0xFF333333, 0xFFFF0000)
            ThemeMode.EARTHY_NATURE -> look(mode, accent, 0xFFF5F0E1, 0xFFE8DCC4, 0xFFD9CBAE, 0x998B4513, 0xFF1F3A22, 0xFF5C4A32, 0xFF8B6B4A)
            ThemeMode.CARBON_FIBER -> look(mode, accent, 0xFF0A0A0A, 0xFF2B2B2B, 0xFF333333, 0x55C0C0C0, 0xFFFFFFFF, 0xFFC0C0C0, 0xFF808080)
            ThemeMode.MIDNIGHT_BLUE -> look(mode, accent, 0xFF0A0A3D, 0xFF00008B, 0xFF1A1A6E, 0x44F8F8FF, 0xFFF8F8FF, 0xFFB8BCD8, 0xFF7880A8)
            ThemeMode.CHERRY_RED -> look(mode, accent, 0xFF000000, 0xFF2A0000, 0xFF8B0000, 0x44FFFFFF, 0xFFFFFFFF, 0xFFFFD6D6, 0xFF9E5B5B)
            ThemeMode.LAVENDER_DREAM -> look(mode, accent, 0xFFFFF8FF, 0xFFE6E6FA, 0xFFC8A2C8, 0xFFC8A2C8, 0xFF2E1A3A, 0xFF5E4A6B, 0xFF8E7AA0)
            ThemeMode.MONOCHROME -> look(mode, accent, 0xFF000000, 0xFF1A1A1A, 0xFF2E2E2E, 0x66808080, 0xFFFFFFFF, 0xFFBFBFBF, 0xFF808080)
            ThemeMode.HIGH_CONTRAST -> look(mode, accent, 0xFF000000, 0xFF111111, 0xFF1A1A1A, 0xFFFFD400, 0xFFFFFFFF, 0xFFFFD400, 0xFFFFD400)
            ThemeMode.SAKURA_PINK -> look(mode, accent, 0xFFFFFFFF, 0xFFFFE4EC, 0xFFFFB7C5, 0x66DE3163, 0xFF3A0F1E, 0xFF6B3A4B, 0xFFA0687A)
            ThemeMode.ARCTIC_FROST -> look(mode, accent, 0xFFF0F8FF, 0xFFFFFFFF, 0xFFC0C0C0, 0xFFC0C0C0, 0xFF0B1F2E, 0xFF3E5566, 0xFF7D93A3)
            ThemeMode.COFFEE_BROWN -> look(mode, accent, 0xFF1C1210, 0xFF2E1E16, 0xFF4A3228, 0x55FFFDD0, 0xFFFFFDD0, 0xFFD8C3A5, 0xFF8C6A4F)
            ThemeMode.EMERALD_LUXE -> look(mode, accent, 0xFF02140D, 0xFF0B2E20, 0x3350C878, 0x88D4AF37, 0xFFFFFFFF, 0xFFD4AF37, 0xFF50C878)
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
