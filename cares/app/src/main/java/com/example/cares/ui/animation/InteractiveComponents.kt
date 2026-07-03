// file: ui/animation/InteractiveComponents.kt
package com.example.cares.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ================================ MODIFIER CON ANIMAZIONE ================================

/**
 * Modifier che aggiunge un effetto di scala al press (touch feedback).
 *
 * **Caratteristiche:**
 * - Riduce la scala dell'elemento quando viene premuto.
 * - Utilizza una spring media per un'animazione fluida e naturale.
 * - Non include il ripple di Material Design (sostituito dall'effetto scala).
 *
 * **Utilizzo tipico:**
 * ```
 * Box(modifier = Modifier.scaleOnPress()) {
 *     Icon(Icons.Default.Star, contentDescription = null)
 * }
 * ```
 *
 * @param scaleFactor Fattore di scala durante la pressione (default: 0.95f).
 * @return Il Modifier configurato con l'animazione di scala.
 */
fun Modifier.scaleOnPress(
    scaleFactor: Float = 0.95f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleFactor else 1f,
        animationSpec = Animations.springMedium(),
        label = "scaleOnPress"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null // Rimosso ripple perché usiamo l'effetto scala
        ) { /* No-op, il clickable è per l'interazione */ }
}

/**
 * Modifier per click con feedback visivo completo (scala + ripple).
 *
 * **Caratteristiche:**
 * - Combina l'effetto scala con il ripple di Material Design.
 * - Utilizza una spring media per un'animazione fluida.
 * - Da preferire a [scaleOnPress] per elementi che richiedono feedback tattile completo.
 *
 * **Utilizzo tipico:**
 * ```
 * Box(modifier = Modifier.animatedClickable { handleClick() }) {
 *     Text("Cliccami")
 * }
 * ```
 *
 * @param onClick Callback eseguito al click.
 * @param scaleFactor Fattore di scala durante la pressione (default: 0.95f).
 * @return Il Modifier configurato con l'animazione di scala e il ripple.
 */
fun Modifier.animatedClickable(
    onClick: () -> Unit,
    scaleFactor: Float = 0.95f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleFactor else 1f,
        animationSpec = Animations.springMedium(),
        label = "animatedClickable"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current
        ) { onClick() }
}

// ================================ COMPONENTI INTERATTIVI ================================

/**
 * Pulsante con animazione di rimbalzo al click.
 *
 * **Caratteristiche:**
 * - Effetto di scala al press (0.95x).
 * - Elevazione che si riduce al press.
 * - Spring media per un'animazione naturale.
 * - Alternativa elegante al Button standard con feedback visivo migliorato.
 *
 * **Utilizzo tipico:**
 * ```
 * AnimatedButton(
 *     onClick = { /* Azione */ },
 *     colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
 * ) {
 *     Text("Cliccami")
 * }
 * ```
 *
 * @param onClick Callback eseguito al click.
 * @param modifier Modificatori da applicare al pulsante.
 * @param enabled Se `true`, il pulsante è interattivo.
 * @param shape Forma del pulsante (default: 12dp arrotondati).
 * @param colors Colori del pulsante (default: trasparente con testo bianco).
 * @param contentPadding Padding interno del contenuto (default: 16dp orizzontale, 12dp verticale).
 * @param content Contenuto del pulsante.
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = Color.White
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animazione di scala al press
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            else -> 1f
        },
        animationSpec = Animations.springMedium(),
        label = "buttonScale"
    )

    // Animazione di elevazione al press
    val elevation by animateDpAsState(
        targetValue = when {
            isPressed -> 0.dp
            else -> 4.dp
        },
        animationSpec = tween(100),
        label = "buttonElevation"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.scale(scale),
        colors = colors,
        shape = shape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = elevation,
            pressedElevation = 0.dp
        ),
        interactionSource = interactionSource,
        contentPadding = contentPadding
    ) {
        content()
    }
}

/**
 * Card interattiva con animazione al press e all'hover.
 *
 * **Caratteristiche:**
 * - Effetto di scala al press (0.98x).
 * - Elevazione che si riduce al press.
 * - Sfondo semitrasparente per effetto glassmorphism.
 * - Clickable con feedback completo (scala + ripple).
 *
 * **Utilizzo tipico:**
 * ```
 * InteractiveCard(
 *     modifier = Modifier.fillMaxWidth(),
 *     onClick = { /* Naviga ai dettagli */ }
 * ) {
 *     Text("Contenuto della card")
 * }
 * ```
 *
 * @param modifier Modificatori da applicare alla card.
 * @param shape Forma della card (default: 16dp arrotondati).
 * @param elevation Elevazione base della card (default: 4dp).
 * @param onClick Callback eseguito al click.
 * @param content Contenuto della card.
 */
@Composable
fun InteractiveCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 4.dp,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animazione di scala al press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = Animations.springMedium(),
        label = "cardScale"
    )

    // Animazione di elevazione al press
    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) elevation * 0.5f else elevation,
        animationSpec = tween(100),
        label = "cardElevation"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .animatedClickable(onClick = onClick, scaleFactor = 0.98f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = animatedElevation
        ),
        shape = shape
    ) {
        content()
    }
}

// ================================ ANIMAZIONI DI ENTRATA ================================

/**
 * Fade-in con slide-up per gli elementi che appaiono.
 *
 * **Caratteristiche:**
 * - Animazione di fade in + slide up dall'alto.
 * - Supporto per delay personalizzato.
 * - Utilizza [Animations.standardTween] per un'animazione fluida.
 *
 * **Utilizzo tipico:**
 * ```
 * AnimatedEntrance(delay = 200) {
 *     Text("Questo appare con un delay")
 * }
 * ```
 *
 * @param modifier Modificatori da applicare al contenitore.
 * @param delay Ritardo in millisecondi prima dell'inizio dell'animazione.
 * @param content Contenuto da animare.
 */
@Composable
fun AnimatedEntrance(
    modifier: Modifier = Modifier,
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    val transition = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }

    // Se il delay è > 0, l'animazione inizia dopo il delay
    LaunchedEffect(Unit) {
        if (delay > 0) {
            kotlinx.coroutines.delay(delay.toLong())
            transition.targetState = true
        }
    }

    AnimatedVisibility(
        visibleState = transition,
        enter = fadeIn(
            animationSpec = Animations.standardTween()
        ) + slideInVertically(
            animationSpec = Animations.standardTween(),
            initialOffsetY = { it / 4 }
        ),
        exit = fadeOut() + slideOutVertically()
    ) {
        content()
    }
}

/**
 * Staggered entrance per liste di elementi.
 *
 * **Caratteristiche:**
 * - Applica un delay progressivo per ogni elemento (80ms di differenza).
 * - Ogni elemento viene animato con [AnimatedEntrance].
 *
 * **Utilizzo tipico:**
 * ```
 * StaggeredList(items = myList) { item, index ->
 *     Text("Elemento $index: $item")
 * }
 * ```
 *
 * @param items Lista di elementi da animare.
 * @param key Funzione per generare la chiave univoca per ogni elemento.
 * @param content Composable per visualizzare ogni elemento con il suo indice.
 */
@Composable
fun StaggeredList(
    items: List<Any>,
    key: (Any) -> Any = { it },
    content: @Composable (Any, Int) -> Unit
) {
    items.forEachIndexed { index, item ->
        AnimatedEntrance(
            delay = index * 80
        ) {
            content(item, index)
        }
    }
}

// ================================ PULSANTE NEON ================================

/**
 * Pulsante con effetto glow (pulsante neon).
 *
 * **Caratteristiche:**
 * - Effetto glow luminoso che si intensifica al press.
 * - Sfondo semitrasparente con il colore neon.
 * - Animazione di scala al press (0.95x).
 *
 * **Utilizzo tipico:**
 * ```
 * NeonButton(
 *     onClick = { /* Azione */ },
 *     text = "Attiva Boost",
 *     glowColor = Neon.Green
 * )
 * ```
 *
 * @param onClick Callback eseguito al click.
 * @param modifier Modificatori da applicare al pulsante.
 * @param enabled Se `true`, il pulsante è interattivo.
 * @param text Testo del pulsante.
 * @param glowColor Colore del glow (default: verde neon #00E676).
 */
@Composable
fun NeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    glowColor: Color = Color(0xFF00E676)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animazione dell'intensità del glow
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 0.2f,
        animationSpec = tween(200),
        label = "glowAlpha"
    )

    // Animazione di scala al press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "neonScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    radius = 100f
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = glowColor.copy(alpha = 0.1f),
            contentColor = glowColor
        ),
        shape = RoundedCornerShape(12.dp),
        interactionSource = interactionSource
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}