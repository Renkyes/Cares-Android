// file: ui/animation/PageTransitions.kt
package com.example.cares.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntOffset

/**
 * Configurazione centralizzata delle transizioni di pagina.
 *
 * Fornisce specifiche di animazione coerenti per tutte le transizioni
 * tra schermate dell'applicazione, utilizzate da NavHost.
 *
 * **Tipi di transizione:**
 * - **Navigazione normale:** la nuova pagina entra da sinistra, la vecchia esce a destra.
 * - **Pop (tornare indietro):** la nuova pagina entra da destra, la vecchia esce a sinistra.
 *
 * **Tempi di animazione:**
 * - **Entrata:** 350ms (più lenta per un effetto di ingresso morbido)
 * - **Uscita:** 250ms (più veloce per non rallentare la navigazione)
 *
 * **Easing:**
 * - [FastOutSlowInEasing] per un'accelerazione e decelerazione naturali.
 *
 * @see AnimatedContentTransitionScope per la documentazione delle transizioni.
 */
object PageTransitions {

    // ================================ SPECIFICHE DI ANIMAZIONE ================================

    /**
     * Specifica per lo scorrimento in entrata (IntOffset).
     * Durata: 350ms, easing FastOutSlowIn.
     */
    private val enterSlide = tween<IntOffset>(
        durationMillis = 350,
        easing = FastOutSlowInEasing
    )

    /**
     * Specifica per lo scorrimento in uscita (IntOffset).
     * Durata: 250ms, easing FastOutSlowIn.
     */
    private val exitSlide = tween<IntOffset>(
        durationMillis = 250,
        easing = FastOutSlowInEasing
    )

    /**
     * Specifica per la trasparenza in entrata (Float).
     * Durata: 350ms, easing FastOutSlowIn.
     */
    private val enterFade = tween<Float>(
        durationMillis = 350,
        easing = FastOutSlowInEasing
    )

    /**
     * Specifica per la trasparenza in uscita (Float).
     * Durata: 250ms, easing FastOutSlowIn.
     */
    private val exitFade = tween<Float>(
        durationMillis = 250,
        easing = FastOutSlowInEasing
    )

    // ================================ TRANSIZIONI DI NAVIGAZIONE ================================

    /**
     * Transizione di entrata per la navigazione normale.
     *
     * **Effetti:**
     * - La nuova pagina scivola da sinistra verso destra.
     * - Fade in simultaneo per una transizione morbida.
     *
     * **Utilizzo:**
     * ```
     * NavHost(
     *     enterTransition = PageTransitions.enterTransition,
     *     exitTransition = PageTransitions.exitTransition,
     *     ...
     * )
     * ```
     */
    val enterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = enterSlide
        ) + fadeIn(animationSpec = enterFade)
    }

    /**
     * Transizione di uscita per la navigazione normale.
     *
     * **Effetti:**
     * - La vecchia pagina scivola verso destra.
     * - Fade out simultaneo.
     */
    val exitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = exitSlide
        ) + fadeOut(animationSpec = exitFade)
    }

    /**
     * Transizione di entrata per il pop (tornare indietro).
     *
     * **Effetti:**
     * - La nuova pagina scivola da destra verso sinistra.
     * - Fade in simultaneo.
     *
     * **Nota:** La direzione è invertita rispetto a [enterTransition]
     * per dare all'utente la sensazione di "tornare indietro".
     */
    val popEnterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = enterSlide
        ) + fadeIn(animationSpec = enterFade)
    }

    /**
     * Transizione di uscita per il pop (tornare indietro).
     *
     * **Effetti:**
     * - La vecchia pagina scivola verso sinistra.
     * - Fade out simultaneo.
     *
     * **Nota:** La direzione è invertita rispetto a [exitTransition]
     * per coerenza con il gesto di "tornare indietro".
     */
    val popExitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = exitSlide
        ) + fadeOut(animationSpec = exitFade)
    }
}