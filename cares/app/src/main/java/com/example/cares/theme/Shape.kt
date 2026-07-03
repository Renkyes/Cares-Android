// file: ui/theme/Shape.kt
package com.example.cares.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Definizione delle forme globali dell'app, utilizzate da MaterialTheme.
 *
 * Le forme vengono applicate automaticamente a tutti i componenti Material
 * che supportano il parametro `shape` (Card, Button, Dialog, ecc.).
 *
 * La gerarchia delle forme segue le linee guida Material Design 3 e
 * fornisce una scala coerente per gli angoli arrotondati.
 *
 * **Gerarchia delle forme:**
 *
 * | Livello     | Dimensione | Utilizzo tipico                                |
 * |-------------|------------|------------------------------------------------|
 * | extraSmall  | 4.dp       | Chip, badge, elementi molto piccoli            |
 * | small       | 8.dp       | Bottoni secondari, piccole card, indicatori    |
 * | medium      | 12.dp      | Card standard, input fields, contenitori medi  |
 * | large       | 16.dp      | Card principali, dialog, pannelli              |
 * | extraLarge  | 24.dp      | Card di contenuto ampio, sezioni principali    |
 *
 * **Best practice:**
 * - Usare `extraSmall` per elementi che richiedono un accenno di arrotondamento.
 * - Usare `small` per elementi interattivi come bottoni secondari.
 * - Usare `medium` per la maggior parte dei contenitori standard.
 * - Usare `large` per elementi di primo piano come card principali.
 * - Usare `extraLarge` per sezioni distintive o contenitori di grandi dimensioni.
 *
 * @see androidx.compose.material3.MaterialTheme.shapes per l'integrazione nel tema.
 * @see RoundedCornerShape per la documentazione della classe di base.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)