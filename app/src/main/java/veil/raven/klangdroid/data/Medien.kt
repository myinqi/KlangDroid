package veil.raven.klangdroid.data

const val STANDARD_ADRESSE = "http://localhost:8788/"

fun normalisiereAdresse(roh: String?): String {
    var s = roh?.trim().orEmpty()
    if (s.isEmpty()) return STANDARD_ADRESSE
    if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://$s"
    return s.trimEnd('/') + "/"
}

fun medienUrl(basis: String, id: String, datei: String): String =
    basis.trimEnd('/') + "/media/" + id + "/" + datei

fun playlistBildUrl(basis: String, id: String): String =
    basis.trimEnd('/') + "/playlistbild/" + id + ".jpg"

/**
 * Eigene Videos heißen eigen.mp4, danach eigen-2.mp4 … Die höchste Nummer
 * ist die jüngste und gilt, wie im KlangTresor. Sonst das Suno-Bewegtbild
 * artwork.mp4, wenn der Katalog eines kennt.
 */
fun videoDatei(lied: Lied, eigen: EigenKunst?): String? {
    if (eigen?.video == true) {
        val nr = eigen.videos.maxOrNull() ?: 1
        return if (nr > 1) "eigen-$nr.mp4" else "eigen.mp4"
    }
    if (!lied.videoCoverUrl.isNullOrBlank()) return "artwork.mp4"
    return null
}

fun bildDatei(lied: Lied, eigen: EigenKunst?, titelbild: Set<String>, kachel: Boolean, kachelStand: Long): String {
    if (eigen?.bild == true) {
        val nr = eigen.bilder.maxOrNull() ?: 1
        return if (nr > 1) "eigen-$nr.jpg" else "eigen.jpg"
    }
    val stempel = if (kachelStand > 0) "?k=$kachelStand" else ""
    if (kachel) return "kachel.jpg$stempel"
    if (lied.id in titelbild) return "titelbild.jpg$stempel"
    return "cover.jpg"
}

/**
 * MP3 ist die Voreinstellung auf dem Handy. WAV und die eigene Fassung
 * werden vorgezogen, wenn sie gewählt sind und wirklich daliegen.
 * Fehlt die gewünschte Datei, fällt die Wahl auf das, was da ist.
 */
fun tonDatei(lied: Lied, eigen: EigenKunst?, quelle: Tonquelle): String? {
    if (quelle == Tonquelle.EIGEN && eigen?.ton == true) return eigen.tonDatei ?: "eigen.mp3"
    if (quelle == Tonquelle.WAV && lied.hatWav) return "audio.wav"
    if (lied.hatMp3) return "audio.mp3"
    if (lied.hatWav) return "audio.wav"
    if (eigen?.ton == true) return eigen.tonDatei ?: "eigen.mp3"
    return null
}

fun hatBewegtbild(lied: Lied, eigen: EigenKunst?) = videoDatei(lied, eigen) != null

fun siebe(
    lieder: List<Lied>,
    eigen: Map<String, EigenKunst>,
    suche: String,
    nurTon: Boolean,
    nurVideo: Boolean,
    papier: Boolean,
    sortierung: Sortierung,
    ton: Tonquelle,
): List<Lied> {
    val nadel = suche.trim().lowercase()
    val gefiltert = lieder.filter { lied ->
        if (!papier && lied.imPapierkorb) return@filter false
        if (nurTon && tonDatei(lied, eigen[lied.id], ton) == null) return@filter false
        if (nurVideo && !hatBewegtbild(lied, eigen[lied.id])) return@filter false
        if (nadel.isEmpty()) return@filter true
        lied.titel.lowercase().contains(nadel) ||
            lied.anzeigename?.lowercase()?.contains(nadel) == true ||
            lied.handle?.lowercase()?.contains(nadel) == true ||
            lied.stilPrompt?.lowercase()?.contains(nadel) == true
    }
    return when (sortierung) {
        Sortierung.NEU -> gefiltert.sortedWith(compareByDescending<Lied> { it.erstellt ?: "" }.thenBy { it.titel })
        Sortierung.TITEL -> gefiltert.sortedBy { it.titel.lowercase() }
        Sortierung.DAUER -> gefiltert.sortedByDescending { it.dauer ?: 0.0 }
    }
}

sealed interface Zeile {
    val id: String
    val titel: String

    data class Bekannt(val lied: Lied, val ton: String?) : Zeile {
        override val id get() = lied.id
        override val titel get() = lied.titel
    }

    data class Fremd(override val id: String, override val titel: String, val name: String?) : Zeile
}

fun playlistZeilen(
    playlist: Playlist,
    nachId: Map<String, Lied>,
    eigen: Map<String, EigenKunst>,
    ton: Tonquelle,
): List<Zeile> = playlist.eintraege.sortedBy { it.position }.map { eintrag ->
    val lied = nachId[eintrag.songId]
    if (lied != null) Zeile.Bekannt(lied, tonDatei(lied, eigen[lied.id], ton))
    else Zeile.Fremd(eintrag.songId, eintrag.titel ?: "Fremder Titel", eintrag.anzeigename)
}

fun dauerText(sekunden: Double?): String {
    val gesamt = (sekunden ?: return "–").toInt().coerceAtLeast(0)
    val minuten = gesamt / 60
    val rest = gesamt % 60
    val stunden = minuten / 60
    return if (stunden > 0) "%d:%02d:%02d".format(stunden, minuten % 60, rest)
    else "%d:%02d".format(minuten, rest)
}

fun zeitText(ms: Long): String {
    if (ms <= 0) return "0:00"
    val sekunden = ms / 1000
    val minuten = sekunden / 60
    val stunden = minuten / 60
    return if (stunden > 0) "%d:%02d:%02d".format(stunden, minuten % 60, sekunden % 60)
    else "%d:%02d".format(minuten, sekunden % 60)
}
