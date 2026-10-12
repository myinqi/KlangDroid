package veil.raven.klangdroid.data

import kotlinx.serialization.Serializable

@Serializable
data class IndexAntwort(
    val version: String? = null,
    val kachelStand: Long = 0,
    val titelbild: List<String> = emptyList(),
    val anzahl: Int = 0,
    val profil: Profil? = null,
    val songs: List<Lied> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

@Serializable
data class Profil(
    val display_name: String? = null,
    val handle: String? = null,
)

@Serializable
data class Lied(
    val id: String,
    val titel: String = "Ohne Titel",
    val anzeigename: String? = null,
    val handle: String? = null,
    val dauer: Double? = null,
    val erstellt: String? = null,
    val farben: Farbwerte? = null,
    val hatMp3: Boolean = false,
    val hatWav: Boolean = false,
    val videoCoverUrl: String? = null,
    val oeffentlich: Boolean = false,
    val imPapierkorb: Boolean = false,
    val hatLyrics: Boolean = false,
    val plays: Int = 0,
    val likes: Int = 0,
    val kommentare: Int = 0,
    val modell: String? = null,
    val modellRoh: String? = null,
    val stilPrompt: String? = null,
)

@Serializable
data class Farbwerte(
    val grund: String? = null,
    val flaeche: String? = null,
    val akzent: String? = null,
    val akzent2: String? = null,
    val text: String? = null,
    val textLeise: String? = null,
    val toene: List<Farbton> = emptyList(),
)

@Serializable
data class Farbton(
    val hex: String? = null,
)

@Serializable
data class Playlist(
    val id: String,
    val name: String = "Playlist",
    val beschreibung: String? = null,
    val eintraege: List<Eintrag> = emptyList(),
)

@Serializable
data class Eintrag(
    val songId: String,
    val position: Int = 0,
    val titel: String? = null,
    val anzeigename: String? = null,
    val eigen: Boolean = true,
)

@Serializable
data class EigenAntwort(
    val songs: Map<String, EigenKunst> = emptyMap(),
    val anzahl: Int = 0,
)

@Serializable
data class EigenKunst(
    val ton: Boolean = false,
    val tonDatei: String? = null,
    val video: Boolean = false,
    val videos: List<Int> = emptyList(),
    val bild: Boolean = false,
    val bilder: List<Int> = emptyList(),
)

@Serializable
data class LiedtextAntwort(
    val lyrics: String? = null,
    val stilPrompt: String? = null,
)

enum class Tonquelle(val id: String, val label: String) {
    MP3("mp3", "MP3"),
    WAV("wav", "WAV"),
    EIGEN("eigen", "Eigene Fassung"),
    ;

    companion object {
        fun von(id: String) = entries.firstOrNull { it.id == id } ?: MP3
    }
}

enum class Sortierung(val label: String) {
    NEU("Neu"),
    TITEL("Titel"),
    DAUER("Länge"),
}

enum class BibliothekAnsicht { KACHELN, LISTE, SCHAUM }

@Serializable
data class KarteAntwort(
    val verfahren: String? = null,
    val gruppen: List<KarteGruppe> = emptyList(),
    val songs: List<KarteLied> = emptyList(),
)

@Serializable
data class KarteGruppe(
    val nr: Int = 0,
    val name: String = "",
)

@Serializable
data class KarteLied(
    val id: String,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val umap: List<Double> = emptyList(),
    val gruppe: Int? = null,
    val genre: String? = null,
)
