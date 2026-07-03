// file: utils/BadgeSystem.kt
package com.example.cares.utils

// ================================ BADGE DATA CLASS ================================

/**
 * Rappresenta un badge ottenibile dall'utente come ricompensa per i progressi.
 *
 * Ogni badge è definito da un identificatore univoco, un nome visualizzato,
 * una descrizione, un'icona emoji e una condizione di sblocco basata sulle
 * statistiche di gioco.
 *
 * **Condizione di sblocco:**
 * La condizione è una lambda che riceve quattro parametri (xp, level, completedQuests, streak)
 * e restituisce `true` se il badge può essere sbloccato. Questa flessibilità permette
 * di definire badge con requisiti complessi e combinati.
 *
 * @property id Identificatore univoco del badge (es. "first_steps").
 * @property name Nome visualizzato del badge (es. "Primi Passi").
 * @property description Descrizione del badge (es. "Completa la tua prima missione").
 * @property icon Emoji o icona rappresentativa del badge (es. "👣").
 * @property condition Lambda che determina lo sblocco del badge.
 *                         Riceve: `xp` (esperienza), `level` (livello),
 *                         `completedQuests` (missioni completate), `streak` (serie di giorni).
 *                         Deve restituire `true` se il badge può essere sbloccato.
 */
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val condition: (xp: Int, level: Int, completedQuests: Int, streak: Int) -> Boolean
)

// ================================ CATALOGO BADGE ================================

/**
 * Lista completa di tutti i badge disponibili nell'app.
 *
 * **Categorie di badge:**
 * - **Missioni completate**: badge legati al numero di missioni completate.
 * - **Livello raggiunto**: badge legati al livello raggiunto.
 * - **Streak mantenuta**: badge legati alla serie di giorni consecutivi.
 *
 * **Ordine di visualizzazione:**
 * I badge sono ordinati per categoria, ma l'ordine all'interno di ciascuna
 * categoria segue un criterio di difficoltà crescente (prima i più facili).
 *
 * @see Badge per la struttura di ogni badge.
 */
val allBadges = listOf(
    // ============================================================
    // BADGE PER MISSIONI COMPLETATE
    // ============================================================

    Badge(
        id = "first_steps",
        name = "Primi Passi",
        description = "Completa la tua prima missione",
        icon = "👣",
        condition = { _, _, completedQuests, _ -> completedQuests >= 1 }
    ),
    Badge(
        id = "amateur",
        name = "Dilettante",
        description = "Completa 10 missioni",
        icon = "🔥",
        condition = { _, _, completedQuests, _ -> completedQuests >= 10 }
    ),
    Badge(
        id = "dedicated",
        name = "Costante",
        description = "Completa 25 missioni",
        icon = "🔥",
        condition = { _, _, completedQuests, _ -> completedQuests >= 25 }
    ),
    Badge(
        id = "marathoner",
        name = "Maratoneta",
        description = "Completa 50 missioni",
        icon = "🏃",
        condition = { _, _, completedQuests, _ -> completedQuests >= 50 }
    ),

    // ============================================================
    // BADGE PER LIVELLO RAGGIUNTO
    // ============================================================

    Badge(
        id = "level_5",
        name = "Guerriero",
        description = "Raggiungi il livello 5",
        icon = "🏆",
        condition = { _, level, _, _ -> level >= 5 }
    ),
    Badge(
        id = "level_10",
        name = "Campione",
        description = "Raggiungi il livello 10",
        icon = "⭐",
        condition = { _, level, _, _ -> level >= 10 }
    ),
    Badge(
        id = "level_25",
        name = "Leggenda",
        description = "Raggiungi il livello 25",
        icon = "⭐",
        condition = { _, level, _, _ -> level >= 25 }
    ),
    Badge(
        id = "level_50",
        name = "Semi-Dio",
        description = "Raggiungi il livello 50",
        icon = "⭐",
        condition = { _, level, _, _ -> level >= 50 }
    ),
    Badge(
        id = "level_100",
        name = "Divinità",
        description = "Raggiungi il livello 100",
        icon = "⭐",
        condition = { _, level, _, _ -> level >= 100 }
    ),

    // ============================================================
    // BADGE PER STREAK (SERIE DI GIORNI CONSECUTIVI)
    // ============================================================

    Badge(
        id = "streak_7_g",
        name = "Costante",
        description = "Mantieni una streak di 7 giorni",
        icon = "🔥",
        condition = { _, _, _, streak -> streak >= 7 }
    ),
    Badge(
        id = "streak_14_g",
        name = "Dedicato",
        description = "Mantieni una streak di 14 giorni",
        icon = "💪",
        condition = { _, _, _, streak -> streak >= 14 }
    ),
    Badge(
        id = "streak_1_m",
        name = "Leggenda",
        description = "Mantieni una streak di 30 giorni",
        icon = "🗡️",
        condition = { _, _, _, streak -> streak >= 30 }
    ),
    Badge(
        id = "streak_3_m",
        name = "Mito",
        description = "Mantieni una streak di 3 mesi (90 giorni)",
        icon = "⚔️",
        condition = { _, _, _, streak -> streak >= 90 }
    ),
    Badge(
        id = "streak_6_m",
        name = "Imbattibile",
        description = "Mantieni una streak di 6 mesi (180 giorni)",
        icon = "⭐",
        condition = { _, _, _, streak -> streak >= 180 }
    ),
    Badge(
        id = "streak_1_y",
        name = "Invincibile",
        description = "Mantieni una streak di 1 anno (365 giorni)",
        icon = "👑",
        condition = { _, _, _, streak -> streak >= 365 }
    )
)

// ================================ FUNZIONE DI VERIFICA ================================

/**
 * Verifica quali badge non ancora sbloccati soddisfano le condizioni di sblocco.
 *
 * **Comportamento:**
 * - Scorre tutti i badge definiti in [allBadges].
 * - Per ogni badge, controlla se il suo ID NON è presente nell'insieme [unlockedBadges].
 * - Se non è ancora sbloccato, valuta la sua condizione con le statistiche attuali.
 * - Restituisce la lista dei badge per cui la condizione è vera (appena sbloccabili).
 *
 * **Momenti di utilizzo:**
 * - Dopo il completamento di una missione.
 * - Dopo l'aumento di XP (che può portare a un nuovo livello).
 * - Dopo l'incremento dello streak.
 * - Dopo il caricamento iniziale del profilo.
 *
 * **Esempio:**
 * ```
 * val newBadges = checkBadges(
 *     xp = 350,
 *     level = 4,
 *     completedQuests = 12,
 *     streak = 8,
 *     unlockedBadges = setOf("first_steps", "amateur")
 * )
 * // newBadges conterrà i badge appena sbloccabili
 * ```
 *
 * @param xp Esperienza attuale dell'utente.
 * @param level Livello attuale dell'utente.
 * @param completedQuests Numero di missioni completate.
 * @param streak Streak attuale dell'utente (giorni consecutivi).
 * @param unlockedBadges Insieme degli ID dei badge già sbloccati.
 * @return Lista dei badge appena sbloccabili (non ancora sbloccati ma che soddisfano le condizioni).
 */
fun checkBadges(
    xp: Int,
    level: Int,
    completedQuests: Int,
    streak: Int,
    unlockedBadges: Set<String>
): List<Badge> {
    return allBadges.filter { badge ->
        !unlockedBadges.contains(badge.id) &&
                badge.condition(xp, level, completedQuests, streak)
    }
}