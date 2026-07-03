// file: ui/theme/Color.kt
package com.example.cares.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Enumerazione dei temi colore disponibili nell'app.
 *
 * Ogni tema ha un nome visualizzato all'utente nelle impostazioni.
 * I colori per ciascun tema sono definiti negli oggetti sottostanti (es. [GreenTheme]).
 *
 * **Temi disponibili:**
 * - [GREEN]: Verde (default)
 * - [BLUE]: Blu
 * - [PURPLE]: Viola
 * - [ORANGE]: Arancione
 * - [PINK]: Rosa
 * - [RED]: Rosso
 * - [TEAL]: Verde acqua
 *
 * @see GreenTheme per i colori del tema verde.
 * @see ThemeManager per la gestione del tema attivo.
 */
enum class AppTheme {
    GREEN,
    BLUE,
    PURPLE,
    ORANGE,
    PINK,
    RED,
    TEAL
}

// ================================ TEMA VERDE (DEFAULT) ================================

/**
 * Tema colore verde, utilizzato come tema predefinito dell'app.
 *
 * Segue le convenzioni di Material Design 3, con colori distinti per modalità chiara e scura.
 * I colori sono definiti secondo le seguenti categorie:
 * - **Primary**: colore principale dell'app (brand).
 * - **PrimaryDark**: variante scura del colore primario.
 * - **PrimaryLight**: variante chiara del colore primario.
 * - **Secondary**: colore secondario per elementi di supporto.
 * - **SecondaryDark**: variante scura del colore secondario.
 * - **SecondaryLight**: variante chiara del colore secondario.
 * - **Background**: colore di sfondo delle schermate.
 * - **Surface**: colore delle superfici (cards, dialoghi, ecc.).
 * - **Error**: colore per messaggi di errore e stati critici.
 * - **OnPrimary**: colore per testo e icone su sfondi primari.
 * - **OnSecondary**: colore per testo e icone su sfondi secondari.
 * - **OnBackground**: colore per testo su sfondo.
 * - **OnSurface**: colore per testo su superfici.
 *
 * **Dark mode:**
 * - I colori scuri sono ottimizzati per la lettura in ambienti con poca luce.
 * - I colori primari sono leggermente più luminosi per mantenere leggibilità.
 */
object GreenTheme {
    // --- Light mode ---
    val Primary = Color(0xFF4CAF50)
    val PrimaryDark = Color(0xFF388E3C)
    val PrimaryLight = Color(0xFFC8E6C9)
    val Secondary = Color(0xFF2196F3)
    val SecondaryDark = Color(0xFF1976D2)
    val SecondaryLight = Color(0xFFBBDEFB)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val Error = Color(0xFFD32F2F)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF1A1A1A)
    val OnSurface = Color(0xFF1A1A1A)

    // --- Dark mode ---
    val DarkPrimary = Color(0xFF66BB6A)
    val DarkPrimaryDark = Color(0xFF2E7D32)
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkOnBackground = Color(0xFFE0E0E0)
    val DarkOnSurface = Color(0xFFE0E0E0)
    val DarkOnPrimary = Color(0xFF121212)
}

// ================================ TEMA BLU ================================

/**
 * Tema colore blu, basato sulla palette Material Blue.
 */
object BlueTheme {
    // --- Light mode ---
    val Primary = Color(0xFF2196F3)
    val PrimaryDark = Color(0xFF1976D2)
    val PrimaryLight = Color(0xFFBBDEFB)
    val Secondary = Color(0xFF4CAF50)
    val SecondaryDark = Color(0xFF388E3C)
    val SecondaryLight = Color(0xFFC8E6C9)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val Error = Color(0xFFD32F2F)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF1A1A1A)
    val OnSurface = Color(0xFF1A1A1A)

    // --- Dark mode ---
    val DarkPrimary = Color(0xFF64B5F6)
    val DarkPrimaryDark = Color(0xFF1565C0)
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkOnBackground = Color(0xFFE0E0E0)
    val DarkOnSurface = Color(0xFFE0E0E0)
    val DarkOnPrimary = Color(0xFF121212)
}

// ================================ TEMA VIOLA ================================

/**
 * Tema colore viola, basato sulla palette Material Purple.
 */
object PurpleTheme {
    // --- Light mode ---
    val Primary = Color(0xFF9C27B0)
    val PrimaryDark = Color(0xFF7B1FA2)
    val PrimaryLight = Color(0xFFE1BEE7)
    val Secondary = Color(0xFF4CAF50)
    val SecondaryDark = Color(0xFF388E3C)
    val SecondaryLight = Color(0xFFC8E6C9)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val Error = Color(0xFFD32F2F)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF1A1A1A)
    val OnSurface = Color(0xFF1A1A1A)

    // --- Dark mode ---
    val DarkPrimary = Color(0xFFAB47BC)
    val DarkPrimaryDark = Color(0xFF6A1B9A)
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkOnBackground = Color(0xFFE0E0E0)
    val DarkOnSurface = Color(0xFFE0E0E0)
    val DarkOnPrimary = Color(0xFF121212)
}

// ================================ TEMA ARANCIONE ================================

/**
 * Tema colore arancione, basato sulla palette Material Orange.
 */
object OrangeTheme {
    // --- Light mode ---
    val Primary = Color(0xFFFF9800)
    val PrimaryDark = Color(0xFFF57C00)
    val PrimaryLight = Color(0xFFFFE0B2)
    val Secondary = Color(0xFF2196F3)
    val SecondaryDark = Color(0xFF1976D2)
    val SecondaryLight = Color(0xFFBBDEFB)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val Error = Color(0xFFD32F2F)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF1A1A1A)
    val OnSurface = Color(0xFF1A1A1A)

    // --- Dark mode ---
    val DarkPrimary = Color(0xFFFFA726)
    val DarkPrimaryDark = Color(0xFFE65100)
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkOnBackground = Color(0xFFE0E0E0)
    val DarkOnSurface = Color(0xFFE0E0E0)
    val DarkOnPrimary = Color(0xFF121212)
}

// ================================ TEMA ROSA ================================

/**
 * Tema colore rosa, basato sulla palette Material Pink.
 */
object PinkTheme {
    // --- Light mode ---
    val Primary = Color(0xFFE91E63)
    val PrimaryDark = Color(0xFFC2185B)
    val PrimaryLight = Color(0xFFF8BBD0)
    val Secondary = Color(0xFF4CAF50)
    val SecondaryDark = Color(0xFF388E3C)
    val SecondaryLight = Color(0xFFC8E6C9)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val Error = Color(0xFFD32F2F)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF1A1A1A)
    val OnSurface = Color(0xFF1A1A1A)

    // --- Dark mode ---
    val DarkPrimary = Color(0xFFF06292)
    val DarkPrimaryDark = Color(0xFFAD1457)
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkOnBackground = Color(0xFFE0E0E0)
    val DarkOnSurface = Color(0xFFE0E0E0)
    val DarkOnPrimary = Color(0xFF121212)
}

// ================================ TEMA ROSSO ================================

/**
 * Tema colore rosso, basato sulla palette Material Red.
 */
object RedTheme {
    // --- Light mode ---
    val Primary = Color(0xFFF44336)
    val PrimaryDark = Color(0xFFD32F2F)
    val PrimaryLight = Color(0xFFFFCDD2)
    val Secondary = Color(0xFF2196F3)
    val SecondaryDark = Color(0xFF1976D2)
    val SecondaryLight = Color(0xFFBBDEFB)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val Error = Color(0xFFD32F2F)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF1A1A1A)
    val OnSurface = Color(0xFF1A1A1A)

    // --- Dark mode ---
    val DarkPrimary = Color(0xFFEF5350)
    val DarkPrimaryDark = Color(0xFFB71C1C)
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkOnBackground = Color(0xFFE0E0E0)
    val DarkOnSurface = Color(0xFFE0E0E0)
    val DarkOnPrimary = Color(0xFF121212)
}

// ================================ TEMA VERDE ACQUA ================================

/**
 * Tema colore verde acqua (Teal), basato sulla palette Material Teal.
 */
object TealTheme {
    // --- Light mode ---
    val Primary = Color(0xFF009688)
    val PrimaryDark = Color(0xFF00796B)
    val PrimaryLight = Color(0xFFB2DFDB)
    val Secondary = Color(0xFF2196F3)
    val SecondaryDark = Color(0xFF1976D2)
    val SecondaryLight = Color(0xFFBBDEFB)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val Error = Color(0xFFD32F2F)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF1A1A1A)
    val OnSurface = Color(0xFF1A1A1A)

    // --- Dark mode ---
    val DarkPrimary = Color(0xFF26A69A)
    val DarkPrimaryDark = Color(0xFF00695C)
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkOnBackground = Color(0xFFE0E0E0)
    val DarkOnSurface = Color(0xFFE0E0E0)
    val DarkOnPrimary = Color(0xFF121212)
}