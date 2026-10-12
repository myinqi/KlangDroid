package veil.raven.klangdroid.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import veil.raven.klangdroid.MainActivity
import veil.raven.klangdroid.R
import veil.raven.klangdroid.data.EigenKunst
import veil.raven.klangdroid.data.Lied
import veil.raven.klangdroid.data.Tonquelle
import veil.raven.klangdroid.data.bildDatei
import veil.raven.klangdroid.data.medienUrl
import veil.raven.klangdroid.data.tonDatei

class WiedergabeDienst : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var sitzung: MediaSession? = null

    override fun onCreate() {
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId("klangdroid_wiedergabe")
                .setChannelName(R.string.kanal_wiedergabe)
                .build()
                .apply { setSmallIcon(R.drawable.ic_stat) },
        )
        super.onCreate()
        val spieler = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = spieler
        val offen = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_SPIELER, true)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        sitzung = MediaSession.Builder(this, spieler)
            .setSessionActivity(offen)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = sitzung

    override fun onDestroy() {
        sitzung?.release()
        sitzung = null
        player?.release()
        player = null
        super.onDestroy()
    }
}

fun mediaItem(
    basis: String,
    lied: Lied,
    eigen: EigenKunst?,
    quelle: Tonquelle,
    titelbild: Set<String>,
    kachelStand: Long,
): MediaItem? {
    val datei = tonDatei(lied, eigen, quelle) ?: return null
    val bild = medienUrl(basis, lied.id, bildDatei(lied, eigen, titelbild, kachel = false, kachelStand))
    return MediaItem.Builder()
        .setMediaId(lied.id)
        .setUri(medienUrl(basis, lied.id, datei))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(lied.titel)
                .setArtist(lied.anzeigename ?: lied.handle.orEmpty())
                .setArtworkUri(Uri.parse(bild))
                .build(),
        )
        .build()
}

fun Player.fehlertext(): String? {
    val f = playerError ?: return null
    return when (f.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> "Der Tresor ist gerade nicht erreichbar."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        -> "Diese Tondatei liegt nicht auf dem Tresor."
        else -> "Der Ton ließ sich nicht abspielen."
    }
}
