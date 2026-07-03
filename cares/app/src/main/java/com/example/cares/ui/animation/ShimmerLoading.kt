// file: ui/animation/ShimmerLoading.kt
package com.example.cares.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ================================ MODIFIER SHIMMER ================================

/**
 * Modifier che applica un effetto shimmer (brillamento) per indicare il caricamento.
 *
 * **Cos'è lo shimmer:**
 * Un effetto di animazione che simula un riflesso di luce che scorre su una superficie,
 * comunemente utilizzato per indicare che il contenuto è in fase di caricamento.
 *
 * **Caratteristiche:**
 * - Utilizza un gradiente lineare animato che si muove orizzontalmente.
 * - I colori sono configurati per creare un effetto di "brillamento" sottile.
 * - Supporta forme personalizzate tramite il parametro [shape].
 *
 * **Utilizzo tipico:**
 * ```
 * Box(modifier = Modifier.shimmer()) {
 *     // Contenuto placeholder
 * }
 * ```
 *
 * @param shape Forma del contenitore (default: 8dp arrotondati).
 * @param shimmerColor Colore del brillamento (default: bianco al 15% di trasparenza).
 * @param baseColor Colore di base del contenitore (default: bianco al 5% di trasparenza).
 * @return Il Modifier configurato con l'effetto shimmer.
 */
fun Modifier.shimmer(
    shape: Shape = RoundedCornerShape(8.dp),
    shimmerColor: Color = Color.White.copy(alpha = 0.15f),
    baseColor: Color = Color.White.copy(alpha = 0.05f)
): Modifier = composed {
    // Transizione infinita per l'animazione del brillamento
    val transition = rememberInfiniteTransition(label = "shimmer")

    // Posizione orizzontale del gradiente che si muove continuamente
    val translateX by transition.animateFloat(
        initialValue = -500f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = Animations.extraSlowTween()
        ),
        label = "shimmerX"
    )

    // Applica il gradiente animato come sfondo
    this.background(
        brush = Brush.linearGradient(
            colors = listOf(
                baseColor,
                shimmerColor,
                baseColor
            ),
            start = Offset(translateX, 0f),
            end = Offset(translateX + 300f, 0f)
        ),
        shape = shape
    )
}

// ================================ COMPONENTI SHIMMER ================================

/**
 * Shimmer per card di caricamento.
 *
 * **Utilizzo tipico:**
 * ```
 * ShimmerCard(
 *     modifier = Modifier.fillMaxWidth(),
 *     height = 150.dp
 * )
 * ```
 *
 * @param modifier Modificatori da applicare alla card.
 * @param height Altezza della card (default: 120dp).
 * @param shape Forma della card (default: 16dp arrotondati).
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    Box(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
            .shimmer(shape = shape)
            .clip(shape)
    )
}

/**
 * Shimmer per righe di lista.
 *
 * **Caratteristiche:**
 * - Simula una riga di lista con icona (opzionale) e due righe di testo.
 * - L'icona è circolare e ha una dimensione configurabile.
 * - Le righe di testo hanno lunghezze diverse per un aspetto più realistico.
 *
 * **Utilizzo tipico:**
 * ```
 * ShimmerListItem(
 *     hasIcon = true,
 *     iconSize = 48.dp
 * )
 * ```
 *
 * @param modifier Modificatori da applicare alla riga.
 * @param iconSize Dimensione dell'icona (default: 40dp).
 * @param hasIcon Se `true`, mostra un'icona circolare (default: true).
 */
@Composable
fun ShimmerListItem(
    modifier: Modifier = Modifier,
    iconSize: Dp = 40.dp,
    hasIcon: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        if (hasIcon) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .shimmer(shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Prima riga di testo (più lunga)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .shimmer()
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Seconda riga di testo (più corta)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .shimmer()
            )
        }
    }
}

/**
 * Shimmer per avatar.
 *
 * **Utilizzo tipico:**
 * ```
 * ShimmerAvatar(
 *     size = 80.dp
 * )
 * ```
 *
 * @param modifier Modificatori da applicare all'avatar.
 * @param size Dimensione dell'avatar (default: 60dp).
 */
@Composable
fun ShimmerAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .shimmer(shape = CircleShape)
    )
}

/**
 * Sostituisce un [CircularProgressIndicator] con un effetto shimmer.
 *
 * **Caratteristiche:**
 * - Mostra 3 righe di lista shimmer per simulare una lista in caricamento.
 * - Perfetto per stati di caricamento di liste e contenuti scrollabili.
 *
 * **Utilizzo tipico:**
 * ```
 * if (isLoading) {
 *     ShimmerLoader()
 * } else {
 *     MyContent()
 * }
 * ```
 *
 * @param modifier Modificatori da applicare al contenitore.
 * @param height Altezza totale del loader (default: 200dp).
 */
@Composable
fun ShimmerLoader(
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            ShimmerListItem()
        }
    }
}