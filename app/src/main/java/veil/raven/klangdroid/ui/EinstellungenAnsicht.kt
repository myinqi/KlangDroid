package veil.raven.klangdroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import veil.raven.klangdroid.KlangModell
import veil.raven.klangdroid.data.STANDARD_ADRESSE
import veil.raven.klangdroid.data.Tonquelle
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EinstellungenAnsicht(modell: KlangModell, farben: KlangFarben, abstand: androidx.compose.foundation.layout.PaddingValues) {
    val werte = modell.werte.value
    var adresse by remember(werte.adresse) { mutableStateOf(werte.adresse) }
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(abstand)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { modell.einstellungenOffen = false }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Zurück", tint = farben.text)
            }
            Text("Einstellungen", style = MaterialTheme.typography.titleLarge, color = farben.text)
        }
        Text("Adresse", color = farben.text, style = MaterialTheme.typography.titleMedium)
        Text(
            "Dort läuft der KlangTresor. Voreingestellt ist $STANDARD_ADRESSE. Im Heimnetz die Adresse, die er beim Start nennt. Im Emulator ist localhost der Emulator selbst: adb reverse tcp:8788 tcp:8788, oder http://10.0.2.2:8788/.",
            color = farben.schwach,
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = adresse,
            onValueChange = { adresse = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                modell.verbinden(adresse)
                focus.clearFocus()
            }),
            shape = RoundedCornerShape(16.dp),
        )
        Button(
            onClick = { modell.verbinden(adresse); focus.clearFocus() },
            colors = ButtonDefaults.buttonColors(containerColor = farben.akzent, contentColor = MaterialTheme.colorScheme.onPrimary),
        ) { Text("Verbinden") }
        val stand = when {
            modell.bestand.laedt -> "Verbinde …"
            modell.bestand.fehler != null -> modell.bestand.fehler
            modell.bestand.index != null -> "Verbunden · KlangTresor ${modell.bestand.index?.version ?: ""} · ${modell.bestand.index?.anzahl ?: 0} Titel"
            else -> null
        }
        if (stand != null) Text(stand, color = if (modell.bestand.fehler != null) farben.akzent2 else farben.schwach, style = MaterialTheme.typography.bodySmall)

        Text("Ton", color = farben.text, style = MaterialTheme.typography.titleMedium)
        Text(
            "MP3 ist auf dem Handy die richtige Voreinstellung. WAV und eine eigene Fassung werden nur genommen, wenn sie beim Titel liegen.",
            color = farben.schwach,
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tonquelle.entries.forEach { quelle ->
                FilterChip(
                    selected = werte.ton == quelle,
                    onClick = { modell.ton(quelle) },
                    label = { Text(quelle.label) },
                )
            }
        }

        Text("Erscheinung", color = farben.text, style = MaterialTheme.typography.titleMedium)
        Text(
            "Dieselben Welten wie im KlangTresor. Rabe legt Violett über alles. Cover färbt die Fläche nach dem laufenden Stück. Die übrigen Paletten stammen aus dem Klangraum.",
            color = farben.schwach,
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            THEMEN.forEach { thema ->
                val an = werte.thema == thema.id
                Column(
                    Modifier
                        .width(156.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(thema.probe.flaeche)
                        .border(if (an) 2.dp else 1.dp, if (an) farben.akzent else thema.probe.rand, RoundedCornerShape(16.dp))
                        .clickable { modell.thema(thema.id) }
                        .padding(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(thema.probe.bg, thema.probe.flaeche2, thema.probe.akzent, thema.probe.akzent2).forEach { farbe ->
                            Box(Modifier.size(14.dp).clip(CircleShape).background(farbe))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(thema.name, color = thema.probe.text, style = MaterialTheme.typography.titleMedium)
                    Text(thema.satz, color = thema.probe.schwach, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("Akzent", color = farben.text, style = MaterialTheme.typography.titleMedium)
        Text(
            "Automatisch übernimmt den Akzent des Themas. Eine eigene Farbe legt sich darüber, die Flächen bleiben.",
            color = farben.schwach,
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AKZENT_WAHL.forEach { (name, hex) ->
                val an = werte.akzent.equals(hex, ignoreCase = true)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (an) farben.flaeche2 else Color.Transparent)
                        .border(1.dp, if (an) farben.akzent else farben.rand, RoundedCornerShape(20.dp))
                        .clickable { modell.akzent(hex) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val punkt = hexZuFarbe(hex) ?: farben.schwach
                    Box(Modifier.size(12.dp).clip(CircleShape).background(punkt))
                    Spacer(Modifier.width(6.dp))
                    Text(name, color = farben.text, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        var farbton by remember(werte.akzent) {
            mutableFloatStateOf(hexZuHue(werte.akzent).takeIf { werte.akzent.isNotBlank() } ?: 280f)
        }
        androidx.compose.material3.Slider(
            value = farbton,
            onValueChange = { farbton = it },
            onValueChangeFinished = { modell.akzent(hueZuHex(farbton)) },
            valueRange = 0f..360f,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = farben.akzent,
                activeTrackColor = farben.akzent,
                inactiveTrackColor = farben.rand,
            ),
        )
        Text("Eigener Farbton", color = farben.schwach, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(8.dp))
        Text("KlangDroid von Tarja Ravenveil", color = farben.text, style = MaterialTheme.typography.bodyMedium)
        Text("für den KlangTresor von Caspar David", color = farben.schwach, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
    }
}

private fun hexZuHue(hex: String): Float {
    val farbe = hexZuFarbe(hex) ?: return 280f
    val r = farbe.red
    val g = farbe.green
    val b = farbe.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    if (d == 0f) return 0f
    val h = when (max) {
        r -> ((g - b) / d) % 6f
        g -> ((b - r) / d) + 2f
        else -> ((r - g) / d) + 4f
    }
    return (h * 60f).let { if (it < 0) it + 360f else it }
}

private fun hueZuHex(hue: Float): String {
    val farbe = Color.hsv(hue, 0.72f, 0.96f)
    val r = (farbe.red * 255).roundToInt().coerceIn(0, 255)
    val g = (farbe.green * 255).roundToInt().coerceIn(0, 255)
    val b = (farbe.blue * 255).roundToInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}
