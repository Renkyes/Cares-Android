// file: ui/animation/Animations.kt
package com.example.cares.ui.animation

import androidx.compose.animation.core.*

/**
 * Specifiche di animazione condivise per tutta l'app.
 *
 * Centralizza i parametri di animazione per garantire coerenza visiva
 * e prestazioni ottimizzate in tutta l'applicazione.
 *
 * **Tipi di animazione:**
 * - **Spring:** Animazioni con effetto molla/rimbalzo, ideali per transizioni naturali.
 * - **Tween:** Animazioni lineari con easing, ideali per transizioni fluide e prevedibili.
 *
 * **Linee guida per l'utilizzo:**
 * - Usare [springLight] per animazioni morbide di elementi UI (card, espansioni).
 * - Usare [springMedium] per animazioni con rimbalzo moderato.
 * - Usare [springStiff] per animazioni rapide con poco rimbalzo.
 * - Usare [standardTween] per la maggior parte delle transizioni UI.
 * - Usare [fastTween] per micro-interazioni (click, toggle).
 * - Usare [slowTween] per transizioni di schermo e apparizioni.
 * - Usare [extraSlowTween] per animazioni di onboarding o intro.
 * - Usare [overshootTween] per effetti di rimbalzo controllato.
 *
 * @see Animatable per l'animazione di valori singoli.
 * @see Transition per l'animazione tra stati.
 */
object Animations {

    // ================================ SPRING ================================

    /**
     * Spring leggero per animazioni morbide con rimbalzo basso e stiffness bassa.
     *
     **Utilizzo tipico:**
     * - Card che si espandono/contraggono.
     * - Elementi UI che appaiono/scompaiono.
     * - Transizioni morbide di layout.
     *
     * @return Specifiche spring con damping ratio basso e stiffness bassa.
     */
    fun <T> springLight() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Spring medio per animazioni con rimbalzo moderato.
     *
     * **Utilizzo tipico:**
     * - Elementi che vengono trascinati.
     * - Pulsanti con effetto di pressione.
     * - Transizioni di schermo.
     *
     * @return Specifiche spring con damping ratio basso e stiffness media.
     */
    fun <T> springMedium() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Spring pesante per animazioni rapide con poco rimbalzo.
     *
     * **Utilizzo tipico:**
     * - Elementi che devono rispondere rapidamente.
     * - Animazioni di caricamento.
     * - Effetti di scorrimento veloci.
     *
     * @return Specifiche spring con damping ratio alto e stiffness alta.
     */
    fun <T> springStiff() = spring<T>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessHigh
    )

    // ================================ TWEEN ================================

    /**
     * Transizione fluida standard con durata di 300ms e easing FastOutSlowIn.
     *
     * **Utilizzo tipico:**
     * - Transizioni di stato UI.
     * - Animazioni di fade in/out.
     * - Cambiamenti di colore.
     *
     * @return Specifiche tween con durata normale e easing standard.
     */
    fun <T> standardTween() = tween<T>(
        durationMillis = DURATION_NORMAL,
        easing = FastOutSlowInEasing
    )

    /**
     * Transizione lenta con durata di 500ms e easing FastOutSlowIn.
     *
     * **Utilizzo tipico:**
     * - Transizioni di schermo complete.
     * - Animazioni di apparizione di elementi importanti.
     * - Effetti di profondità.
     *
     * @return Specifiche tween con durata lenta e easing standard.
     */
    fun <T> slowTween() = tween<T>(
        durationMillis = DURATION_SLOW,
        easing = FastOutSlowInEasing
    )

    /**
     * Transizione molto lenta con durata di 1500ms e easing FastOutSlowIn.
     *
     * **Utilizzo tipico:**
     * - Onboarding e tutorial.
     * - Intro animazioni.
     * - Effetti drammatici.
     *
     * @return Specifiche tween con durata molto lenta e easing standard.
     */
    fun <T> extraSlowTween() = tween<T>(
        durationMillis = DURATION_EXTRA_SLOW,
        easing = FastOutSlowInEasing
    )

    /**
     * Transizione veloce con durata di 150ms e easing LinearOutSlowIn.
     *
     * **Utilizzo tipico:**
     * - Micro-interazioni.
     * - Click e toggle.
     * - Feedback immediato.
     *
     * @return Specifiche tween con durata veloce e easing lineare.
     */
    fun <T> fastTween() = tween<T>(
        durationMillis = DURATION_FAST,
        easing = LinearOutSlowInEasing
    )

    /**
     * Transizione con overshoot (superamento) utilizzando CubicBezier.
     *
     * L'effetto overshoot crea un superamento del valore target prima di
     * stabilizzarsi, dando un senso di elasticità controllata.
     *
     * **Utilizzo tipico:**
     * - Effetti di rimbalzo controllato.
     * - Elementi che "atterrano" in posizione.
     * - Animazioni giocose e dinamiche.
     *
     * @return Specifiche tween con overshoot e durata di 600ms.
     */
    fun <T> overshootTween() = tween<T>(
        durationMillis = 600,
        easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    )

    // ================================ DURATION ================================

    /** Durata molto veloce: 150ms per micro-interazioni. */
    const val DURATION_FAST = 150

    /** Durata normale: 300ms per transizioni UI standard. */
    const val DURATION_NORMAL = 300

    /** Durata lenta: 500ms per transizioni di schermo. */
    const val DURATION_SLOW = 500

    /** Durata molto lenta: 1500ms per onboarding e intro. */
    const val DURATION_EXTRA_SLOW = 1500
}

/**
 * Estensione per creare un [Animatable] con spring light preconfigurato.
 *
 * Fornisce un modo rapido per creare valori animati con effetto molla leggero.
 *
 * **Utilizzo tipico:**
 * ```
 * val offsetX = animateFloatAsSpringLight(0f)
 * LaunchedEffect(Unit) {
 *     offsetX.animateTo(100f)
 * }
 * ```
 *
 * @param targetValue Valore iniziale dell'animazione.
 * @param visibilityThreshold Soglia di visibilità per l'ottimizzazione delle prestazioni.
 * @return Un [Animatable] configurato con spring light.
 *
 * @see Animatable per la documentazione dettagliata.
 */
fun animateFloatAsSpringLight(
    targetValue: Float,
    visibilityThreshold: Float? = null
): Animatable<Float, AnimationVector1D> {
    return Animatable(targetValue)
}