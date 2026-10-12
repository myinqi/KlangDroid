package veil.raven.klangdroid

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import veil.raven.klangdroid.data.EigenKunst
import veil.raven.klangdroid.data.BibliothekAnsicht
import veil.raven.klangdroid.data.Einstellungen
import veil.raven.klangdroid.data.IndexAntwort
import veil.raven.klangdroid.data.KlangApi
import veil.raven.klangdroid.data.Lied
import veil.raven.klangdroid.data.Sortierung
import veil.raven.klangdroid.data.Tonquelle
import veil.raven.klangdroid.data.Werte
import veil.raven.klangdroid.player.SpielerZentrale

data class BestandStand(
    val laedt: Boolean = true,
    val fehler: String? = null,
    val index: IndexAntwort? = null,
    val eigen: Map<String, EigenKunst> = emptyMap(),
    val karte: veil.raven.klangdroid.data.KarteAntwort? = null,
)

data class Textblatt(
    val id: String,
    val laedt: Boolean,
    val lyrics: String? = null,
    val stil: String? = null,
    val fehler: String? = null,
)

class KlangModell(app: Application) : AndroidViewModel(app) {
    private val einstellungen = (app as KlangAnwendung).einstellungen
    val spieler: SpielerZentrale = (app as KlangAnwendung).spieler
    private val api = KlangApi()

    val werte = einstellungen.werte.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        Werte(),
    )

    var bestand by mutableStateOf(BestandStand())
        private set

    var suche by mutableStateOf("")
    var nurTon by mutableStateOf(true)
    var nurVideo by mutableStateOf(false)
    var papier by mutableStateOf(false)
    var sortierung by mutableStateOf(Sortierung.NEU)
    var ansicht by mutableStateOf(BibliothekAnsicht.KACHELN)
    var schaumMass by mutableStateOf(veil.raven.klangdroid.schaum.SchaumMass.HERZEN)
    var schaumGliederung by mutableStateOf(veil.raven.klangdroid.schaum.SchaumGliederung.KLANG)
    var schaumZoom by mutableStateOf<String?>(null)
    var schaumDeckung by mutableFloatStateOf(1f)
    val schaumSpeicher = object : LinkedHashMap<String, veil.raven.klangdroid.schaum.SchaumBild>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, veil.raven.klangdroid.schaum.SchaumBild>?) = size > 6
    }
    var playlistId by mutableStateOf<String?>(null)
    var spielerOffen by mutableStateOf(false)
    var einstellungenOffen by mutableStateOf(false)
    var textblatt by mutableStateOf<Textblatt?>(null)
    var hinweis by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            einstellungen.werte.map { it.adresse }.distinctUntilChanged().collect { adresse ->
                neuLaden(adresse)
            }
        }
    }

    fun neuLaden(adresse: String = werte.value.adresse) {
        viewModelScope.launch {
            bestand = bestand.copy(laedt = true, fehler = null)
            bestand = runCatching { api.laden(adresse) }
                .fold(
                    onSuccess = {
                        schaumSpeicher.clear()
                        BestandStand(laedt = false, index = it.index, eigen = it.eigen, karte = it.karte)
                    },
                    onFailure = { fehler ->
                        Log.e("KlangDroid", "Katalog", fehler)
                        val text = when (fehler) {
                            is java.net.ConnectException,
                            is java.net.SocketTimeoutException,
                            is java.net.UnknownHostException,
                            -> "Unter $adresse antwortet kein KlangTresor."
                            else -> fehler.message ?: "Der Katalog ließ sich nicht laden."
                        }
                        BestandStand(laedt = false, fehler = text)
                    },
                )
        }
    }

    fun verbinden(adresse: String) {
        viewModelScope.launch { einstellungen.adresse(adresse) }
    }

    fun thema(id: String) {
        viewModelScope.launch { einstellungen.thema(id) }
    }

    fun akzent(hex: String) {
        viewModelScope.launch { einstellungen.akzent(hex) }
    }

    fun ton(quelle: Tonquelle) {
        viewModelScope.launch { einstellungen.ton(quelle) }
    }

    fun lied(id: String?): Lied? = id?.let { bestand.index?.songs?.firstOrNull { lied -> lied.id == it } }

    fun abspielen(lieder: List<Lied>, index: Int, oeffnen: Boolean = true) {
        val w = werte.value
        val indexDaten = bestand.index
        spieler.spielen(
            basis = w.adresse,
            lieder = lieder,
            index = index,
            quelle = w.ton,
            eigen = bestand.eigen,
            titelbild = indexDaten?.titelbild?.toSet().orEmpty(),
            kachelStand = indexDaten?.kachelStand ?: 0,
        )
        if (oeffnen) spielerOffen = true
        hinweis = null
    }

    fun textOeffnen(lied: Lied) {
        val w = werte.value
        textblatt = Textblatt(id = lied.id, laedt = true, stil = lied.stilPrompt)
        viewModelScope.launch {
            textblatt = runCatching { api.liedtext(w.adresse, lied.id) }
                .fold(
                    onSuccess = { Textblatt(lied.id, laedt = false, lyrics = it.lyrics, stil = it.stilPrompt ?: lied.stilPrompt) },
                    onFailure = { Textblatt(lied.id, laedt = false, stil = lied.stilPrompt, fehler = "Der Text ließ sich nicht laden.") },
                )
        }
    }
}
