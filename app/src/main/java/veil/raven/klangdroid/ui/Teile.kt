package veil.raven.klangdroid.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import coil3.compose.AsyncImage
import veil.raven.klangdroid.R

@Composable
fun Wortmarke(farben: KlangFarben) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_rabe),
            contentDescription = "Rabe",
            tint = Color.White,
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(listOf(farben.akzent, farben.akzent2)),
                        blendMode = BlendMode.SrcIn,
                    )
                },
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "KlangDroid",
                style = MaterialTheme.typography.headlineMedium.copy(
                    brush = Brush.linearGradient(listOf(farben.akzent, farben.akzent2)),
                ),
            )
            Text(
                "für den KlangTresor",
                style = MaterialTheme.typography.bodySmall,
                color = farben.schwach,
            )
        }
    }
}

@Composable
fun Titelbild(
    url: String?,
    beschreibung: String,
    farben: KlangFarben,
    modifier: Modifier = Modifier,
    spielt: Boolean = false,
    videoUrl: String? = null,
    videoLaeuft: Boolean = false,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(farben.flaeche2)
            .then(
                if (spielt) Modifier.border(2.dp, farben.akzent, RoundedCornerShape(18.dp))
                else Modifier.border(1.dp, farben.rand.copy(alpha = 0.7f), RoundedCornerShape(18.dp)),
            )
            .semantics { contentDescription = beschreibung },
    ) {
        if (videoUrl != null) {
            Bewegtbild(
                videoUrl = videoUrl,
                poster = url,
                spielt = videoLaeuft,
                farben = farben,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun Takte(farbe: Color, modifier: Modifier = Modifier) {
    val lauf = rememberInfiniteTransition(label = "takt")
    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(0, 140, 280).forEach { versatz ->
            val hoehe by lauf.animateFloat(
                initialValue = 0.28f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(520, delayMillis = versatz, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "balken$versatz",
            )
            Box(
                Modifier
                    .width(3.dp)
                    .height((4 + 10 * hoehe).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(farbe),
            )
        }
    }
}

@Composable
fun RunderKnopf(
    farben: KlangFarben,
    gefuellt: Boolean,
    beschreibung: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inhalt: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(if (gefuellt) 72.dp else 48.dp)
            .clip(CircleShape)
            .background(if (gefuellt) farben.akzent else farben.flaeche2)
            .semantics { contentDescription = beschreibung }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        inhalt()
    }
}
