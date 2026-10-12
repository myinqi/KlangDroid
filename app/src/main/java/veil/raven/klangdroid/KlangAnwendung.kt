package veil.raven.klangdroid

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import veil.raven.klangdroid.data.Einstellungen
import veil.raven.klangdroid.player.SpielerZentrale

class KlangAnwendung : Application(), SingletonImageLoader.Factory {
    lateinit var einstellungen: Einstellungen
        private set
    lateinit var spieler: SpielerZentrale
        private set

    override fun onCreate() {
        super.onCreate()
        einstellungen = Einstellungen(this)
        spieler = SpielerZentrale(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
}
