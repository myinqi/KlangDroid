package veil.raven.klangdroid.schaum

import android.app.Activity
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.json.JSONTokener
import veil.raven.klangdroid.data.KarteAntwort
import veil.raven.klangdroid.data.Lied
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class SchaumMass(val id: String, val label: String, val erklaerung: String) {
    HERZEN("herzen", "Herzen", "Jeder Titel so groß wie die Herzen, die er bekommen hat. Titel ohne Herz fehlen."),
    ABRUFE("abrufe", "Abrufe", "Jeder Titel so groß, wie oft er abgerufen wurde."),
    KOMMENTARE("kommentare", "Kommentare", "Jeder Titel so groß wie die Zahl seiner Kommentare. Titel ohne Kommentar fehlen."),
    RESONANZ("resonanz", "Resonanz", "Abrufe + 2 × Herzen + 3 × Kommentare."),
    LAENGE("laenge", "Titellänge", "Jeder Titel so groß, wie lange er spielt."),
    HOERZEIT("hoerzeit", "Hörzeit", "Abrufe × Titellänge. Eine Schätzung, als wäre jeder Abruf ganz gehört worden."),
    ANZAHL("anzahl", "Anzahl", "Jeder Titel gleich groß."),
}

enum class SchaumGliederung(val id: String, val label: String, val oben: String, val unten: String) {
    KLANG("klang", "Stilgruppe", "Stilgruppen", "Genres"),
    MODELL("modell", "Modell", "Modelle", "Jahre"),
    ZEIT("zeit", "Zeit", "Jahre", "Monate"),
}

private val MONATE = listOf(
    "Januar", "Februar", "März", "April", "Mai", "Juni",
    "Juli", "August", "September", "Oktober", "November", "Dezember",
)

fun gewicht(lied: Lied, mass: SchaumMass): Double = when (mass) {
    SchaumMass.HERZEN -> lied.likes.toDouble()
    SchaumMass.ABRUFE -> lied.plays.toDouble()
    SchaumMass.KOMMENTARE -> lied.kommentare.toDouble()
    SchaumMass.RESONANZ -> lied.plays + 2.0 * lied.likes + 3.0 * lied.kommentare
    SchaumMass.LAENGE -> lied.dauer ?: 0.0
    SchaumMass.HOERZEIT -> lied.plays * (lied.dauer ?: 0.0)
    SchaumMass.ANZAHL -> 1.0
}

fun gliederungVon(lied: Lied, karte: KarteAntwort?, art: SchaumGliederung): Pair<String, String> = when (art) {
    SchaumGliederung.KLANG -> {
        val punkt = karte?.songs?.firstOrNull { it.id == lied.id }
        val name = punkt?.gruppe?.let { nr -> karte.gruppen.firstOrNull { it.nr == nr }?.name }
        (name ?: "ohne Stilgruppe") to (punkt?.genre?.takeIf { it.isNotBlank() } ?: "")
    }
    SchaumGliederung.MODELL -> {
        val modell = lied.modell ?: lied.modellRoh?.let { "Modell: $it" } ?: "ohne Modell"
        modell to (lied.erstellt?.take(4) ?: "")
    }
    SchaumGliederung.ZEIT -> {
        val e = lied.erstellt.orEmpty()
        val monat = e.drop(5).take(2).toIntOrNull()?.let { if (it in 1..12) MONATE[it - 1] else "" } ?: ""
        (e.take(4).ifBlank { "ohne Datum" }) to monat
    }
}

@Serializable
data class SchaumZeile(
    val gruppe: String,
    val genre: String,
    val titel: String,
    val id: String,
    val w: String,
)

@Serializable
data class SchaumAuftrag(
    val rows: List<SchaumZeile>,
    val levels: List<String>,
    val W: Int,
    val H: Int,
    val seeds: Map<String, List<Double>>? = null,
)

@Serializable
data class SchaumZelle(
    val name: String = "",
    val depth: Int = 0,
    val row: Int = -1,
    val id: String = "",
    val outline: List<List<Double>> = emptyList(),
)

@Serializable
data class SchaumBild(
    val width: Double = 1000.0,
    val height: Double = 1000.0,
    val ms: Double = 0.0,
    val zellen: List<SchaumZelle> = emptyList(),
)

fun schaumAuftrag(
    lieder: List<Lied>,
    karte: KarteAntwort?,
    mass: SchaumMass,
    art: SchaumGliederung,
    zoom: String?,
    breitZuHoch: Float,
): SchaumAuftrag? {
    val zeilen = lieder.mapNotNull { lied ->
        if (lied.imPapierkorb) return@mapNotNull null
        val w = gewicht(lied, mass)
        if (w <= 0.0) return@mapNotNull null
        val (oben, unten) = gliederungVon(lied, karte, art)
        if (zoom != null && oben != zoom) return@mapNotNull null
        SchaumZeile(oben, unten, lied.titel.ifBlank { "Ohne Titel" }, lied.id, w.toString())
    }
    if (zeilen.isEmpty()) return null
    val ebenen = if (zoom == null) listOf("gruppe", "genre") else listOf("genre")
    val b = 1000
    val h = (1000f * breitZuHoch).toInt().coerceIn(600, 2200)
    val seeds = saat(zeilen, ebenen, karte, b.toDouble(), h.toDouble())
    return SchaumAuftrag(zeilen, ebenen + "row", b, h, seeds)
}

private fun saat(
    zeilen: List<SchaumZeile>,
    ebenen: List<String>,
    karte: KarteAntwort?,
    breite: Double,
    hoehe: Double,
): Map<String, List<Double>>? {
    val lage = karte?.songs?.associateBy { it.id }.orEmpty()
    val umap = karte?.verfahren == "umap"
    fun punkt(id: String): DoubleArray? {
        val p = lage[id] ?: return null
        val q = if (umap && p.umap.size >= 2) doubleArrayOf(p.umap[0], p.umap[1]) else doubleArrayOf(p.x, p.y)
        return if (q[0].isFinite() && q[1].isFinite()) q else null
    }
    val roh = zeilen.map { punkt(it.id) }
    val da = roh.filterNotNull()
    if (da.size < 2) return null
    val x0 = da.minOf { it[0] }
    val x1 = da.maxOf { it[0] }
    val y0 = da.minOf { it[1] }
    val y1 = da.maxOf { it[1] }
    val rand = 0.06
    fun abb(p: DoubleArray) = doubleArrayOf(
        breite * (rand + (1 - 2 * rand) * (p[0] - x0) / ((x1 - x0).takeIf { it != 0.0 } ?: 1.0)),
        hoehe * (1 - rand - (1 - 2 * rand) * (p[1] - y0) / ((y1 - y0).takeIf { it != 0.0 } ?: 1.0)),
    )
    val ohne = zeilen.size - da.size
    var k = 0
    val punkte = zeilen.indices.map { ri ->
        val p = roh[ri]
        if (p != null) abb(p) else {
            val r = sqrt((k + 0.5) / ohne.coerceAtLeast(1)) * 0.4 * min(breite, hoehe)
            val a = k++ * 2.39996
            doubleArrayOf(breite / 2 + r * cos(a), hoehe / 2 + r * sin(a))
        }
    }
    return saatSchluessel(zeilen, ebenen, punkte)
}

private fun saatSchluessel(
    zeilen: List<SchaumZeile>,
    ebenen: List<String>,
    punkte: List<DoubleArray>,
): Map<String, List<Double>> {
    val saat = HashMap<String, List<Double>>()
    val summe = HashMap<String, DoubleArray>()
    fun dazu(key: String, q: DoubleArray) {
        val s = summe.getOrPut(key) { doubleArrayOf(0.0, 0.0, 0.0) }
        s[0] += q[0]
        s[1] += q[1]
        s[2] += 1.0
    }
    zeilen.forEachIndexed { ri, z ->
        val q = punkte[ri]
        q[0] += (ri % 7) * 1e-3
        q[1] += (ri % 5) * 1e-3
        var last = "(none)"
        val teile = ArrayList<String>()
        for (feld in ebenen) {
            val v = (if (feld == "gruppe") z.gruppe else z.genre).trim()
            val name = v.ifBlank { last }
            last = name
            teile += name
            dazu(teile.joinToString("\u0001"), q)
        }
        dazu("", q)
        val blatt = z.titel.trim().ifBlank { "row ${ri + 1}" }
        teile += "$blatt\u0000$ri"
        saat[teile.joinToString("\u0001")] = listOf(q[0], q[1])
    }
    for ((key, s) in summe) saat[key] = listOf(s[0] / s[2], s[1] / s[2])
    return saat
}

object SchaumRechner {
    private val json = Json { ignoreUnknownKeys = true }
    private val sperre = Mutex()
    private var script: String? = null
    private var motor: Motor? = null

    /**
     * foamtree legt ein großes Gefäß auf dem Handy spürbar lang.
     * Dieselbe Datei läuft in der WebView. Das fertige Bild bleibt unter
     * filesDir, Schlüssel ist der Auftrag: gleiche Zahlen öffnen sofort,
     * neue Zahlen legen neu. Android räumt filesDir nicht als Cache leer.
     */
    suspend fun legen(context: Context, auftrag: SchaumAuftrag): SchaumBild = withContext(Dispatchers.IO) {
        sperre.withLock {
            val roh = json.encodeToString(SchaumAuftrag.serializer(), auftrag)
            val name = sha(roh) + ".json"
            val datei = File(context.filesDir, "schaum/$name")
            val alt = File(context.cacheDir, "schaum/$name")
            if (!datei.isFile && alt.isFile) {
                datei.parentFile?.mkdirs()
                alt.copyTo(datei, overwrite = true)
            }
            if (datei.isFile) {
                return@withLock json.decodeFromString(SchaumBild.serializer(), datei.readText())
            }
            val erg = withContext(Dispatchers.Main) { rechnen(context, roh) }
            datei.parentFile?.mkdirs()
            datei.writeText(erg)
            json.decodeFromString(SchaumBild.serializer(), erg)
        }
    }

    private suspend fun rechnen(context: Context, roh: String): String {
        val m = motor(context)
        m.bereit.await()
        return suspendCoroutine { cont ->
            m.web.evaluateJavascript("legenSicher(${JSONObject.quote(roh)})") { raw ->
                val text = runCatching { JSONTokener(raw ?: "null").nextValue() as? String }.getOrNull()
                when {
                    text == null -> cont.resumeWithException(IllegalStateException("Der Schaum ließ sich nicht legen."))
                    text.startsWith("ERR:") -> cont.resumeWithException(IllegalStateException(text.removePrefix("ERR:")))
                    else -> cont.resume(text)
                }
            }
        }
    }

    private fun motor(context: Context): Motor {
        motor?.let { return it }
        val text = script ?: context.assets.open("klangschaum/foamtree.js").bufferedReader().use { it.readText() }
            .also { script = it }
        val activity = context as? Activity
            ?: error("Der Schaum braucht das Fenster der App.")
        val web = WebView(activity)
        val bereit = CompletableDeferred<Unit>()
        web.settings.javaScriptEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (!bereit.isCompleted) bereit.complete(Unit)
            }
        }
        val html = "<!DOCTYPE html><meta charset=\"utf-8\"><script>\n" + text +
            "\nfunction legenSicher(j){try{return legen(j)}catch(e){return 'ERR:'+(e&&e.stack||e)}}\n</script>"
        activity.addContentView(web, FrameLayout.LayoutParams(1, 1).apply { leftMargin = -8 })
        web.loadDataWithBaseURL("https://klangdroid.local/", html, "text/html", "utf-8", null)
        return Motor(web, bereit).also { motor = it }
    }

    private fun sha(text: String): String {
        val dig = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return dig.joinToString("") { "%02x".format(it) }.take(32)
    }

    private class Motor(val web: WebView, val bereit: CompletableDeferred<Unit>)
}
