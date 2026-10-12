package veil.raven.klangdroid.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import coil3.BitmapImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size as CoilSize
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import veil.raven.klangdroid.KlangModell
import veil.raven.klangdroid.data.KarteAntwort
import veil.raven.klangdroid.data.Lied
import veil.raven.klangdroid.data.bildDatei
import veil.raven.klangdroid.data.medienUrl
import veil.raven.klangdroid.data.tonDatei
import veil.raven.klangdroid.schaum.SchaumBild
import veil.raven.klangdroid.schaum.SchaumGliederung
import veil.raven.klangdroid.schaum.SchaumMass
import veil.raven.klangdroid.schaum.SchaumRechner
import veil.raven.klangdroid.schaum.SchaumZelle
import veil.raven.klangdroid.schaum.gliederungVon
import veil.raven.klangdroid.schaum.schaumAuftrag
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SchaumAnsicht(
    modell: KlangModell,
    farben: KlangFarben,
    spielId: String?,
    tonLaeuft: Boolean,
    modifier: Modifier = Modifier,
) {
    val index = modell.bestand.index
    // Nur Titel mit einer lokalen Datei. Der Rest liegt nur bei Suno,
    // und ein Gefäß aus hunderten solcher Zellen legt der Schaum minutenlang.
    val ton = modell.werte.value.ton
    val lieder = index?.songs.orEmpty().filter { tonDatei(it, modell.bestand.eigen[it.id], ton) != null }
    val karte = modell.bestand.karte
    val mass = modell.schaumMass
    val art = modell.schaumGliederung
    val thema = modell.werte.value.thema
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(mass.erklaerung, color = farben.schwach, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Zeile(farben) {
            SchaumMass.entries.forEach { m ->
                WahlChip(m.label, mass == m, farben) {
                    modell.schaumMass = m
                    modell.schaumZoom = null
                }
            }
        }
        Zeile(farben) {
            SchaumGliederung.entries.forEach { g ->
                WahlChip(g.label, art == g, farben) {
                    modell.schaumGliederung = g
                    modell.schaumZoom = null
                }
            }
        }
        if (modell.schaumZoom != null) {
            WahlChip("← alle ${art.oben}", false, farben) { modell.schaumZoom = null }
        }
        Gruppenleiste(modell, farben, lieder, karte, mass, art, thema)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Farbe", color = farben.schwach, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            Slider(
                value = modell.schaumDeckung,
                onValueChange = { modell.schaumDeckung = it },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(thumbColor = farben.akzent, activeTrackColor = farben.akzent, inactiveTrackColor = farben.rand),
            )
        }
        SchaumFeld(modell, farben, lieder, karte, spielId, tonLaeuft, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun Zeile(farben: KlangFarben, inhalt: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { inhalt() }
}

@Composable
private fun Gruppenleiste(
    modell: KlangModell,
    farben: KlangFarben,
    lieder: List<Lied>,
    karte: KarteAntwort?,
    mass: SchaumMass,
    art: SchaumGliederung,
    thema: String,
) {
    val gruppen = remember(lieder, karte, mass, art) { gruppenSummen(lieder, karte, mass, art) }
    if (gruppen.isEmpty() || modell.schaumZoom != null) return
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        gruppen.forEach { (name, _) ->
            val farbe = gruppenFarbe(thema, name, art, karte, farben.akzent)
            Row(
                Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .background(farben.flaeche)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .then(Modifier)
                    .pointerTap { modell.schaumZoom = name },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(farbe))
                Text(name, color = farben.text, modifier = Modifier.padding(start = 6.dp), maxLines = 1, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun Modifier.pointerTap(onTap: () -> Unit): Modifier =
    pointerInput(onTap) { detectTapGestures(onTap = { onTap() }) }

@Composable
private fun SchaumFeld(
    modell: KlangModell,
    farben: KlangFarben,
    lieder: List<Lied>,
    karte: KarteAntwort?,
    spielId: String?,
    tonLaeuft: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val mass = modell.schaumMass
    val art = modell.schaumGliederung
    val zoom = modell.schaumZoom
    var verh by remember { mutableFloatStateOf(0f) }
    val schluessel = remember(lieder, karte, mass, art, zoom, verh) {
        val ids = lieder.joinToString(",") { "${it.id}:${veil.raven.klangdroid.schaum.gewicht(it, mass)}" }
        listOf(mass.id, art.id, zoom ?: "", verh, ids.hashCode()).joinToString("|")
    }
    var bild by remember(schluessel) { mutableStateOf(modell.schaumSpeicher[schluessel]) }
    var fehler by remember(schluessel) { mutableStateOf<String?>(null) }
    var sekunden by remember(schluessel) { mutableStateOf(0) }
    val deckung = modell.schaumDeckung
    LaunchedEffect(schluessel) {
        if (verh <= 0f || bild != null) return@LaunchedEffect
        val auftrag = schaumAuftrag(lieder, karte, mass, art, zoom, verh) ?: return@LaunchedEffect
        fehler = null
        val erg = runCatching {
            coroutineScope {
                val uhr = async {
                    while (true) {
                        delay(1000)
                        sekunden += 1
                    }
                }
                try {
                    SchaumRechner.legen(context, auftrag)
                } finally {
                    uhr.cancel()
                }
            }
        }
        erg.fold(
            onSuccess = {
                modell.schaumSpeicher[schluessel] = it
                bild = it
            },
            onFailure = { fehler = it.message ?: "Der Schaum ließ sich nicht legen." },
        )
    }
    BoxWithConstraints(modifier) {
        // Der Mini-Player ändert die Höhe nur wenig. Neu legen erst,
        // wenn sich das Feld wirklich anders proportioniert, etwa im Querformat.
        val neu = if (maxWidth.value > 0f) ((maxHeight / maxWidth) * 20).toInt() / 20f else 0f
        if (neu > 0.4f && (verh <= 0f || kotlin.math.abs(neu - verh) > 0.18f)) verh = neu.coerceIn(0.6f, 2.2f)
        val fertig = bild
        when {
            fehler != null -> Text(fehler!!, color = farben.akzent, modifier = Modifier.padding(12.dp))
            fertig == null && schaumAuftrag(lieder, karte, mass, art, zoom, verh) == null ->
                Text("Kein Titel hat bei ${mass.label} mehr als null.", color = farben.schwach, modifier = Modifier.padding(12.dp))
            fertig == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = farben.akzent)
                Text("Dein Klang wird aufgeschäumt … ${sekunden}s", color = farben.schwach, modifier = Modifier.padding(top = 12.dp))
            }
            else -> SchaumLeinwand(modell, farben, fertig, lieder, karte, spielId, tonLaeuft, deckung, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun BoxWithConstraints(modifier: Modifier, inhalt: @Composable androidx.compose.foundation.layout.BoxWithConstraintsScope.() -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier, content = inhalt)
}

@Composable
private fun SchaumLeinwand(
    modell: KlangModell,
    farben: KlangFarben,
    bild: SchaumBild,
    lieder: List<Lied>,
    karte: KarteAntwort?,
    spielId: String?,
    tonLaeuft: Boolean,
    deckung: Float,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val thema = modell.werte.value.thema
    val art = modell.schaumGliederung
    var blick by remember(bild) { mutableStateOf(Offset.Zero) }
    var zoom by remember(bild) { mutableFloatStateOf(1f) }
    val blaetter = remember(bild) { bild.zellen.filter { it.row >= 0 && it.outline.size >= 3 } }
    val gefaesse = remember(bild) { bild.zellen.filter { it.row < 0 && it.outline.size >= 3 } }
    var decken by remember(bild) { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    LaunchedEffect(bild, modell.werte.value.adresse) {
        val basis = modell.werte.value.adresse
        val titelbild = modell.bestand.index?.titelbild?.toSet().orEmpty()
        val stand = modell.bestand.index?.kachelStand ?: 0
        val ziele = blaetter.map { it.id }.distinct().mapNotNull { id ->
            val lied = modell.lied(id) ?: return@mapNotNull null
            id to medienUrl(basis, id, bildDatei(lied, modell.bestand.eigen[id], titelbild, true, stand))
        }
        decken = coroutineScope {
            ziele.map { (id, url) ->
                async {
                    val erg = runCatching {
                        val loader = coil3.SingletonImageLoader.get(context)
                        val res = loader.execute(ImageRequest.Builder(context).data(url).size(CoilSize(160, 160)).build())
                        val bmp = (res as? SuccessResult)?.image as? BitmapImage
                        bmp?.bitmap?.asImageBitmap()?.let { id to it }
                    }.getOrNull()
                    erg
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }
    val puls = rememberInfiniteTransition(label = "wabern")
    val welle by puls.animateFloat(0f, 1f, infiniteRepeatable(tween(3100, easing = LinearEasing)), label = "welle")
    val atem = if (tonLaeuft) 1f + 0.035f * sin(welle * 2.0 * PI).toFloat() else 1f
    val schrift = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = android.graphics.Color.WHITE
            setShadowLayer(6f, 0f, 1f, 0x99000000.toInt())
        }
    }
    val dichte = LocalDensity.current
    Canvas(
        modifier.pointerInput(bild) {
            detectTransformGestures { _, pan, geste, _ ->
                zoom = (zoom * geste).coerceIn(0.7f, 6f)
                blick += pan
            }
        }.pointerInput(bild, blaetter, zoom, blick) {
            detectTapGestures { stelle ->
                val treffer = treffer(stelle, blaetter, bild, size.width.toFloat(), size.height.toFloat(), zoom, blick) ?: return@detectTapGestures
                val lied = modell.lied(treffer.id) ?: return@detectTapGestures
                val reihe = blaetter.mapNotNull { modell.lied(it.id) }
                val spielbar = reihe.filter { tonDatei(it, modell.bestand.eigen[it.id], modell.werte.value.ton) != null }
                val bei = spielbar.indexOfFirst { it.id == lied.id }
                if (bei < 0) modell.hinweis = "Für „${lied.titel}“ liegt lokal kein Ton."
                else modell.abspielen(spielbar, bei, oeffnen = false)
            }
        },
    ) {
        val fit = min(size.width / bild.width.toFloat(), size.height / bild.height.toFloat())
        val ox = (size.width - bild.width.toFloat() * fit) / 2f + blick.x
        val oy = (size.height - bild.height.toFloat() * fit) / 2f + blick.y
        fun sx(x: Double) = ox + x.toFloat() * fit * zoom
        fun sy(y: Double) = oy + (bild.height - y).toFloat() * fit * zoom
        val stich = min(1f, deckung * 2f)
        val flach = max(0f, deckung * 2f - 1f) * 0.65f
        for (zelle in blaetter) {
            val roh = pfad(zelle, ::sx, ::sy)
            val spielt = zelle.id == spielId
            val pfad = if (spielt && atem != 1f) geatmet(roh, atem) else roh
            val lied = modell.lied(zelle.id)
            val gruppenName = if (lied != null) gliederungVon(lied, karte, art).first else zelle.name
            val basis = gruppenFarbe(thema, gruppenName, art, karte, farben.akzent)
            val ton = zellTon(basis, zelle.row)
            val bildchen = decken[zelle.id]
            if (bildchen != null) {
                clipPath(pfad) {
                    val grenze = pfad.getBounds()
                    val bw = bildchen.width.toFloat()
                    val bh = bildchen.height.toFloat()
                    val s = max(grenze.width / bw, grenze.height / bh)
                    val dw = bw * s
                    val dh = bh * s
                    drawImage(
                        bildchen,
                        dstOffset = IntOffset((grenze.center.x - dw / 2f).toInt(), (grenze.center.y - dh / 2f).toInt()),
                        dstSize = IntSize(dw.toInt().coerceAtLeast(1), dh.toInt().coerceAtLeast(1)),
                    )
                }
            } else {
                drawPath(pfad, ton)
            }
            if (stich > 0f && bildchen != null) drawPath(pfad, ton.copy(alpha = stich), blendMode = BlendMode.Softlight)
            if (flach > 0f && bildchen != null) drawPath(pfad, ton.copy(alpha = flach))
            drawPath(pfad, farben.bg, style = Stroke(width = 2.dp.toPx()))
            val grenze = pfad.getBounds()
            if (grenze.width > 46.dp.toPx() && grenze.height > 22.dp.toPx()) {
                val px = with(dichte) { 11.dp.toPx() }
                schrift.textSize = min(px * 1.3f, grenze.width / (zelle.name.length.coerceAtLeast(4) * 0.55f))
                drawContext.canvas.nativeCanvas.drawText(
                    zelle.name,
                    grenze.center.x,
                    grenze.center.y + schrift.textSize * 0.35f,
                    schrift,
                )
            }
        }
        for (zelle in gefaesse) {
            drawPath(pfad(zelle, ::sx, ::sy), farben.text.copy(alpha = 0.28f), style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

private fun pfad(zelle: SchaumZelle, sx: (Double) -> Float, sy: (Double) -> Float): Path = Path().apply {
    val o = zelle.outline
    moveTo(sx(o[0][0]), sy(o[0][1]))
    for (i in 1 until o.size) lineTo(sx(o[i][0]), sy(o[i][1]))
    close()
}

private fun geatmet(pfad: Path, faktor: Float): Path {
    val c = pfad.getBounds().center
    val nativ = android.graphics.Path(pfad.asAndroidPath())
    val m = android.graphics.Matrix()
    m.setScale(faktor, faktor, c.x, c.y)
    nativ.transform(m)
    return nativ.asComposePath()
}

private fun treffer(
    stelle: Offset,
    blaetter: List<SchaumZelle>,
    bild: SchaumBild,
    breite: Float,
    hoehe: Float,
    zoom: Float,
    blick: Offset,
): SchaumZelle? {
    val fit = min(breite / bild.width.toFloat(), hoehe / bild.height.toFloat())
    val ox = (breite - bild.width.toFloat() * fit) / 2f + blick.x
    val oy = (hoehe - bild.height.toFloat() * fit) / 2f + blick.y
    fun sx(x: Double) = ox + x.toFloat() * fit * zoom
    fun sy(y: Double) = oy + (bild.height - y).toFloat() * fit * zoom
    return blaetter.lastOrNull { zelle ->
        val pts = zelle.outline.map { Offset(sx(it[0]), sy(it[1])) }
        innen(pts, stelle)
    }
}

private fun innen(pts: List<Offset>, p: Offset): Boolean {
    var drin = false
    var j = pts.lastIndex
    for (i in pts.indices) {
        val a = pts[i]
        val b = pts[j]
        if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) drin = !drin
        j = i
    }
    return drin
}

private fun gruppenSummen(
    lieder: List<Lied>,
    karte: KarteAntwort?,
    mass: SchaumMass,
    art: SchaumGliederung,
): List<Pair<String, Double>> {
    val summe = LinkedHashMap<String, Double>()
    for (lied in lieder) {
        if (lied.imPapierkorb) continue
        val w = veil.raven.klangdroid.schaum.gewicht(lied, mass)
        if (w <= 0.0) continue
        val name = gliederungVon(lied, karte, art).first
        summe[name] = (summe[name] ?: 0.0) + w
    }
    return summe.entries.sortedByDescending { it.value }.map { it.key to it.value }
}

private val SCHAUM_PALETTE = mapOf(
    "rabe" to listOf("#b05cd6", "#ff5fb3", "#5a6cff", "#e590ff", "#7d3cff", "#ff9ad8", "#4fa3ff", "#c9a8ff"),
    "nebel" to listOf("#e0455a", "#2ec4b6", "#4f7bd9", "#e8c04a", "#b05cd6", "#ff8c42", "#7bd389", "#9bb7ff"),
    "eis" to listOf("#6fd3ff", "#2ec4b6", "#6f7bff", "#c9ecff", "#2f8cff", "#7ff2dc", "#a99bff", "#3fa8c4"),
    "polar" to listOf("#45f0a0", "#3aa8ff", "#b36cff", "#8ff6ff", "#2fd1b4", "#6f7dff", "#e0a6ff", "#a6ffcf"),
    "mond" to listOf("#9ff3ff", "#a9c8ff", "#efe0c2", "#20d4e8", "#7fb2e6", "#d8f6ff", "#c9b48f", "#5fe0d0"),
    "glut" to listOf("#ff5a48", "#ffb03a", "#ff6fa8", "#ffe07a", "#d9456e", "#ff8a52", "#f0c38f", "#c7372f"),
    "borg" to listOf("#39ff6a", "#18b85c", "#a8ff3e", "#0e8f6e", "#62dca0", "#d4ff8f", "#2fe6bd", "#7aa83a"),
    "haus" to listOf("#f97b14", "#e31c79", "#ffb03a", "#ff5a48", "#ff8ad4", "#ffd27a", "#c44bff", "#ff7a3c"),
)

private fun gruppenFarbe(thema: String, name: String, art: SchaumGliederung, karte: KarteAntwort?, akzent: Color): Color {
    if (name.startsWith("ohne ") || name == "ohne Modell" || name == "ohne Datum") return Color(0xFF5D6066)
    val palette = SCHAUM_PALETTE[thema] ?: return akzent
    val index = if (art == SchaumGliederung.KLANG) {
        karte?.gruppen?.firstOrNull { it.name == name }?.nr ?: name.hashCode().ushr(1)
    } else {
        name.hashCode().ushr(1)
    }
    return Color(android.graphics.Color.parseColor(palette[index % palette.size]))
}

private fun zellTon(basis: Color, rang: Int): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(basis.toArgb(), hsv)
    val h = (hsv[0] + (rang % 5 - 2) * 7f + 360f) % 360f
    return Color.hsv(h, (0.46f + (rang % 4) * 0.05f).coerceAtMost(0.85f), 0.62f)
}
