// file: data/models/InventoryItem.kt
package com.example.cares.data.models

import androidx.compose.ui.graphics.Color
import com.example.cares.ui.theme.Neon

/**
 * Rappresenta un oggetto nell'inventario dell'utente.
 *
 * Gli oggetti possono essere ottenuti come ricompensa speciale
 * per il completamento di missioni difficili o come premio
 * per traguardi raggiunti.
 *
 * **Tipi di oggetti disponibili:**
 * - [StreakSave]: Scudo che protegge la streak dalla scadenza.
 * - [XpBoost]: Moltiplicatore di XP per un periodo limitato.
 * - [StoryFragment]: Frammento che sblocca un capitolo della storia.
 *
 * **Gerarchia di rarità:**
 * - COMMON (🔵): Missioni facili
 * - RARE (🟣): Missioni medie
 * - EPIC (🟠): Missioni difficili
 * - LEGENDARY (🔴): Missioni molto difficili
 *
 * @property id Identificatore univoco dell'oggetto.
 * @property name Nome visualizzato nell'inventario.
 * @property description Descrizione dell'effetto dell'oggetto.
 * @property icon Emoji rappresentativa dell'oggetto.
 * @property rarity Rarità dell'oggetto (determina colore e probabilità).
 */
sealed class InventoryItem(
    open val id: String,
    open val name: String,
    open val description: String,
    open val icon: String,
    open val rarity: Rarity
) {

    /**
     * Rarità dell'oggetto, che determina il colore di sfondo e la probabilità di ottenimento.
     *
     * **Correlazione con la difficoltà:**
     * - COMMON: ottenibile da missioni facili (alta probabilità).
     * - RARE: ottenibile da missioni medie (media probabilità).
     * - EPIC: ottenibile da missioni difficili (bassa probabilità).
     * - LEGENDARY: ottenibile da missioni molto difficili (molto bassa probabilità).
     */
    enum class Rarity {
        COMMON,
        RARE,
        EPIC,
        LEGENDARY;

        /**
         * Restituisce il colore associato alla rarità per la UI.
         *
         * @return Colore rappresentativo della rarità.
         */
        fun getColor(): Color = when (this) {
            COMMON -> Color(0xFF42A5F5)      // Blu
            RARE -> Color(0xFFAB47BC)        // Viola
            EPIC -> Color(0xFFFFA726)        // Arancione
            LEGENDARY -> Color(0xFFEF5350)   // Rosso
        }

        /**
         * Restituisce l'emoji della rarità.
         *
         * @return Emoji rappresentativa (🔵, 🟣, 🟠, 🔴).
         */
        fun getEmoji(): String = when (this) {
            COMMON -> "🔵"
            RARE -> "🟣"
            EPIC -> "🟠"
            LEGENDARY -> "🔴"
        }

        /**
         * Restituisce il nome della rarità in italiano.
         *
         * @return Stringa localizzata per la UI.
         */
        fun getDisplayName(): String = when (this) {
            COMMON -> "Comune"
            RARE -> "Raro"
            EPIC -> "Epico"
            LEGENDARY -> "Leggendario"
        }
    }

    // ================================ TIPI DI OGGETTI ================================

    /**
     * Oggetto "Scudo Streak" - Salva la streak quando sta per scadere.
     *
     * Se l'utente salta un giorno di attività, lo scudo viene consumato
     * automaticamente e la streak non viene resettata a 0, ma rimane
     * invariata, permettendo all'utente di continuare la sequenza.
     *
     * **Utilizzo:** Si consuma automaticamente, non richiede azione utente.
     */
    data object StreakSave : InventoryItem(
        id = "streak_save",
        name = "Scudo Streak",
        description = "Salva la tua streak quando sta per scadere. Si consuma automaticamente.",
        icon = "🛡️",
        rarity = Rarity.RARE
    )

    /**
     * Oggetto "Boost XP" - Moltiplica l'XP guadagnato per un certo periodo.
     *
     * Una volta attivato, tutti gli XP guadagnati vengono moltiplicati
     * per il fattore specificato per la durata indicata in giorni.
     *
     * @property days Durata del boost in giorni (1-7).
     * @property multiplier Moltiplicatore dell'XP (es. 1.5f per +50%, 2.0f per +100%).
     */
    data class XpBoost(
        val days: Int,
        val multiplier: Float = 1.5f
    ) : InventoryItem(
        id = "xp_boost_${days}d_${multiplier}x",
        name = "Boost XP x${multiplier}",
        description = "Moltiplica l'XP guadagnato per ${multiplier}x per $days giorni",
        icon = if (multiplier >= 2.0f) "⚡" else "⚡",
        rarity = if (multiplier >= 2.0f) Rarity.EPIC else Rarity.RARE
    ) {
        /**
         * Verifica se il boost è ancora attivo.
         *
         * @param activatedAt Timestamp di attivazione del boost.
         * @return `true` se il boost è ancora attivo, `false` altrimenti.
         */
        fun isActive(activatedAt: Long): Boolean {
            val elapsedDays = (System.currentTimeMillis() - activatedAt) / (24 * 60 * 60 * 1000)
            return elapsedDays < days
        }

        /**
         * Calcola i giorni rimanenti del boost.
         *
         * @param activatedAt Timestamp di attivazione del boost.
         * @return Numero di giorni rimanenti (0 se scaduto).
         */
        fun getDaysRemaining(activatedAt: Long): Int {
            val elapsedDays = (System.currentTimeMillis() - activatedAt) / (24 * 60 * 60 * 1000)
            return (days - elapsedDays).coerceAtLeast(0).toInt()
        }
    }

    /**
     * Oggetto "Frammento di Storia" - Sblocca un capitolo della storia.
     *
     * Raccogliendo frammenti di storia, l'utente può sbloccare capitoli
     * della narrazione senza dover raggiungere i requisiti di sblocco
     * tradizionali (livello, streak, missioni).
     *
     * @property chapterId ID del capitolo da sbloccare.
     * @property chapterTitle Titolo del capitolo (per visualizzazione).
     */
    data class StoryFragment(
        val chapterId: String,
        val chapterTitle: String
    ) : InventoryItem(
        id = "story_fragment_$chapterId",
        name = "📖 $chapterTitle",
        description = "Sblocca il capitolo '$chapterTitle' della tua avventura!",
        icon = "📖",
        rarity = Rarity.EPIC
    )

    // ================================ FUNZIONI DI UTILITY ================================

    /**
     * Restituisce la descrizione formattata per la UI.
     *
     * Aggiunge informazioni contestuali specifiche per ogni tipo di oggetto,
     * rendendo la descrizione più chiara e utile per l'utente.
     *
     * @return Descrizione formattata con emoji e dettagli.
     */
    fun getFormattedDescription(): String {
        return when (this) {
            is StreakSave -> "🛡️ Consumabile: salva la tua streak quando sta per scadere."
            is XpBoost -> "⚡ Attivo per $days giorni: +${((multiplier - 1) * 100).toInt()}% XP"
            is StoryFragment -> "📖 Sblocca il capitolo: $chapterTitle"
        }
    }

    /**
     * Verifica se l'oggetto è utilizzabile (non è una risorsa passiva).
     *
     * Alcuni oggetti si attivano automaticamente (es. StoryFragment),
     * mentre altri richiedono un'azione esplicita dall'utente (es. XpBoost).
     *
     * @return `true` se l'oggetto può essere utilizzato attivamente.
     */
    fun isUsable(): Boolean {
        return when (this) {
            is StreakSave -> true    // Si consuma automaticamente quando necessario
            is XpBoost -> true       // Richiede attivazione manuale
            is StoryFragment -> false // Si attiva automaticamente al completamento
        }
    }

    companion object {
        /**
         * Crea un oggetto a partire da un ID e parametri.
         * Utile per la deserializzazione da fonti esterne.
         *
         * @param id L'ID dell'oggetto da creare.
         * @param params Parametri aggiuntivi per la creazione (es. days, chapterId).
         * @return L'oggetto creato o `null` se l'ID non è riconosciuto.
         */
        fun fromId(id: String, params: Map<String, Any> = emptyMap()): InventoryItem? {
            return when (id) {
                "streak_save" -> StreakSave
                else -> null
            }
        }

        /**
         * Restituisce la lista di tutti i tipi di oggetti (per UI di esempio).
         *
         * Utile per anteprime, demo o per mostrare all'utente i tipi
         * di oggetti disponibili.
         *
         * @return Lista di esempi di tutti i tipi di oggetti.
         */
        fun getAllExampleItems(): List<InventoryItem> = listOf(
            StreakSave,
            XpBoost(days = 3, multiplier = 1.5f),
            XpBoost(days = 2, multiplier = 2.0f),
            StoryFragment(chapterId = "w_chapter_extra_1", chapterTitle = "Il Risveglio del Drago")
        )
    }
}