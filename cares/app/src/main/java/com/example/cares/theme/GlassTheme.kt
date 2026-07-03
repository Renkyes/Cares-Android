// file: ui/theme/GlassTheme.kt
package com.example.cares.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset

// ================================ COLORI NEON ================================

/**
 * Colori neon utilizzati per effetti luminosi e accenti nell'interfaccia.
 *
 * Questi colori vengono utilizzati per creare un'estetica moderna e giocosa,
 * ispirata al design cyberpunk/neon, e si abbinano bene sia al tema scuro che a quello chiaro.
 *
 * **Palette colori:**
 * - [Green]: Verde neon (#00E676) - colore primario di accento
 * - [Cyan]: Ciano neon (#00E5FF) - effetti di luce secondari
 * - [Blue]: Blu neon (#2979FF) - elementi di enfasi
 * - [Purple]: Viola neon (#7C4DFF) - accenti in sezioni specifiche
 * - [Pink]: Rosa neon (#FF4081) - effetti di luce caldi
 * - [Orange]: Arancione neon (#FF9100) - messaggi di avviso
 * - [Dark]: Sfondo scuro (#0D0D0D) - sezioni in stile dark
 * - [DarkSurface]: Superficie glass (#1AFFFFFF) - semitrasparente
 * - [Glow]: Glow verde (#3300E676) - effetti di bagliore
 *
 * @see Gradients per gli sfondi a gradiente che utilizzano questi colori.
 */
object Neon {
    val Green = Color(0xFF00E676)
    val Cyan = Color(0xFF00E5FF)
    val Blue = Color(0xFF2979FF)
    val Purple = Color(0xFF7C4DFF)
    val Pink = Color(0xFFFF4081)
    val Orange = Color(0xFFFF9100)
    val Dark = Color(0xFF0D0D0D)
    val DarkSurface = Color(0x1AFFFFFF)
    val Glow = Color(0x3300E676)
}

// ================================ SISTEMA DI SFONDI A GRADIENTE ================================

/**
 * Oggetto contenitore per gradienti utilizzati come sfondi dell'applicazione.
 *
 * Fornisce gradienti ottimizzati per tema scuro e chiaro, e per l'effetto glassmorphism.
 *
 * **Gradienti disponibili:**
 * - [mainBackground]: Sfondo principale dell'app, adattato al tema.
 * - [glass]: Sfondo per l'effetto glassmorphism, utilizzato per le card e i contenitori.
 */
object Gradients {

    /**
     * Sfondo principale dell'app, adattato al tema corrente.
     *
     * **Tema scuro:** gradiente verticale da nero profondo a blu scuro,
     * che crea un'atmosfera immersiva e notturna.
     *
     * **Tema chiaro:** gradiente verticale da bianco/grigio chiaro a grigio medio,
     * per uno sfondo elegante e minimalista.
     *
     * @param darkTheme Se `true` restituisce la versione scura, altrimenti la versione chiara.
     * @return Un [Brush] da applicare come sfondo.
     */
    fun mainBackground(darkTheme: Boolean): Brush {
        return if (darkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A0A0A), // Nero profondo
                    Color(0xFF1A1A2E)  // Blu scuro
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF5F7FA), // Bianco/grigio chiaro
                    Color(0xFFE4E7EB)  // Grigio medio
                )
            )
        }
    }

    /**
     * Sfondo per l'effetto glassmorphism, utilizzato per le card e i contenitori.
     *
     * **Effetto glassmorphism:** combinazione di:
     * 1. Un gradiente verticale con colori semitrasparenti (bianco o nero a seconda del tema).
     * 2. Un secondo sfondo con trasparenza ulteriore per attenuare la luminosità.
     * 3. Un bordo sottile e luminoso per creare l'effetto "vetro".
     *
     * **Tema scuro:** gradienti da bianco al 30% a bianco al 10%.
     * **Tema chiaro:** gradienti da bianco al 60% a bianco al 30%.
     *
     * @param darkTheme Se `true` restituisce la versione scura, altrimenti la versione chiara.
     * @return Un [Brush] da applicare come sfondo della card glass.
     */
    fun glass(darkTheme: Boolean): Brush {
        return if (darkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x4DFFFFFF), // 30% white
                    Color(0x1AFFFFFF)  // 10% white
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x99FFFFFF), // 60% white
                    Color(0x4DFFFFFF)  // 30% white
                )
            )
        }
    }
}

// ================================ TIPOGRAFIA MODERNA ================================

/**
 * Tipografia personalizzata per l'app, che estende quella di Material Design 3.
 *
 * Aggiunge un'ombra luminosa (shadow) al testo di livello headlineLarge per un effetto glow.
 * I font size seguono la gerarchia di Material Design 3:
 * - headlineLarge: 32sp, Bold, con ombra verde neon
 * - headlineMedium: 24sp, SemiBold
 * - headlineSmall: 20sp, SemiBold
 * - titleLarge: 18sp, Medium
 * - titleMedium: 16sp, Medium
 * - bodyLarge: 16sp, Normal
 * - bodyMedium: 14sp, Normal
 * - bodySmall: 12sp, Normal
 *
 * @see androidx.compose.material3.Typography per la documentazione di base.
 */
val GlassTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        shadow = Shadow(
            color = Neon.Green.copy(alpha = 0.3f),
            offset = Offset(0f, 4f),
            blurRadius = 12f
        )
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    )
)

// ================================ COMPONENTI GLASSMORPHISM ================================

/**
 * Card con effetto glassmorphism (vetro smerigliato).
 *
 * **Caratteristiche:**
 * - Sfondo semitrasparente con gradiente ([Gradients.glass]).
 * - Bordo sottile e luminoso per creare profondità.
 * - Padding interno predefinito di 16dp.
 * - Angoli arrotondati di 16dp.
 *
 * **Utilizzo tipico:**
 * ```
 * GlassCard(
 *     modifier = Modifier.fillMaxWidth(),
 *     darkTheme = isDarkTheme
 * ) {
 *     Text("Contenuto della card")
 * }
 * ```
 *
 * @param modifier Modificatori da applicare alla card.
 * @param darkTheme Se `true` utilizza la versione scura del glassmorphism, altrimenti la chiara.
 *                  Default: `true` (tema scuro).
 * @param content Contenuto della card.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Gradients.glass(darkTheme),
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = if (darkTheme) Color(0x2A000000) else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (darkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        content()
    }
}