// file: ui/theme/Type.kt
package com.example.cares.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Definizione della tipografia globale dell'app, utilizzata da MaterialTheme.
 *
 * Ogni stile è configurato per garantire leggibilità e coerenza visiva su tutti i dispositivi.
 * Le dimensioni sono espresse in `sp` (scale-independent pixels) per rispettare le preferenze
 * di dimensione del testo dell'utente.
 *
 * **Gerarchia degli stili (Material Design 3):**
 *
 * | Categoria | Stili                                        | Utilizzo                                        |
 * |-----------|----------------------------------------------|-------------------------------------------------|
 * | Display   | displayLarge, displayMedium, displaySmall    | Titoli molto grandi (onboarding, splash screen) |
 * | Headline  | headlineLarge, headlineMedium, headlineSmall | Titoli principali di schermata                  |
 * | Title     | titleLarge, titleMedium, titleSmall          | Sottotitoli ed elementi di rilievo              |
 * | Body      | bodyLarge, bodyMedium, bodySmall             | Testi correnti, paragrafi, descrizioni          |
 * | Label     | labelLarge, labelMedium, labelSmall          | Etichette di pulsanti, campi di input, chip     |
 *
 * **Linee guida per l'utilizzo:**
 * - **Display:** Utilizzare solo per titoli molto grandi e schermate introduttive.
 *   Evitare l'uso eccessivo per non sovraccaricare l'interfaccia.
 * - **Headline:** Utilizzare per i titoli principali delle schermate e sezioni.
 * - **Title:** Utilizzare per sottotitoli, intestazioni di card e elementi di rilievo.
 * - **Body:** Utilizzare per la maggior parte dei testi correnti e descrizioni.
 * - **Label:** Utilizzare per elementi interattivi come pulsanti, chip e campi di input.
 *
 * **Nota:** Questa è la tipografia base di Material Design 3. Per l'app è stata creata una
 * variante [GlassTypography] in `GlassTheme.kt` che estende questa con effetti di ombra
 * e glow per alcuni stili (es. headlineLarge).
 *
 * @see GlassTypography per la versione con effetti luminosi.
 * @see androidx.compose.material3.Typography per la documentazione di base.
 */
val Typography = Typography(
    // ==================== Display ====================
    // Titoli molto grandi per onboarding e schermate introduttive
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),

    // ==================== Headline ====================
    // Titoli principali di schermata e sezioni
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),

    // ==================== Title ====================
    // Sottotitoli ed elementi di rilievo
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),

    // ==================== Body ====================
    // Testi correnti, paragrafi e descrizioni
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),

    // ==================== Label ====================
    // Etichette di pulsanti, campi di input e chip
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)