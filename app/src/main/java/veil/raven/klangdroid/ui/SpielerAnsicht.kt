package veil.raven.klangdroid.ui

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import veil.raven.klangdroid.KlangModell
import veil.raven.klangdroid.R
import veil.raven.klangdroid.data.bildDatei
import veil.raven.klangdroid.data.dauerText
import veil.raven.klangdroid.data.medienUrl
import veil.raven.klangdroid.data.videoDatei
import veil.raven.klangdroid.data.zeitText
import veil.raven.klangdroid.player.SpielerZustand

@Composable
fun MiniSpieler(modell: KlangModell, farben: KlangFarben, stand: SpielerZustand, onOeffnen: () -> Unit) {
    val lied = modell.lied(stand.mediaId)
    val anteil = if (stand.dauerMs > 0) stand.positionMs.toFloat() / stand.dauerMs else 0f
    Column(
        Modifier
            .fillMaxWidth()
            .background(farben.flaeche)
            .clickable(onClick = onOeffnen),
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(farben.rand)) {
            Box(Modifier.fillMaxWidth(anteil.coerceIn(0f, 1f)).height(2.dp).background(farben.akzent))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bild = lied?.let {
                medienUrl(
                    modell.werte.value.adresse, it.id,
                    bildDatei(it, modell.bestand.eigen[it.id], modell.bestand.index?.titelbild?.toSet().orEmpty(), true, modell.bestand.index?.kachelStand ?: 0),
                )
            }
            Titelbild(bild, stand.titel ?: "", farben, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stand.titel ?: "", color = farben.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stand.kuenstler.orEmpty(), color = farben.schwach, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
            if (stand.puffert) {
                CircularProgressIndicator(Modifier.size(28.dp), color = farben.akzent, strokeWidth = 2.dp)
            }
            IconButton(onClick = { modell.spieler.umschalten() }) {
                Icon(
                    if (stand.spielt) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (stand.spielt) "Pause" else "Abspielen",
                    tint = farben.akzent,
                )
            }
        }
    }
}

@Composable
fun SpielerAnsicht(modell: KlangModell, farben: KlangFarben, stand: SpielerZustand) {
    val lied = modell.lied(stand.mediaId)
    val basis = modell.werte.value.adresse
    val eigen = lied?.let { modell.bestand.eigen[it.id] }
    val titelbild = modell.bestand.index?.titelbild?.toSet().orEmpty()
    val kachelStand = modell.bestand.index?.kachelStand ?: 0
    val poster = lied?.let { medienUrl(basis, it.id, bildDatei(it, eigen, titelbild, false, kachelStand)) }
    val video = lied?.let { videoDatei(it, eigen)?.let { datei -> medienUrl(basis, lied.id, datei) } }
    Box(Modifier.fillMaxSize().background(farben.bg)) {
        if (poster != null) {
            AsyncImage(
                model = poster,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.38f,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(farben.bg.copy(alpha = 0.25f), farben.bg.copy(alpha = 0.92f))),
            ),
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { modell.spielerOffen = false }) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Schließen", tint = farben.text)
                }
                Spacer(Modifier.weight(1f))
                if (lied?.hatLyrics == true) {
                    IconButton(onClick = { modell.textOeffnen(lied) }) {
                        Icon(Icons.Rounded.Lyrics, contentDescription = "Text", tint = farben.text)
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .align(Alignment.Center),
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(listOf(farben.akzent.copy(alpha = 0.45f), Color.Transparent)),
                                ),
                        )
                        Bewegtbild(
                            videoUrl = video,
                            poster = poster,
                            spielt = stand.spielt,
                            farben = farben,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(28.dp))
                                .clickable { modell.spieler.umschalten() },
                        )
                        if (!stand.spielt) {
                            Box(
                                Modifier
                                    .align(Alignment.Center)
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.38f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                            }
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(stand.titel ?: lied?.titel.orEmpty(), style = MaterialTheme.typography.headlineMedium, color = farben.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stand.kuenstler ?: lied?.anzeigename.orEmpty(), color = farben.schwach, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(
                    lied?.erstellt?.take(10),
                    lied?.modell ?: lied?.modellRoh,
                    if (lied == null) null else if (lied.oeffentlich) "öffentlich" else "privat",
                    if (video != null) "Bewegtbild" else null,
                ).joinToString("  ·  ")
                if (meta.isNotEmpty()) {
                    Text(meta, color = farben.schwach, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
                stand.fehler?.let {
                    Text(it, color = farben.akzent2, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
                Fortschritt(modell, farben, stand)
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { modell.spieler.zufall(!stand.zufall) }) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = "Zufall", tint = if (stand.zufall) farben.akzent else farben.schwach)
                    }
                    IconButton(onClick = { modell.spieler.zurueck() }) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Vorheriger Titel", tint = farben.text, modifier = Modifier.size(36.dp))
                    }
                    RunderKnopf(farben, true, if (stand.spielt) "Pause" else "Abspielen", { modell.spieler.umschalten() }) {
                        if (stand.puffert) {
                            CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                if (stand.spielt) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                    IconButton(onClick = { modell.spieler.weiter() }) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Nächster Titel", tint = farben.text, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = { modell.spieler.wiederholenZyklus() }) {
                        Icon(
                            if (stand.wiederholen == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Wiederholen",
                            tint = if (stand.wiederholen == Player.REPEAT_MODE_OFF) farben.schwach else farben.akzent,
                        )
                    }
                }
                val toene = lied?.farben?.toene?.mapNotNull { it.hex }?.distinct()?.take(6).orEmpty()
                if (toene.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.Center) {
                        toene.forEach { hex ->
                            val farbe = hexZuFarbe(hex) ?: return@forEach
                            Box(Modifier.padding(horizontal = 4.dp).size(10.dp).clip(CircleShape).background(farbe))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Fortschritt(modell: KlangModell, farben: KlangFarben, stand: SpielerZustand) {
    var zieht by remember { mutableStateOf(false) }
    var ziehPos by remember { mutableFloatStateOf(0f) }
    val anteil = if (zieht) ziehPos else if (stand.dauerMs > 0) stand.positionMs.toFloat() / stand.dauerMs else 0f
    Column {
        Slider(
            value = anteil.coerceIn(0f, 1f),
            onValueChange = { zieht = true; ziehPos = it },
            onValueChangeFinished = {
                modell.spieler.suchen((ziehPos * stand.dauerMs).toLong())
                zieht = false
            },
            colors = SliderDefaults.colors(
                thumbColor = farben.akzent,
                activeTrackColor = farben.akzent,
                inactiveTrackColor = farben.rand,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(zeitText(if (zieht) (ziehPos * stand.dauerMs).toLong() else stand.positionMs), color = farben.schwach, style = MaterialTheme.typography.bodySmall)
            Text(if (stand.dauerMs > 0) zeitText(stand.dauerMs) else dauerText(modell.lied(stand.mediaId)?.dauer), color = farben.schwach, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun Bewegtbild(
    videoUrl: String?,
    poster: String?,
    spielt: Boolean,
    farben: KlangFarben,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var kaputt by remember(videoUrl) { mutableStateOf(false) }
    val exo = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
        }
    }
    DisposableEffect(exo) {
        val lauscher = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                kaputt = true
            }
        }
        exo.addListener(lauscher)
        onDispose {
            exo.removeListener(lauscher)
            exo.release()
        }
    }
    LaunchedEffect(videoUrl, spielt, kaputt) {
        if (videoUrl == null || kaputt) {
            exo.pause()
            return@LaunchedEffect
        }
        val aktuell = exo.currentMediaItem?.localConfiguration?.uri?.toString()
        if (aktuell != videoUrl) {
            exo.setMediaItem(MediaItem.fromUri(videoUrl))
            exo.prepare()
        }
        if (spielt) exo.play() else exo.pause()
    }
    Box(modifier.background(farben.flaeche)) {
        if (poster != null) {
            AsyncImage(model = poster, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (videoUrl != null && !kaputt) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.bewegtbild, FrameLayout(ctx), false) as PlayerView).apply {
                        player = exo
                        useController = false
                        isClickable = false
                        isFocusable = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { it.player = exo },
            )
        }
    }
}

@Composable
fun TextblattAnsicht(modell: KlangModell, farben: KlangFarben) {
    val blatt = modell.textblatt ?: return
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { modell.textblatt = null },
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxSize(0.78f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(farben.flaeche)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Text", style = MaterialTheme.typography.titleLarge, color = farben.text, modifier = Modifier.weight(1f))
                IconButton(onClick = { modell.textblatt = null }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Schließen", tint = farben.text)
                }
            }
            when {
                blatt.laedt -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = farben.akzent)
                }
                else -> androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize()) {
                    blatt.fehler?.let { fehler ->
                        item { Text(fehler, color = farben.akzent2) }
                    }
                    blatt.stil?.takeIf { it.isNotBlank() }?.let { stil ->
                        item {
                            Text("Stil", color = farben.akzent, style = MaterialTheme.typography.labelLarge)
                            Text(stil, color = farben.schwach, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                        }
                    }
                    item {
                        Text(
                            blatt.lyrics?.takeIf { it.isNotBlank() } ?: "Kein Text hinterlegt.",
                            color = farben.text,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
