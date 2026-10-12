package veil.raven.klangdroid.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import veil.raven.klangdroid.data.Farbwerte

data class KlangFarben(
    val bg: Color,
    val flaeche: Color,
    val flaeche2: Color,
    val rand: Color,
    val text: Color,
    val schwach: Color,
    val akzent: Color,
    val akzent2: Color,
)

fun Color.mischen(andere: Color, anteilAndere: Float): Color {
    val t = anteilAndere.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - t) + andere.red * t,
        green = green * (1f - t) + andere.green * t,
        blue = blue * (1f - t) + andere.blue * t,
        alpha = 1f,
    )
}

fun hexZuFarbe(hex: String?): Color? {
    val h = hex?.trim()?.removePrefix("#") ?: return null
    val wert = h.toLongOrNull(16) ?: return null
    return when (h.length) {
        6 -> Color((0xFF000000L or wert).toInt())
        8 -> Color(wert.toInt())
        else -> null
    }
}

private val Tinte = Color(0xFF0C0C0D)
private val Tinte2 = Color(0xFF141416)

private fun ausAkzent(akzent: Color, zweit: Color): KlangFarben {
    val bg = Tinte.mischen(akzent, 0.10f)
    val flaeche = Tinte2.mischen(akzent, 0.16f)
    return KlangFarben(
        bg = bg,
        flaeche = flaeche,
        flaeche2 = flaeche.mischen(akzent, 0.20f),
        rand = flaeche.mischen(akzent, 0.42f),
        text = Color(0xFFF3F1F6),
        schwach = Color(0xFFB7B3C0).mischen(akzent, 0.22f),
        akzent = akzent,
        akzent2 = zweit,
    )
}

object Paletten {
    val rabe = KlangFarben(
        bg = Color(0xFF0D0A12),
        flaeche = Color(0xFF191323),
        flaeche2 = Color(0xFF2A1C3A),
        rand = Color(0xFF4A3270),
        text = Color(0xFFF6F0FA),
        schwach = Color(0xFFB7A4C9),
        akzent = Color(0xFFB44DFF),
        akzent2 = Color(0xFF8A63C8),
    )
    val haus = KlangFarben(
        bg = Color(0xFF0C0C0D),
        flaeche = Color(0xFF161618),
        flaeche2 = Color(0xFF2C221C),
        rand = Color(0xFF4A342C),
        text = Color(0xFFECECED),
        schwach = Color(0xFF8B8B90),
        akzent = Color(0xFFF97B14),
        akzent2 = Color(0xFFE31C79),
    )
    val unbunt = KlangFarben(
        bg = Color(0xFF0C0C0D),
        flaeche = Color(0xFF161618),
        flaeche2 = Color(0xFF242427),
        rand = Color(0xFF333336),
        text = Color(0xFFECECED),
        schwach = Color(0xFF8B8B90),
        akzent = Color(0xFFC9C9CD),
        akzent2 = Color(0xFF8B8B90),
    )
    val nebel = ausAkzent(Color(0xFFE0455A), Color(0xFF2EC4B6))
    val eis = ausAkzent(Color(0xFF6FD3FF), Color(0xFF2EC4B6))
    val polar = ausAkzent(Color(0xFF45F0A0), Color(0xFF3AA8FF))
    val mond = ausAkzent(Color(0xFF9FF3FF), Color(0xFFEFE0C2))
    val glut = ausAkzent(Color(0xFFFF5A48), Color(0xFFFFB03A))
    val borg = ausAkzent(Color(0xFF39FF6A), Color(0xFF18B85C))
    val coverVorschau = ausAkzent(Color(0xFFAD7357), Color(0xFFE7C2A2))
}

data class ThemaWahl(val id: String, val name: String, val satz: String, val probe: KlangFarben)

val THEMEN = listOf(
    ThemaWahl("rabe", "Rabe", "Violett über allem", Paletten.rabe),
    ThemaWahl("haus", "Haus", "Orange nach Magenta", Paletten.haus),
    ThemaWahl("cover", "Cover", "Farbe des laufenden Stücks", Paletten.coverVorschau),
    ThemaWahl("unbunt", "Unbunt", "Graustufen, ruhig", Paletten.unbunt),
    ThemaWahl("nebel", "Nebel", "H-alpha und O-III", Paletten.nebel),
    ThemaWahl("eis", "Eisnebel", "Kaltes Cyan", Paletten.eis),
    ThemaWahl("polar", "Polarlicht", "Grün und Blau", Paletten.polar),
    ThemaWahl("mond", "Mondlicht", "Hellcyan und Beige", Paletten.mond),
    ThemaWahl("glut", "Sonnenglut", "Rot und Gold", Paletten.glut),
    ThemaWahl("borg", "Borg", "Grün der Musik-Matrix", Paletten.borg),
)

val AKZENT_WAHL = listOf(
    "Automatisch" to "",
    "Rabe" to "#B44DFF",
    "Orange" to "#F97B14",
    "Magenta" to "#E31C79",
    "H-alpha" to "#E0455A",
    "Eis" to "#6FD3FF",
    "Polar" to "#45F0A0",
    "Glut" to "#FFB03A",
    "Borg" to "#39FF6A",
    "Mond" to "#9FF3FF",
)

fun themaFarben(id: String, cover: Farbwerte?): KlangFarben = when (id) {
    "haus" -> Paletten.haus
    "unbunt" -> Paletten.unbunt
    "cover" -> coverFarben(cover)
    "nebel" -> Paletten.nebel
    "eis" -> Paletten.eis
    "polar" -> Paletten.polar
    "mond" -> Paletten.mond
    "glut" -> Paletten.glut
    "borg" -> Paletten.borg
    else -> Paletten.rabe
}

fun coverFarben(cover: Farbwerte?): KlangFarben {
    val akzent = hexZuFarbe(cover?.akzent) ?: return Paletten.unbunt
    val akzent2 = hexZuFarbe(cover?.akzent2) ?: akzent
    val bg = hexZuFarbe(cover?.grund) ?: Paletten.unbunt.bg
    val flaeche = hexZuFarbe(cover?.flaeche) ?: bg.mischen(Color.White, 0.06f)
    val text = hexZuFarbe(cover?.text) ?: Color(0xFFECECED)
    val leise = hexZuFarbe(cover?.textLeise) ?: text.mischen(bg, 0.35f)
    return KlangFarben(
        bg = bg,
        flaeche = flaeche,
        flaeche2 = flaeche.mischen(akzent, 0.18f),
        rand = flaeche.mischen(akzent, 0.38f),
        text = text,
        schwach = leise,
        akzent = akzent,
        akzent2 = akzent2,
    )
}

fun mitAkzent(basis: KlangFarben, hex: String): KlangFarben {
    val akzent = hexZuFarbe(hex) ?: return basis
    val zweit = akzent.mischen(Color.Black, 0.28f)
    return basis.copy(
        akzent = akzent,
        akzent2 = zweit,
        flaeche2 = basis.flaeche.mischen(akzent, 0.18f),
        rand = basis.flaeche.mischen(akzent, 0.40f),
    )
}

private val Typografie = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
)

@Composable
fun animierteFarben(ziel: KlangFarben): KlangFarben {
    val spec = tween<Color>(800)
    val bg by animateColorAsState(ziel.bg, spec, label = "bg")
    val flaeche by animateColorAsState(ziel.flaeche, spec, label = "flaeche")
    val flaeche2 by animateColorAsState(ziel.flaeche2, spec, label = "flaeche2")
    val rand by animateColorAsState(ziel.rand, spec, label = "rand")
    val text by animateColorAsState(ziel.text, spec, label = "text")
    val schwach by animateColorAsState(ziel.schwach, spec, label = "schwach")
    val akzent by animateColorAsState(ziel.akzent, spec, label = "akzent")
    val akzent2 by animateColorAsState(ziel.akzent2, spec, label = "akzent2")
    return KlangFarben(bg, flaeche, flaeche2, rand, text, schwach, akzent, akzent2)
}

@Composable
fun KlangThema(farben: KlangFarben, content: @Composable () -> Unit) {
    val aufAkzent = if (farben.akzent.luminance() > 0.62f) Color(0xFF16141A) else Color(0xFFF8F4FC)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = farben.akzent,
            onPrimary = aufAkzent,
            secondary = farben.akzent2,
            onSecondary = aufAkzent,
            background = farben.bg,
            onBackground = farben.text,
            surface = farben.flaeche,
            onSurface = farben.text,
            surfaceVariant = farben.flaeche2,
            onSurfaceVariant = farben.schwach,
            outline = farben.rand,
        ),
        typography = Typografie,
        content = content,
    )
}
