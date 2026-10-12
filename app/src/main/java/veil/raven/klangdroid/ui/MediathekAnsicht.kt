package veil.raven.klangdroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import veil.raven.klangdroid.KlangModell
import veil.raven.klangdroid.data.Lied
import veil.raven.klangdroid.data.Playlist
import veil.raven.klangdroid.data.Sortierung
import veil.raven.klangdroid.data.Zeile
import veil.raven.klangdroid.data.bildDatei
import veil.raven.klangdroid.data.dauerText
import veil.raven.klangdroid.data.hatBewegtbild
import veil.raven.klangdroid.data.medienUrl
import veil.raven.klangdroid.data.playlistBildUrl
import veil.raven.klangdroid.data.playlistZeilen
import veil.raven.klangdroid.data.siebe
import veil.raven.klangdroid.data.tonDatei
import veil.raven.klangdroid.data.videoDatei

@Composable
fun MediathekAnsicht(
    modell: KlangModell,
    farben: KlangFarben,
    abstand: PaddingValues,
    spielId: String?,
    tonLaeuft: Boolean,
) {
    val index = modell.bestand.index
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(abstand)
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Wortmarke(farben)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { modell.neuLaden() }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Aktualisieren", tint = farben.text)
            }
            IconButton(onClick = { modell.einstellungenOffen = true }) {
                Icon(Icons.Rounded.Settings, contentDescription = "Einstellungen", tint = farben.text)
            }
        }
        val profil = index?.profil
        val name = profil?.display_name ?: profil?.handle
        Text(
            when {
                modell.bestand.laedt -> "Verbinde mit dem Tresor …"
                modell.bestand.fehler != null -> "Nicht verbunden"
                name != null -> "$name  ·  ${index?.songs.orEmpty().count { tonDatei(it, modell.bestand.eigen[it.id], modell.werte.value.ton) != null }} mit Ton"
                else -> "${index?.anzahl ?: 0} Titel"
            },
            color = farben.schwach,
            style = MaterialThemeBody(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = modell.suche,
            onValueChange = { modell.suche = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Titel, Name, Stil", color = farben.schwach) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = farben.schwach) },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            colors = feldFarben(farben),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WahlChip("Mit Ton", modell.nurTon, farben) { modell.nurTon = !modell.nurTon }
            WahlChip("Bewegtbild", modell.nurVideo, farben) { modell.nurVideo = !modell.nurVideo }
            WahlChip("Playlists", modell.playlistId == "" , farben) {
                modell.playlistId = if (modell.playlistId == "") null else ""
            }
        }
        Spacer(Modifier.height(4.dp))
        if (modell.playlistId != "") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sortierung.entries.forEach { art ->
                    WahlChip(art.label, modell.sortierung == art, farben) { modell.sortierung = art }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { modell.kacheln = !modell.kacheln }) {
                    Icon(
                        if (modell.kacheln) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                        contentDescription = if (modell.kacheln) "Listenansicht" else "Kachelansicht",
                        tint = farben.akzent,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        when {
            modell.bestand.laedt && index == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = farben.akzent)
            }
            modell.bestand.fehler != null && index == null -> Fehlerkasten(modell, farben)
            modell.playlistId == "" -> PlaylistListe(modell, farben, index?.playlists.orEmpty())
            else -> TitelRaster(modell, farben, spielId, tonLaeuft)
        }
    }
}

@Composable
private fun MaterialThemeBody() = androidx.compose.material3.MaterialTheme.typography.bodySmall

@Composable
private fun feldFarben(farben: KlangFarben) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = farben.text,
    unfocusedTextColor = farben.text,
    focusedBorderColor = farben.akzent,
    unfocusedBorderColor = farben.rand,
    focusedContainerColor = farben.flaeche,
    unfocusedContainerColor = farben.flaeche,
    cursorColor = farben.akzent,
)

@Composable
private fun WahlChip(text: String, an: Boolean, farben: KlangFarben, onClick: () -> Unit) {
    FilterChip(
        selected = an,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = farben.flaeche,
            labelColor = farben.schwach,
            selectedContainerColor = farben.akzent.copy(alpha = 0.22f),
            selectedLabelColor = farben.text,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = an,
            borderColor = farben.rand,
            selectedBorderColor = farben.akzent,
        ),
    )
}

@Composable
private fun Fehlerkasten(modell: KlangModell, farben: KlangFarben) {
    Column(Modifier.fillMaxSize().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Der Tresor antwortet nicht", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = farben.text)
        Text(modell.bestand.fehler.orEmpty(), color = farben.schwach)
        Text(
            "Voreingestellt ist http://localhost:8788/. Im Emulator ist das der Emulator selbst — mit adb reverse tcp:8788 tcp:8788 trifft die Adresse den Rechner, sonst http://10.0.2.2:8788/.",
            color = farben.schwach,
        )
        TextButton(onClick = { modell.einstellungenOffen = true }) {
            Text("Adresse ändern", color = farben.akzent)
        }
    }
}

@Composable
private fun TitelRaster(modell: KlangModell, farben: KlangFarben, spielId: String?, tonLaeuft: Boolean) {
    val index = modell.bestand.index ?: return
    val werte = modell.werte.value
    val titelbild = remember(index.titelbild) { index.titelbild.toSet() }
    val liste = siebe(
        index.songs, modell.bestand.eigen, modell.suche, modell.nurTon, modell.nurVideo,
        modell.papier, modell.sortierung, werte.ton,
    )
    if (liste.isEmpty()) {
        Text("Nichts passt auf diese Suche.", color = farben.schwach, modifier = Modifier.padding(top = 24.dp))
        return
    }
    if (!modell.kacheln) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 18.dp, top = 6.dp),
        ) {
            items(liste, key = { it.id }) { lied ->
                LiedZeile(modell, farben, lied, titelbild, liste, spielId == lied.id, tonLaeuft && spielId == lied.id)
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(158.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 18.dp, top = 6.dp),
    ) {
        items(liste, key = { it.id }) { lied ->
            LiedKarte(modell, farben, lied, titelbild, liste, spielId == lied.id, tonLaeuft && spielId == lied.id)
        }
    }
}

@Composable
private fun LiedKarte(
    modell: KlangModell,
    farben: KlangFarben,
    lied: Lied,
    titelbild: Set<String>,
    liste: List<Lied>,
    spielt: Boolean,
    videoLaeuft: Boolean,
) {
    val werte = modell.werte.value
    val eigen = modell.bestand.eigen[lied.id]
    val bild = medienUrl(
        werte.adresse, lied.id,
        bildDatei(lied, eigen, titelbild, kachel = true, modell.bestand.index?.kachelStand ?: 0),
    )
    Column(Modifier.clickable { liedAbspielen(modell, lied, liste) }) {
        val video = if (spielt && !modell.spielerOffen) {
            videoDatei(lied, eigen)?.let { medienUrl(werte.adresse, lied.id, it) }
        } else {
            null
        }
        Box {
            Titelbild(
                bild,
                lied.titel,
                farben,
                Modifier.fillMaxWidth().aspectRatio(1f),
                spielt,
                videoUrl = video,
                videoLaeuft = videoLaeuft,
            )
            if (hatBewegtbild(lied, eigen)) {
                Icon(
                    Icons.Rounded.Movie,
                    contentDescription = "Bewegtbild",
                    tint = farben.text,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(farben.bg.copy(alpha = 0.62f))
                        .padding(3.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(lied.titel, color = farben.text, maxLines = 2, overflow = TextOverflow.Ellipsis, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (spielt) Takte(farben.akzent) else Text(dauerText(lied.dauer), color = farben.schwach, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LiedZeile(
    modell: KlangModell,
    farben: KlangFarben,
    lied: Lied,
    titelbild: Set<String>,
    liste: List<Lied>,
    spielt: Boolean,
    videoLaeuft: Boolean,
) {
    val werte = modell.werte.value
    val eigen = modell.bestand.eigen[lied.id]
    val ton = tonDatei(lied, eigen, werte.ton)
    val bild = medienUrl(
        werte.adresse, lied.id,
        bildDatei(lied, eigen, titelbild, kachel = true, modell.bestand.index?.kachelStand ?: 0),
    )
    val video = if (spielt && !modell.spielerOffen) {
        videoDatei(lied, eigen)?.let { medienUrl(werte.adresse, lied.id, it) }
    } else {
        null
    }
    val meta = listOfNotNull(
        if (spielt) null else dauerText(lied.dauer),
        lied.erstellt?.take(10),
        lied.modell ?: lied.modellRoh,
    ).joinToString("  ·  ")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(farben.flaeche)
            .clickable { liedAbspielen(modell, lied, liste) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Titelbild(bild, lied.titel, farben, Modifier.size(72.dp), spielt, videoUrl = video, videoLaeuft = videoLaeuft)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                lied.titel,
                color = if (ton != null) farben.text else farben.schwach,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
            Text(
                lied.anzeigename ?: lied.handle.orEmpty(),
                color = farben.schwach,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (spielt) Takte(farben.akzent)
                if (meta.isNotBlank()) {
                    Text(meta, color = farben.schwach, maxLines = 1, overflow = TextOverflow.Ellipsis, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (hatBewegtbild(lied, eigen)) {
            Icon(Icons.Rounded.Movie, contentDescription = "Bewegtbild", tint = farben.schwach, modifier = Modifier.padding(start = 8.dp).size(18.dp))
        }
    }
}

private fun liedAbspielen(modell: KlangModell, lied: Lied, liste: List<Lied>) {
    val werte = modell.werte.value
    if (tonDatei(lied, modell.bestand.eigen[lied.id], werte.ton) == null) {
        modell.hinweis = "Für „${lied.titel}“ liegt lokal kein Ton."
        return
    }
    val spielbar = liste.filter { tonDatei(it, modell.bestand.eigen[it.id], werte.ton) != null }
    modell.abspielen(spielbar, spielbar.indexOfFirst { it.id == lied.id }.coerceAtLeast(0))
}

@Composable
private fun PlaylistListe(modell: KlangModell, farben: KlangFarben, playlists: List<Playlist>) {
    if (playlists.isEmpty()) {
        Text("Keine Playlists im Katalog.", color = farben.schwach, modifier = Modifier.padding(top = 24.dp))
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 18.dp, top = 6.dp),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(farben.flaeche)
                    .clickable { modell.playlistId = playlist.id }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Titelbild(
                    playlistBildUrl(modell.werte.value.adresse, playlist.id),
                    playlist.name,
                    farben,
                    Modifier.size(64.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(playlist.name, color = farben.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${playlist.eintraege.size} Titel", color = farben.schwach, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun PlaylistAnsicht(modell: KlangModell, farben: KlangFarben, abstand: PaddingValues, spielId: String?) {
    val index = modell.bestand.index
    val playlist = index?.playlists?.firstOrNull { it.id == modell.playlistId }
    Column(Modifier.fillMaxSize().padding(abstand).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { modell.playlistId = "" }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Zurück", tint = farben.text)
            }
            Column {
                Text(playlist?.name ?: "Playlist", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = farben.text)
                Text("${playlist?.eintraege?.size ?: 0} Titel", color = farben.schwach, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
        if (playlist == null || index == null) return
        val nachId = remember(index.songs) { index.songs.associateBy { it.id } }
        val zeilen = playlistZeilen(playlist, nachId, modell.bestand.eigen, modell.werte.value.ton)
        val spielbar = zeilen.mapNotNull { (it as? Zeile.Bekannt)?.takeIf { zeile -> zeile.ton != null }?.lied }
        LazyColumn(contentPadding = PaddingValues(bottom = 18.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(zeilen, key = { it.id + it.titel }) { zeile ->
                val bekannt = zeile as? Zeile.Bekannt
                val kann = bekannt?.ton != null
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (!kann || bekannt == null) {
                                modell.hinweis = if (bekannt == null) "Liegt nicht im lokalen Archiv." else "Für diesen Titel liegt lokal kein Ton."
                            } else {
                                modell.abspielen(spielbar, spielbar.indexOfFirst { it.id == bekannt.id }.coerceAtLeast(0))
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val bild = if (bekannt != null) {
                        medienUrl(
                            modell.werte.value.adresse,
                            bekannt.id,
                            bildDatei(bekannt.lied, modell.bestand.eigen[bekannt.id], index.titelbild.toSet(), true, index.kachelStand),
                        )
                    } else null
                    Titelbild(bild, zeile.titel, farben, Modifier.size(52.dp), spielId == zeile.id)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(zeile.titel, color = if (kann) farben.text else farben.schwach, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            bekannt?.lied?.anzeigename ?: (zeile as? Zeile.Fremd)?.name ?: "",
                            color = farben.schwach,
                            maxLines = 1,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (spielId == zeile.id) Takte(farben.akzent)
                    else if (bekannt != null && hatBewegtbild(bekannt.lied, modell.bestand.eigen[bekannt.id])) {
                        Icon(Icons.Rounded.Movie, contentDescription = "Bewegtbild", tint = farben.schwach, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
