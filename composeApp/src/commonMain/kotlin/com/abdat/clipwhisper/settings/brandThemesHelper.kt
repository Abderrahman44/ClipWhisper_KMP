package com.abdat.clipwhisper.settings

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

// ---- mean merge (ARGB average) ----
fun meanColor( colors: Array<Color>): Color {
    require(colors.isNotEmpty())
    val n = colors.size

    var aSum = 0
    var rSum = 0
    var gSum = 0
    var bSum = 0

    for (c in colors) {
        val argb = c.toArgb()
        aSum += (argb ushr 24) and 0xFF
        rSum += (argb ushr 16) and 0xFF
        gSum += (argb ushr 8) and 0xFF
        bSum += (argb) and 0xFF
    }

    val a = (aSum / n) and 0xFF
    val r = (rSum / n) and 0xFF
    val g = (gSum / n) and 0xFF
    val b = (bSum / n) and 0xFF

    return Color((a shl 24) or (r shl 16) or (g shl 8) or b)
}

private fun Color.toThemeLong(): Long = this.toArgb().toLong()
private fun Long.toColor(): Color = Color(this.toInt())


@Immutable
data class ThemePreset(
    val name: String,
    val seed: Color,        // stored in settings as Long
    val previewBrush: Brush? // null => solid
)

object BrandThemePresets {

    val jetbrainsPurple = Color(0xFF7F52FF)
    val jetbrainsMagenta = Color(0xFFC711E1)
    val jetbrainsOrange = Color(0xFFE64A19)

    val JetBrainsGradient = Brush.linearGradient(
        colors = listOf(jetbrainsPurple, jetbrainsMagenta, jetbrainsOrange)
    )
    val JetBrainsSeed = meanColor(listOf(jetbrainsPurple, jetbrainsMagenta, jetbrainsOrange).toTypedArray())

    val kotlinConfRose = Color(0xFFE24462)
    val kotlinConfPurple = Color(0xFFB125EA)
    val kotlinConfIndigo = Color(0xFF7F52FF)

    val KotlinConfGradient = Brush.linearGradient(
        colors = listOf(kotlinConfRose, kotlinConfPurple, kotlinConfIndigo)
    )
    val KotlinConfSeed = meanColor(listOf(kotlinConfRose, kotlinConfPurple, kotlinConfIndigo).toTypedArray())

   val composePurple = Color(0xFF6750A4)
    val composeBlue = Color(0xFF3B82F6)
    val composeTeal = Color(0xFF03DAC6)

    val ComposeMpGradient = Brush.linearGradient(
        colors = listOf(composePurple, composeBlue, composeTeal)
    )
    val ComposeMpSeed = meanColor( listOf(composePurple, composeBlue, composeTeal).toTypedArray())

    val presets: List<ThemePreset> = listOf(
        ThemePreset("JetBrains", JetBrainsSeed, JetBrainsGradient),
        ThemePreset("KotlinConf", KotlinConfSeed, KotlinConfGradient),
        ThemePreset("Compose MP", ComposeMpSeed, ComposeMpGradient),

        ThemePreset("Rose", Color(0xFFE24462), null),
        ThemePreset("Purple", Color(0xFFB125EA), null),
        ThemePreset("Purple Blue", Color(0xFF7F52FF), null),
        ThemePreset("Magenta", Color(0xFFE627F8), null),
        ThemePreset("Red", Color(0xFFFF0007), null),
        ThemePreset("Orange", Color(0xFFFF841B), null),
        ThemePreset("Royal Blue", Color(0xFF674FEB), null),
        ThemePreset("Light Purple", Color(0xFFCE67FF), null),
        ThemePreset("Green", Color(0xFF48E155), null),
        ThemePreset("Material Purple", Color(0xFF6200EE), null),
        ThemePreset("Teal", Color(0xFF03DAC6), null),
    )

    fun findByStoredLong(themeColor: Long): ThemePreset? =
        presets.firstOrNull { it.seed.toThemeLong() == themeColor }

    fun brushForStoredLong(themeColor: Long): Brush {
        val preset = findByStoredLong(themeColor)
        return preset?.previewBrush ?: SolidColor(themeColor.toColor())
    }

    fun seedForStoredLong(themeColor: Long): Color =
        findByStoredLong(themeColor)?.seed ?: themeColor.toColor()

    fun onTopBarColor(themeColor: Long): Color {
        val seed = seedForStoredLong(themeColor)
        return if (seed.luminance() < 0.45f) Color.White else Color.Black
    }
}
