package veil.raven.klangdroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import veil.raven.klangdroid.KlangModell

@Composable
fun KlangDroidApp(spielerSignal: Int, modell: KlangModell = viewModel()) {
    val werte by modell.werte.collectAsState()
    val stand by modell.spieler.zustand.collectAsState()
    val lied = modell.lied(stand.mediaId)
    val ziel = mitAkzent(
        themaFarben(werte.thema, if (werte.thema == "cover") lied?.farben else null),
        werte.akzent,
    )
    val farben = animierteFarben(ziel)
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(spielerSignal) {
        if (spielerSignal > 0) modell.spielerOffen = true
    }
    LaunchedEffect(modell.hinweis) {
        val text = modell.hinweis ?: return@LaunchedEffect
        snack.showSnackbar(text)
        modell.hinweis = null
    }

    BackHandler(modell.einstellungenOffen || modell.spielerOffen || modell.textblatt != null || modell.playlistId != null) {
        when {
            modell.textblatt != null -> modell.textblatt = null
            modell.einstellungenOffen -> modell.einstellungenOffen = false
            modell.spielerOffen -> modell.spielerOffen = false
            modell.playlistId != null && modell.playlistId != "" -> modell.playlistId = ""
            modell.playlistId == "" -> modell.playlistId = null
        }
    }

    KlangThema(farben) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = farben.bg,
                snackbarHost = { SnackbarHost(snack) },
                bottomBar = {
                    if (stand.mediaId != null && !modell.spielerOffen) {
                        MiniSpieler(modell, farben, stand) { modell.spielerOffen = true }
                    }
                },
            ) { abstand ->
                when {
                    modell.einstellungenOffen -> EinstellungenAnsicht(modell, farben, abstand)
                    modell.playlistId != null && modell.playlistId != "" -> PlaylistAnsicht(modell, farben, abstand, stand.mediaId)
                    else -> MediathekAnsicht(modell, farben, abstand, stand.mediaId, stand.spielt)
                }
            }
            AnimatedVisibility(
                visible = modell.spielerOffen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                SpielerAnsicht(modell, farben, stand)
            }
            if (modell.textblatt != null) TextblattAnsicht(modell, farben)
        }
    }
}
