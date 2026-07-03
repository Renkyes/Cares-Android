// file: utils/DailyQuestManager.kt
package com.example.cares.utils

import com.example.cares.data.manager.UserPreferencesManager
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ================================ TIPI DI VERIFICA ================================

/**
 * Definisce il tipo di verifica richiesta per completare una missione.
 *
 * @property NONE Nessuna verifica, completamento diretto.
 * @property PHOTO L'utente deve scattare una foto (URI salvato).
 * @property TEXT L'utente deve scrivere un testo.
 * @property PHOTO_RECOGNITION L'utente deve scattare una foto e riconoscere un oggetto specifico con ML Kit.
 */
enum class VerificationType {
    NONE,
    PHOTO,
    TEXT,
    PHOTO_RECOGNITION
}

// ================================ RICOMPENSE SPECIALI ================================

/**
 * Rappresenta una ricompensa speciale che può essere ottenuta completando una missione.
 *
 * **Tipi di ricompense:**
 * - [StreakSave]: Aggiunge uno scudo streak all'inventario.
 * - [XpBoost]: Aggiunge un boost XP all'inventario (da attivare manualmente).
 * - [StoryFragment]: Sblocca un capitolo della storia.
 */
sealed class SpecialReward {

    /**
     * Scudo Streak: salva la streak quando sta per scadere.
     * @param amount Numero di scudi da aggiungere (default 1)
     */
    data class StreakSave(val amount: Int = 1) : SpecialReward()

    /**
     * Boost XP: moltiplica l'XP guadagnato per un certo periodo.
     * @param days Durata in giorni (1-7)
     * @param multiplier Moltiplicatore (1.5x, 2.0x)
     */
    data class XpBoost(val days: Int, val multiplier: Float = 1.5f) : SpecialReward()

    /**
     * Frammento di Storia: sblocca un capitolo extra della storia.
     * @param chapterId ID del capitolo da sbloccare
     * @param chapterTitle Titolo del capitolo (per visualizzazione)
     */
    data class StoryFragment(val chapterId: String, val chapterTitle: String) : SpecialReward()
}

// ================================ QUEST DATA CLASS ================================

/**
 * Rappresenta una missione (quest) che l'utente può completare.
 *
 * @property id Identificatore univoco.
 * @property title Titolo della missione.
 * @property description Descrizione dettagliata.
 * @property xpReward XP guadagnati al completamento.
 * @property duration Durata stimata in minuti.
 * @property tags Categorie (cardio, strength, mindfulness, ecc.).
 * @property targetProgress Numero di step necessari (default 1).
 * @property progressUnit Unità di misura del progresso (es. "volte", "giorni").
 * @property verification Tipo di verifica richiesta.
 * @property verificationPrompt Messaggio guida per l'utente.
 * @property verificationExtra Parametro extra per il riconoscimento (es. "banana").
 * @property specialReward Ricompensa speciale (null se non presente).
 */
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val duration: Int,
    val tags: Set<String> = emptySet(),
    val targetProgress: Int = 1,
    val progressUnit: String = "volte",
    val verification: VerificationType = VerificationType.NONE,
    val verificationPrompt: String = "",
    val verificationExtra: String = "",
    val specialReward: SpecialReward? = null
)

// ================================ POOL DI MISSIONI ================================

/**
 * Pool di missioni giornaliere con varietà di tipologie e difficoltà.
 * Alcune missioni richiedono verifica (foto, testo, riconoscimento oggetto).
 */
val allPossibleQuests = listOf(
    // --- ESERCIZI FISICI ---
    Quest("q1", "🚶 Camminata", "Fai 10 minuti di camminata", 20, 10, setOf("cardio", "daily")),
    Quest("q2", "🏋️ Esercizio", "Fai 15 flessioni", 25, 5, setOf("strength", "upper_body")),
    Quest("q3", "🏃 Corsa", "Fai 5 minuti di corsa sul posto", 25, 5, setOf("cardio", "hiit")),
    Quest("q4", "🚴 Movimento", "Fai 5 minuti di cyclette", 20, 5, setOf("cardio", "endurance")),
    Quest("q5", "🦵 Squat", "Fai 20 squat", 20, 5, setOf("strength", "lower_body")),
    Quest("q6", "💪 Plank", "Mantieni la posizione plank per 1 minuto", 20, 1, setOf("strength", "core")),
    Quest("q7", "🧘 Yoga", "Fai 10 minuti di yoga", 25, 10, setOf("flexibility", "mindfulness")),
    Quest("q8", "🏊 Nuoto", "Fai 15 minuti di nuoto", 30, 15, setOf("cardio", "endurance", "full_body")),
    Quest("q9", "🤸 Stretching dinamico", "Fai 10 minuti di stretching dinamico", 15, 10, setOf("flexibility", "warmup")),
    Quest("q10", "🚶 Camminata veloce", "Fai 15 minuti di camminata veloce", 25, 15, setOf("cardio", "daily")),
    Quest("q11", "🏋️ Allenamento HIIT", "Fai 10 minuti di allenamento HIIT", 35, 10, setOf("cardio", "hiit", "strength")),
    Quest("q12", "🧘 Stretching", "Fai 5 minuti di stretching", 10, 5, setOf("flexibility", "cooldown")),
    Quest("q13", "🤸 Jumping Jack", "Fai 30 jumping jack", 15, 3, setOf("cardio", "hiit")),
    Quest("q14", "🦵 Affondi", "Fai 15 affondi per gamba", 20, 5, setOf("strength", "lower_body")),

    // --- BENESSERE MENTALE ---
    Quest("q15", "🧠 Meditazione", "Medita per 5 minuti", 15, 5, setOf("mindfulness", "recovery")),
    Quest("q16", "🧘 Mindfulness", "Pratica 5 minuti di mindfulness", 15, 5, setOf("mindfulness", "recovery")),
    Quest("q17", "📝 Diario", "Scrivi 3 cose per cui sei grato", 10, 5, setOf("mindfulness"),
        verification = VerificationType.PHOTO,
        verificationPrompt = "Scatta una foto delle 3 cose che hai scritto"),
    Quest("q18", "🌿 Respirazione", "Fai 10 respiri profondi", 10, 2, setOf("mindfulness", "recovery")),
    Quest("q19", "🧘 Meditazione guidata", "Ascolta una meditazione guidata di 5 minuti", 20, 5, setOf("mindfulness", "recovery")),

    // --- NUTRIZIONE E IDRATAZIONE ---
    Quest("q20", "💧 Idratazione", "Bevi 2 litri d'acqua", 15, 120, setOf("nutrition", "daily"),
        targetProgress = 2, progressUnit = "litri"),
    Quest("q21", "🍎 Alimentazione", "Mangia un frutto", 10, 5, setOf("nutrition", "daily")),
    Quest("q22", "🥗 Insalata", "Prepara e mangia un'insalata", 15, 15, setOf("nutrition")),
    Quest("q23", "🍌 Banana", "Mangia una banana prima dell'allenamento", 10, 5, setOf("nutrition"),
        verification = VerificationType.PHOTO_RECOGNITION,
        verificationPrompt = "Fai una foto con la banana che stai per mangiare",
        verificationExtra = "banana"),
    Quest("q24", "🥤 Acqua e limone", "Bevi un bicchiere d'acqua con limone", 10, 2, setOf("nutrition")),
    Quest("q25", "🍲 Pasto bilanciato", "Prepara un pasto bilanciato", 20, 20, setOf("nutrition")),

    // --- ATTIVITÀ CREATIVE E SOCIALI ---
    Quest("q26", "📖 Lettura", "Leggi un articolo sulla salute", 10, 10, setOf("mindfulness", "daily")),
    Quest("q27", "📚 Leggi un libro", "Leggi 10 pagine di un libro", 15, 15, setOf("mindfulness"),
        targetProgress = 10, progressUnit = "pagine",
        verification = VerificationType.PHOTO,
        verificationPrompt = "Scatta una foto delle pagine che hai letto"),
    Quest("q28", "🎵 Musica", "Ascolta un brano che ti motiva", 10, 3, setOf("daily"),
        verification = VerificationType.TEXT,
        verificationPrompt = "Scrivi il titolo della canzone e l'autore"),
    Quest("q29", "📞 Amici", "Chiama un amico per 5 minuti", 15, 5, setOf("social")),
    Quest("q30", "🎨 Creatività", "Disegna o colora per 10 minuti", 15, 10, setOf("mindfulness")),

    // --- ATTIVITÀ DOMESTICHE ---
    Quest("q31", "🧹 Pulizia", "Fai 10 minuti di pulizia", 10, 10, setOf("daily")),
    Quest("q32", "🌱 Piante", "Annaffia le piante", 10, 5, setOf("daily")),
    Quest("q33", "🧺 Ordine", "Riordina la scrivania", 10, 5, setOf("daily")),

    // --- SONNO E RECUPERO ---
    Quest("q34", "😴 Sonno", "Vai a letto 30 minuti prima", 15, 0, setOf("recovery")),
    Quest("q35", "🧘 Rilassamento", "Fai 5 minuti di rilassamento prima di dormire", 10, 5, setOf("recovery", "mindfulness"))
)

/**
 * Pool di missioni settimanali con maggiore difficoltà e ricompense speciali.
 * Le missioni contrassegnate con 🔁 hanno ricompense speciali.
 */
val weeklyQuestsPool = listOf(
    Quest("w1", "🏃 5 Allenamenti", "Completa 5 allenamenti questa settimana", 50, 0,
        setOf("cardio", "strength"), targetProgress = 5, progressUnit = "allenamenti"),
    Quest("w2", "🚶 Camminata 30 min", "Cammina per 30 minuti in un'unica sessione", 40, 30,
        setOf("cardio", "endurance")),
    Quest("w3", "🧘 3 Stretching", "Fai stretching per 5 minuti in 3 giorni diversi", 45, 0,
        setOf("flexibility"), targetProgress = 3, progressUnit = "giorni"),
    Quest("w4", "💪 100 Flessioni", "Completa 100 flessioni durante la settimana", 60, 0,
        setOf("strength", "upper_body"), targetProgress = 100, progressUnit = "flessioni"),
    Quest("w5", "📖 3 Articoli Salute", "Leggi 3 articoli sulla salute", 35, 0,
        setOf("mindfulness"), targetProgress = 3, progressUnit = "articoli"),
    // 🔁 w6 - Raro: Boost XP x1.5 per 3 giorni
    Quest("w6", "🏃 10 km di Corsa", "Corri per 10 km totali nella settimana", 70, 0,
        setOf("cardio", "endurance"), targetProgress = 10, progressUnit = "km",
        specialReward = SpecialReward.XpBoost(days = 3, multiplier = 1.5f)),
    Quest("w7", "🧘 5 Yoga Sessioni", "Completa 5 sessioni di yoga da 10 minuti", 55, 0,
        setOf("flexibility", "mindfulness"), targetProgress = 5, progressUnit = "sessioni"),
    Quest("w8", "💧 14 litri d'Acqua", "Bevi 14 litri d'acqua nella settimana", 40, 0,
        setOf("nutrition"), targetProgress = 14, progressUnit = "litri"),
    Quest("w9", "🍎 7 Frutti", "Mangia almeno 7 porzioni di frutta", 35, 0,
        setOf("nutrition"), targetProgress = 7, progressUnit = "porzioni"),
    Quest("w10", "🧠 5 Meditazioni", "Medita per 5 giorni della settimana", 50, 0,
        setOf("mindfulness"), targetProgress = 5, progressUnit = "giorni"),
    // 🔁 w11 - Raro: Scudo Streak
    Quest("w11", "🏋️ 3 HIIT", "Fai 3 allenamenti HIIT durante la settimana", 65, 0,
        setOf("cardio", "hiit"), targetProgress = 3, progressUnit = "allenamenti",
        specialReward = SpecialReward.StreakSave(amount = 1)),
    Quest("w12", "🚶 21 km a Piedi", "Cammina per 21 km totali nella settimana", 55, 0,
        setOf("cardio", "endurance"), targetProgress = 21, progressUnit = "km"),
    Quest("w13", "💪 200 Squat", "Completa 200 squat durante la settimana", 50, 0,
        setOf("strength", "lower_body"), targetProgress = 200, progressUnit = "squat"),
    // 🔁 w14 - Epico: Frammento di Storia
    Quest("w14", "📚 Leggi 50 pagine", "Leggi 50 pagine di un libro", 40, 0,
        setOf("mindfulness"), targetProgress = 50, progressUnit = "pagine",
        verification = VerificationType.PHOTO,
        verificationPrompt = "Scatta una foto delle pagine che hai letto",
        specialReward = SpecialReward.StoryFragment(
            chapterId = "w_chapter_extra_1",
            chapterTitle = "Il Risveglio del Drago"
        )),
    Quest("w15", "🎵 Playlist Motivazionale", "Crea una playlist motivazionale da 30 minuti", 30, 0,
        setOf("daily"),
        verification = VerificationType.TEXT,
        verificationPrompt = "Scrivi i titoli delle canzoni che hai aggiunto alla playlist")
)

/**
 * Pool di missioni mensili con alta difficoltà e ricompense speciali.
 * Le missioni contrassegnate con 🔁 hanno ricompense speciali.
 */
val monthlyQuestsPool = listOf(
    Quest("m1", "🏆 20 Allenamenti", "Completa 20 allenamenti questo mese", 100, 0,
        setOf("cardio", "strength"), targetProgress = 20, progressUnit = "allenamenti"),
    // 🔁 m2 - Epico: Boost XP x2.0 per 3 giorni
    Quest("m2", "🏃 10 km di Corsa", "Corri per 10 km totali nel mese", 120, 0,
        setOf("cardio", "endurance"), targetProgress = 10, progressUnit = "km",
        specialReward = SpecialReward.XpBoost(days = 3, multiplier = 2.0f)),
    Quest("m3", "🧘 10 Meditazioni", "Medita per 10 giorni questo mese", 90, 0,
        setOf("mindfulness"), targetProgress = 10, progressUnit = "giorni"),
    Quest("m4", "💧 60 litri d'Acqua", "Bevi 60 litri d'acqua nel mese", 80, 0,
        setOf("nutrition"), targetProgress = 60, progressUnit = "litri"),
    Quest("m5", "🍎 30 Frutti", "Mangia 30 porzioni di frutta nel mese", 70, 0,
        setOf("nutrition"), targetProgress = 30, progressUnit = "porzioni"),
    // 🔁 m6 - Epico: Boost XP x1.5 per 5 giorni
    Quest("m6", "🏃 40 km di Corsa", "Corri per 40 km totali nel mese", 140, 0,
        setOf("cardio", "endurance"), targetProgress = 40, progressUnit = "km",
        specialReward = SpecialReward.XpBoost(days = 5, multiplier = 1.5f)),
    Quest("m7", "🧘 20 Yoga Sessioni", "Completa 20 sessioni di yoga nel mese", 110, 0,
        setOf("flexibility", "mindfulness"), targetProgress = 20, progressUnit = "sessioni"),
    Quest("m8", "💪 500 Flessioni", "Completa 500 flessioni nel mese", 130, 0,
        setOf("strength", "upper_body"), targetProgress = 500, progressUnit = "flessioni"),
    // 🔁 m9 - Epico: Frammento di Storia
    Quest("m9", "📚 Leggi 2 Libri", "Leggi 2 libri interi nel mese", 100, 0,
        setOf("mindfulness"), targetProgress = 2, progressUnit = "libri",
        verification = VerificationType.PHOTO,
        verificationPrompt = "Scatta una foto di ogni libro completato",
        specialReward = SpecialReward.StoryFragment(
            chapterId = "w_chapter_extra_2",
            chapterTitle = "La Profezia dell'Eroe"
        )),
    Quest("m10", "🧠 15 Meditazioni", "Medita per 15 giorni nel mese", 105, 0,
        setOf("mindfulness"), targetProgress = 15, progressUnit = "giorni"),
    Quest("m11", "🚶 80 km a Piedi", "Cammina per 80 km totali nel mese", 120, 0,
        setOf("cardio", "endurance"), targetProgress = 80, progressUnit = "km"),
    Quest("m12", "💪 1000 Squat", "Completa 1000 squat nel mese", 150, 0,
        setOf("strength", "lower_body"), targetProgress = 1000, progressUnit = "squat"),
    Quest("m13", "💧 100 litri d'Acqua", "Bevi 100 litri d'acqua nel mese", 100, 0,
        setOf("nutrition"), targetProgress = 100, progressUnit = "litri"),
    Quest("m14", "🍎 60 Frutti", "Mangia 60 porzioni di frutta nel mese", 90, 0,
        setOf("nutrition"), targetProgress = 60, progressUnit = "porzioni"),
    // 🔁 m15 - Leggendario: Boost XP x2.0 per 7 giorni
    Quest("m15", "🏆 10 HIIT", "Completa 10 allenamenti HIIT nel mese", 160, 0,
        setOf("cardio", "hiit"), targetProgress = 10, progressUnit = "allenamenti",
        specialReward = SpecialReward.XpBoost(days = 7, multiplier = 2.0f))
)

// ================================ FUNZIONI DI PERSONALIZZAZIONE ================================

/**
 * Calcola il punteggio di rilevanza di una missione in base all'obiettivo fitness dell'utente.
 *
 * **Bonus applicati:**
 * - Missioni con verifica: +1.0
 * - Missioni con ricompensa speciale: +2.0
 * - Tag matching: peso variabile in base all'obiettivo
 *
 * @param quest La missione da valutare.
 * @param fitnessGoal L'obiettivo fitness dell'utente.
 * @return Punteggio di rilevanza (più alto = più rilevante).
 */
private fun getRelevanceScore(quest: Quest, fitnessGoal: String): Double {
    val goalWeights = when (fitnessGoal) {
        "Perdere peso" -> mapOf(
            "cardio" to 2.0, "nutrition" to 1.5, "hiit" to 1.5, "strength" to 0.8,
            "daily" to 0.5, "mindfulness" to 0.3, "flexibility" to 0.3, "recovery" to 0.5
        )
        "Tonificare" -> mapOf(
            "strength" to 2.0, "flexibility" to 1.5, "core" to 1.5, "cardio" to 0.8,
            "mindfulness" to 0.3, "nutrition" to 0.5
        )
        "Resistenza" -> mapOf(
            "cardio" to 2.0, "endurance" to 2.0, "recovery" to 1.0, "mindfulness" to 0.5,
            "strength" to 0.5, "nutrition" to 0.5
        )
        else -> emptyMap()
    }
    if (goalWeights.isEmpty()) return 0.5
    val score = quest.tags.sumOf { tag -> goalWeights[tag] ?: 0.0 }
    val verificationBonus = if (quest.verification != VerificationType.NONE) 1.0 else 0.0
    val specialBonus = if (quest.specialReward != null) 2.0 else 0.0
    return score + (quest.tags.size * 0.1) + verificationBonus + specialBonus
}

/**
 * Seleziona le missioni più rilevanti da un pool, garantendo varietà.
 *
 * **Strategia:**
 * 1. Filtra per durata massima.
 * 2. Calcola il punteggio di rilevanza per ogni missione.
 * 3. Ordina per punteggio decrescente.
 * 4. Prende i primi `count * 2` elementi.
 * 5. Seleziona casualmente `count` elementi da questo pool.
 * 6. Mescola il risultato finale per varietà.
 */
private fun selectRelevantQuests(
    pool: List<Quest>,
    count: Int,
    maxDuration: Int,
    fitnessGoal: String,
    addRandomness: Boolean = true
): List<Quest> {
    val filtered = pool.filter { it.duration <= maxDuration }
    if (filtered.size <= count) return filtered.shuffled()

    val scored = filtered.map { it to getRelevanceScore(it, fitnessGoal) }
    val sorted = scored.sortedByDescending { it.second }.map { it.first }
    val topPool = sorted.take(count * 2)
    val selected = topPool.shuffled().take(count)
    return if (addRandomness) selected.shuffled() else selected
}

/**
 * Assicura che la lista contenga almeno una missione con verifica.
 * Se non ne contiene, sostituisce l'ultima missione con una a caso tra quelle con verifica.
 */
private fun ensureVerificationQuest(selected: MutableList<Quest>, pool: List<Quest>, maxDuration: Int): MutableList<Quest> {
    val hasVerification = selected.any { it.verification != VerificationType.NONE }
    if (hasVerification) return selected

    val available = pool.filter {
        it.verification != VerificationType.NONE &&
                it.duration <= maxDuration &&
                !selected.any { sel -> sel.id == it.id }
    }
    if (available.isNotEmpty()) {
        selected[selected.lastIndex] = available.random()
    }
    return selected
}

/**
 * Assicura che la lista contenga almeno una missione con ricompensa speciale.
 * Se non ne contiene, sostituisce una missione senza ricompensa speciale.
 */
private fun ensureSpecialRewardQuest(selected: MutableList<Quest>, pool: List<Quest>, maxDuration: Int): MutableList<Quest> {
    val hasSpecial = selected.any { it.specialReward != null }
    if (hasSpecial) return selected

    val available = pool.filter {
        it.specialReward != null &&
                it.duration <= maxDuration &&
                !selected.any { sel -> sel.id == it.id }
    }
    if (available.isNotEmpty()) {
        val lastIndex = selected.indexOfLast { it.specialReward == null }
        if (lastIndex != -1) {
            selected[lastIndex] = available.random()
        }
    }
    return selected
}

// ================================ GENERAZIONE MISSIONI ================================

/**
 * Genera una missione personalizzata dall'obiettivo giornaliero.
 * Esempi: 5000 passi -> missione passi, 50 push-up -> missione push-up, ecc.
 *
 * @param dailyGoal L'obiettivo giornaliero dell'utente.
 * @return La missione personalizzata o `null` se non riconosciuta.
 */
fun getGoalQuest(dailyGoal: String): Quest? {
    return when {
        dailyGoal.contains("passi", ignoreCase = true) -> {
            val steps = dailyGoal.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 5000
            Quest(
                id = "q_daily_goal_steps",
                title = "🚶 Raggiungi $steps passi",
                description = "Cammina fino a raggiungere $steps passi oggi",
                xpReward = 30,
                duration = 0,
                tags = setOf("cardio", "daily", "steps"),
                targetProgress = steps,
                progressUnit = "passi"
            )
        }
        dailyGoal.contains("push-up", ignoreCase = true) -> {
            val count = dailyGoal.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 50
            Quest(
                id = "q_daily_goal_pushup",
                title = "💪 Fai $count push-up",
                description = "Completa $count push-up oggi",
                xpReward = 25,
                duration = 0,
                tags = setOf("strength", "daily"),
                targetProgress = count,
                progressUnit = "push-up"
            )
        }
        dailyGoal.contains("yoga", ignoreCase = true) -> {
            val minutes = dailyGoal.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 15
            Quest(
                id = "q_daily_goal_yoga",
                title = "🧘 Fai $minutes minuti di yoga",
                description = "Pratica yoga per $minutes minuti oggi",
                xpReward = 20,
                duration = minutes,
                tags = setOf("flexibility", "mindfulness", "daily")
            )
        }
        dailyGoal.contains("lettura", ignoreCase = true) -> {
            val pages = dailyGoal.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 20
            Quest(
                id = "q_daily_goal_reading",
                title = "📖 Leggi $pages pagine",
                description = "Leggi $pages pagine di un libro oggi",
                xpReward = 15,
                duration = 0,
                tags = setOf("mindfulness", "daily"),
                targetProgress = pages,
                progressUnit = "pagine"
            )
        }
        else -> null
    }
}

/**
 * Genera missioni giornaliere personalizzate, inclusa l'eventuale missione obiettivo.
 * Garantisce almeno una missione con verifica.
 *
 * @param level Livello dell'utente.
 * @param activityLevel Livello di attività ("Sedentario", "Moderato", "Attivo").
 * @param fitnessGoal Obiettivo fitness.
 * @param dailyGoal Obiettivo giornaliero (opzionale).
 * @return Lista di missioni giornaliere personalizzate.
 */
fun generateDailyQuests(
    level: Int,
    activityLevel: String,
    fitnessGoal: String,
    dailyGoal: String = ""
): List<Quest> {
    val maxDuration = when (activityLevel) {
        "Sedentario" -> 10
        "Moderato" -> 20
        else -> Int.MAX_VALUE
    }

    var selected = selectRelevantQuests(
        pool = allPossibleQuests,
        count = 7,
        maxDuration = maxDuration,
        fitnessGoal = fitnessGoal,
        addRandomness = true
    ).toMutableList()

    selected = ensureVerificationQuest(selected, allPossibleQuests, maxDuration)

    if (dailyGoal.isNotBlank() && dailyGoal != "Nessun obiettivo") {
        val goalQuest = getGoalQuest(dailyGoal)
        if (goalQuest != null && selected.none { it.id == goalQuest.id }) {
            selected.add(0, goalQuest)
        }
    }

    return selected
}

/**
 * Genera missioni settimanali personalizzate.
 * Garantisce almeno una missione con verifica e una con ricompensa speciale.
 *
 * @param level Livello dell'utente.
 * @param activityLevel Livello di attività.
 * @param fitnessGoal Obiettivo fitness.
 * @return Lista di missioni settimanali personalizzate.
 */
fun generateWeeklyQuests(level: Int, activityLevel: String, fitnessGoal: String): List<Quest> {
    val maxDuration = when (activityLevel) {
        "Sedentario" -> 15
        "Moderato" -> 30
        else -> Int.MAX_VALUE
    }

    var selected = selectRelevantQuests(
        pool = weeklyQuestsPool,
        count = 5,
        maxDuration = maxDuration,
        fitnessGoal = fitnessGoal,
        addRandomness = true
    ).toMutableList()

    selected = ensureVerificationQuest(selected, weeklyQuestsPool, maxDuration)
    selected = ensureSpecialRewardQuest(selected, weeklyQuestsPool, maxDuration)

    return selected
}

/**
 * Genera missioni mensili personalizzate.
 * Garantisce almeno una missione con verifica e una con ricompensa speciale.
 *
 * @param level Livello dell'utente.
 * @param activityLevel Livello di attività.
 * @param fitnessGoal Obiettivo fitness.
 * @return Lista di missioni mensili personalizzate.
 */
fun generateMonthlyQuests(level: Int, activityLevel: String, fitnessGoal: String): List<Quest> {
    val maxDuration = when (activityLevel) {
        "Sedentario" -> 20
        "Moderato" -> 45
        else -> Int.MAX_VALUE
    }

    var selected = selectRelevantQuests(
        pool = monthlyQuestsPool,
        count = 3,
        maxDuration = maxDuration,
        fitnessGoal = fitnessGoal,
        addRandomness = true
    ).toMutableList()

    selected = ensureVerificationQuest(selected, monthlyQuestsPool, maxDuration)
    selected = ensureSpecialRewardQuest(selected, monthlyQuestsPool, maxDuration)

    return selected
}

// ================================ RESET GIORNALIERO ================================

/**
 * Verifica se è un nuovo giorno e, in caso affermativo, resetta le missioni e il contatore passi.
 *
 * @param preferencesManager Manager delle preferenze.
 * @return `true` se è stato effettuato un reset, `false` altrimenti.
 */
suspend fun checkAndResetQuests(preferencesManager: UserPreferencesManager): Boolean {
    val today = getTodayDate()
    val lastReset = preferencesManager.getLastResetDate().first()
    if (lastReset != today) {
        preferencesManager.resetQuests()
        preferencesManager.setLastResetDate(today)
        preferencesManager.resetStepCounter()
        return true
    }
    return false
}

// ================================ UTILITY DATE ================================

/**
 * Restituisce la data odierna nel formato "yyyy-MM-dd".
 *
 * @return La data odierna come stringa.
 */
fun getTodayDate(): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return format.format(Date())
}