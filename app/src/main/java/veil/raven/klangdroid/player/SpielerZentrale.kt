package veil.raven.klangdroid.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import veil.raven.klangdroid.data.EigenKunst
import veil.raven.klangdroid.data.Lied
import veil.raven.klangdroid.data.Tonquelle

data class SpielerZustand(
    val verbunden: Boolean = false,
    val spielt: Boolean = false,
    val mediaId: String? = null,
    val titel: String? = null,
    val kuenstler: String? = null,
    val positionMs: Long = 0,
    val dauerMs: Long = 0,
    val index: Int = 0,
    val anzahl: Int = 0,
    val zufall: Boolean = false,
    val wiederholen: Int = Player.REPEAT_MODE_OFF,
    val puffert: Boolean = false,
    val fehler: String? = null,
)

class SpielerZentrale(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val zustandInnen = MutableStateFlow(SpielerZustand())
    val zustand: StateFlow<SpielerZustand> = zustandInnen

    private var controller: MediaController? = null
    private var wartend: (() -> Unit)? = null

    init {
        val token = SessionToken(app, ComponentName(app, WiedergabeDienst::class.java))
        val zukunft = MediaController.Builder(app, token).buildAsync()
        zukunft.addListener(
            {
                val c = runCatching { zukunft.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        veroeffentlichen()
                    }
                })
                veroeffentlichen()
                wartend?.invoke()
                wartend = null
            },
            { befehl -> app.mainExecutor.execute(befehl) },
        )
        scope.launch {
            while (isActive) {
                if (controller?.isPlaying == true) veroeffentlichen()
                delay(250)
            }
        }
    }

    fun spielen(
        basis: String,
        lieder: List<Lied>,
        index: Int,
        quelle: Tonquelle,
        eigen: Map<String, EigenKunst>,
        titelbild: Set<String>,
        kachelStand: Long,
    ) {
        val tun = tun@{
            val c = controller ?: return@tun
            val stuecke = lieder.mapNotNull { mediaItem(basis, it, eigen[it.id], quelle, titelbild, kachelStand) }
            if (stuecke.isEmpty()) return@tun
            val ziel = index.coerceIn(0, stuecke.lastIndex)
            c.setMediaItems(stuecke, ziel, 0)
            c.prepare()
            c.play()
        }
        if (controller == null) wartend = tun else tun()
    }

    fun umschalten() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun weiter() { controller?.seekToNext() }
    fun zurueck() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPrevious()
    }

    fun suchen(ms: Long) { controller?.seekTo(ms.coerceAtLeast(0)) }

    fun zufall(an: Boolean) { controller?.shuffleModeEnabled = an }

    fun wiederholenZyklus() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        veroeffentlichen()
    }

    private fun veroeffentlichen() {
        val c = controller
        if (c == null) {
            zustandInnen.value = SpielerZustand()
            return
        }
        val meta = c.mediaMetadata
        val dauer = c.duration
        zustandInnen.value = SpielerZustand(
            verbunden = true,
            spielt = c.isPlaying,
            mediaId = c.currentMediaItem?.mediaId,
            titel = meta.title?.toString(),
            kuenstler = meta.artist?.toString(),
            positionMs = c.currentPosition.coerceAtLeast(0),
            dauerMs = if (dauer == C.TIME_UNSET || dauer < 0) 0 else dauer,
            index = c.currentMediaItemIndex.coerceAtLeast(0),
            anzahl = c.mediaItemCount,
            zufall = c.shuffleModeEnabled,
            wiederholen = c.repeatMode,
            puffert = c.playbackState == Player.STATE_BUFFERING,
            fehler = c.fehlertext(),
        )
    }
}
