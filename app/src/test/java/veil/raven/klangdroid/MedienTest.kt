package veil.raven.klangdroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import veil.raven.klangdroid.data.EigenKunst
import veil.raven.klangdroid.data.Lied
import veil.raven.klangdroid.data.Sortierung
import veil.raven.klangdroid.data.Tonquelle
import veil.raven.klangdroid.data.bildDatei
import veil.raven.klangdroid.data.normalisiereAdresse
import veil.raven.klangdroid.data.siebe
import veil.raven.klangdroid.data.tonDatei
import veil.raven.klangdroid.data.videoDatei

class MedienTest {
    @Test
    fun adresse() {
        assertEquals("http://localhost:8788/", normalisiereAdresse("  localhost:8788 "))
        assertEquals("http://localhost:8788/", normalisiereAdresse("http://localhost:8788"))
        assertEquals("http://10.0.2.2:8788/", normalisiereAdresse("http://10.0.2.2:8788/"))
        assertEquals("http://localhost:8788/", normalisiereAdresse(" "))
    }

    @Test
    fun videoUndTon() {
        val lied = Lied(id = "a", videoCoverUrl = "https://cdn.example/v.mp4", hatMp3 = true, hatWav = true)
        val eigen = EigenKunst(video = true, videos = listOf(1, 3), ton = true, tonDatei = "eigen.wav", bild = true, bilder = listOf(2))
        assertEquals("eigen-3.mp4", videoDatei(lied, eigen))
        assertEquals("artwork.mp4", videoDatei(lied, null))
        assertNull(videoDatei(Lied(id = "b"), null))
        assertEquals("eigen.wav", tonDatei(lied, eigen, Tonquelle.EIGEN))
        assertEquals("audio.wav", tonDatei(lied, eigen, Tonquelle.WAV))
        assertEquals("audio.mp3", tonDatei(lied, eigen, Tonquelle.MP3))
        assertEquals("audio.wav", tonDatei(Lied(id = "c", hatWav = true), null, Tonquelle.MP3))
        assertEquals("eigen-2.jpg", bildDatei(lied, eigen, emptySet(), kachel = true, kachelStand = 5))
        assertEquals("kachel.jpg?k=5", bildDatei(lied, null, emptySet(), kachel = true, kachelStand = 5))
        assertEquals("titelbild.jpg?k=5", bildDatei(lied, null, setOf("a"), kachel = false, kachelStand = 5))
    }

    @Test
    fun sieb() {
        val mit = Lied(id = "1", titel = "Rabenlied", hatMp3 = true, erstellt = "2026-02-01")
        val ohne = Lied(id = "2", titel = "Stille", erstellt = "2026-03-01")
        val alt = Lied(id = "3", titel = "Alt", hatMp3 = true, erstellt = "2025-01-01", imPapierkorb = true)
        val liste = listOf(ohne, alt, mit)
        val eng = siebe(liste, emptyMap(), "raben", nurTon = true, nurVideo = false, papier = false, Sortierung.NEU, Tonquelle.MP3)
        assertEquals(listOf("1"), eng.map { it.id })
        val alles = siebe(liste, emptyMap(), "", nurTon = false, nurVideo = false, papier = true, Sortierung.TITEL, Tonquelle.MP3)
        assertEquals(listOf("Alt", "Rabenlied", "Stille"), alles.map { it.titel })
    }
}
