// file: data/manager/StoryManager.kt
package com.example.cares.data.manager

/**
 * Rappresenta un capitolo della storia dell'utente.
 *
 * Ogni capitolo è associato a un personaggio specifico e contiene:
 * - Un titolo e un contenuto narrativo.
 * - Una condizione di sblocco basata su XP, livello, streak e missioni completate.
 * - Un'icona emoji rappresentativa.
 *
 * @property id Identificatore univoco del capitolo.
 * @property title Titolo visualizzato.
 * @property content Testo narrativo.
 * @property unlockCondition Lambda che determina lo sblocco in base ai progressi dell'utente.
 * @property icon Emoji rappresentativa (default: "📖").
 */
data class StoryChapter(
    val id: String,
    val title: String,
    val content: String,
    val unlockCondition: (xp: Int, level: Int, streak: Int, completedQuests: Int) -> Boolean,
    val icon: String = "📖"
)

/**
 * Gestisce i capitoli della storia, personalizzati in base al personaggio scelto.
 *
 * Ogni personaggio ha una storia unica composta da 8 capitoli che si sbloccano
 * progressivamente in base ai traguardi raggiunti dall'utente:
 * - **Missioni completate**: 0, 3, 10
 * - **Livello**: 3, 5
 * - **Streak (giorni consecutivi)**: 5, 14, 30
 *
 * **Personaggi disponibili:**
 * - 🧙 Mago - Storia incentrata sulla magia e l'arcano
 * - 🦸 Eroe - Storia incentrata sulle battaglie e l'onore
 * - 🧝 Elfo - Storia incentrata sulla natura e l'armonia
 */
object StoryManager {

    // ================================ CAPITOLI DEL MAGO (🧙) ================================

    /**
     * Capitoli della storia per il personaggio Mago.
     * Ogni capitolo si sblocca al raggiungimento di specifici traguardi.
     */
    private val wizardChapters = listOf(
        StoryChapter(
            id = "w_chapter_1",
            title = "🔮 La Torre del Sapere",
            content = "Sei un giovane mago che si risveglia in una torre dimenticata. I tuoi primi passi nel mondo della magia ti porteranno a scoprire antichi segreti.",
            unlockCondition = { _, _, _, completedQuests -> completedQuests >= 0 }
        ),
        StoryChapter(
            id = "w_chapter_2",
            title = "📜 Il Grimorio Perduto",
            content = "Hai trovato un antico grimorio. Per comprenderne il potere, devi completare almeno 3 incantesimi (missioni).",
            unlockCondition = { _, _, _, completedQuests -> completedQuests >= 3 }
        ),
        StoryChapter(
            id = "w_chapter_3",
            title = "✨ La Fiamma Eterna",
            content = "La tua costanza nello studio (5 giorni consecutivi) ha acceso la Fiamma Eterna nella torre.",
            unlockCondition = { _, _, streak, _ -> streak >= 5 }
        ),
        StoryChapter(
            id = "w_chapter_4",
            title = "⚡ Il Livello dell'Arcano",
            content = "Hai raggiunto il livello 3. Ora puoi accedere ai segreti più profondi della magia arcana.",
            unlockCondition = { _, level, _, _ -> level >= 3 }
        ),
        StoryChapter(
            id = "w_chapter_5",
            title = "🏆 Il Consiglio dei Maghi",
            content = "Con 10 missioni completate, il Consiglio dei Maghi ti ha convocato. Una nuova era sta per iniziare.",
            unlockCondition = { _, _, _, completedQuests -> completedQuests >= 10 }
        ),
        StoryChapter(
            id = "w_chapter_6",
            title = "⭐ Il Drago di Cristallo",
            content = "Al livello 5, affronti il Drago di Cristallo, una creatura fatta di pura energia magica.",
            unlockCondition = { _, level, _, _ -> level >= 5 }
        ),
        StoryChapter(
            id = "w_chapter_7",
            title = "👑 L'Impero delle Ombre",
            content = "Dopo 14 giorni di studio, l'Impero delle Ombre ti ha notato. Sei diventato una minaccia per i loro piani.",
            unlockCondition = { _, _, streak, _ -> streak >= 14 }
        ),
        StoryChapter(
            id = "w_chapter_8",
            title = "🌟 L'Apice del Mago",
            content = "30 giorni di costanza. Sei diventato il mago più potente della storia. Ora il mondo intero ti teme e ti rispetta.",
            unlockCondition = { _, _, streak, _ -> streak >= 30 }
        )
    )

    // ================================ CAPITOLI DELL'EROE (🦸) ================================

    /**
     * Capitoli della storia per il personaggio Eroe.
     * Ogni capitolo si sblocca al raggiungimento di specifici traguardi.
     */
    private val heroChapters = listOf(
        StoryChapter(
            id = "h_chapter_1",
            title = "⚔️ Il Risveglio del Guerriero",
            content = "Sei un eroe guerriero, il tuo destino è scritto nelle stelle. La prima battaglia ti attende.",
            unlockCondition = { _, _, _, completedQuests -> completedQuests >= 0 }
        ),
        StoryChapter(
            id = "h_chapter_2",
            title = "🛡️ La Sfida del Campione",
            content = "Hai superato le prime 3 battaglie (missioni). Ora sei un campione riconosciuto.",
            unlockCondition = { _, _, _, completedQuests -> completedQuests >= 3 }
        ),
        StoryChapter(
            id = "h_chapter_3",
            title = "🔥 La Fiamma del Guerriero",
            content = "La tua costanza (5 giorni consecutivi) ha acceso la Fiamma del Guerriero.",
            unlockCondition = { _, _, streak, _ -> streak >= 5 }
        ),
        StoryChapter(
            id = "h_chapter_4",
            title = "🏅 Il Livello dell'Eroe",
            content = "Al livello 3, vieni incoronato eroe del regno.",
            unlockCondition = { _, level, _, _ -> level >= 3 }
        ),
        StoryChapter(
            id = "h_chapter_5",
            title = "🏆 La Lega degli Eroi",
            content = "10 missioni completate. Sei stato invitato alla Lega degli Eroi.",
            unlockCondition = { _, _, _, completedQuests -> completedQuests >= 10 }
        ),
        StoryChapter(
            id = "h_chapter_6",
            title = "🐉 Il Drago dell'Ombra",
            content = "Al livello 5, affronti il temibile Drago dell'Ombra.",
            unlockCondition = { _, level, _, _ -> level >= 5 }
        ),
        StoryChapter(
            id = "h_chapter_7",
            title = "👑 La Corona dell'Imperatore",
            content = "14 giorni di costanza. L'Imperatore ti offre un posto nella sua corte.",
            unlockCondition = { _, _, streak, _ -> streak >= 14 }
        ),
        StoryChapter(
            id = "h_chapter_8",
            title = "🌟 L'Apice dell'Eroe",
            content = "30 giorni di costanza. Sei diventato una leggenda vivente.",
            unlockCondition = { _, _, streak, _ -> streak >= 30 }
        )
    )

    // ================================ CAPITOLI DELL'ELFO (🧝) ================================

    /**
     * Capitoli della storia per il personaggio Elfo.
     * Ogni capitolo si sblocca al raggiungimento di specifici traguardi.
     */
    private val elfChapters = listOf(
        StoryChapter(
            id = "e_chapter_1",
            title = "🌿 La Foresta Antica",
            content = "Sei un elfo antico, nato nella foresta millenaria. La natura ti chiama.",
            unlockCondition = { _, _, _, completedQuests -> completedQuests >= 0 }
        ),
        StoryChapter(
            id = "e_chapter_2",
            title = "🌳 Il Patto della Natura",
            content = "3 missioni completate. La foresta ti ha riconosciuto come suo guardiano.",
            unlockCondition = { _, _, _, completedQuests -> completedQuests >= 3 }
        ),
        StoryChapter(
            id = "e_chapter_3",
            title = "🍃 La Danza del Vento",
            content = "5 giorni consecutivi di armonia con la natura.",
            unlockCondition = { _, _, streak, _ -> streak >= 5 }
        ),
        StoryChapter(
            id = "e_chapter_4",
            title = "🌲 Il Livello del Guardiano",
            content = "Al livello 3, diventi guardiano ufficiale della foresta.",
            unlockCondition = { _, level, _, _ -> level >= 3 }
        ),
        StoryChapter(
            id = "e_chapter_5",
            title = "🏆 Il Consiglio degli Elfi",
            content = "10 missioni completate. Il Consiglio degli Elfi ti ha convocato.",
            unlockCondition = { _, _, _, completedQuests -> completedQuests >= 10 }
        ),
        StoryChapter(
            id = "e_chapter_6",
            title = "🐉 Il Drago di Cristallo",
            content = "Al livello 5, proteggi la foresta dal Drago di Cristallo.",
            unlockCondition = { _, level, _, _ -> level >= 5 }
        ),
        StoryChapter(
            id = "e_chapter_7",
            title = "👑 La Regina della Foresta",
            content = "14 giorni di costanza. La Regina degli Elfi ti ha scelto come suo erede.",
            unlockCondition = { _, _, streak, _ -> streak >= 14 }
        ),
        StoryChapter(
            id = "e_chapter_8",
            title = "🌟 L'Apice dell'Elfo",
            content = "30 giorni di costanza. Sei diventato l'elfo più potente della storia.",
            unlockCondition = { _, _, streak, _ -> streak >= 30 }
        )
    )

    // ================================ MAPPA DEI CAPITOLI ================================

    /**
     * Mappa che associa ogni personaggio alla sua lista di capitoli.
     * Utilizzata per recuperare i capitoli in base all'avatar selezionato.
     */
    private val chaptersMap = mapOf(
        "🧙" to wizardChapters,
        "🦸" to heroChapters,
        "🧝" to elfChapters
    )

    // ================================ API PUBBLICHE ================================

    /**
     * Restituisce i capitoli della storia per un determinato avatar/personaggio.
     *
     * @param avatar L'emoji del personaggio (es. "🧙", "🦸", "🧝").
     * @return La lista dei capitoli per il personaggio selezionato,
     *         oppure i capitoli dell'Eroe come default se l'avatar non è riconosciuto.
     */
    fun getChaptersForAvatar(avatar: String): List<StoryChapter> {
        return chaptersMap[avatar] ?: heroChapters
    }

    /**
     * Verifica quali capitoli non ancora sbloccati soddisfano le condizioni di sblocco.
     *
     * Utile per controllare periodicamente se l'utente ha sbloccato nuovi capitoli
     * dopo aver completato missioni o aumentato di livello.
     *
     * @param xp L'esperienza attuale dell'utente.
     * @param level Il livello attuale dell'utente.
     * @param streak Lo streak giornaliero dell'utente.
     * @param completedQuests Il numero di missioni completate dall'utente.
     * @param unlockedChapters L'insieme degli ID dei capitoli già sbloccati.
     * @param avatar L'emoji del personaggio selezionato.
     * @return Lista dei capitoli che possono essere sbloccati.
     */
    fun checkUnlockedChapters(
        xp: Int,
        level: Int,
        streak: Int,
        completedQuests: Int,
        unlockedChapters: Set<String>,
        avatar: String
    ): List<StoryChapter> {
        val allChapters = getChaptersForAvatar(avatar)
        return allChapters.filter { chapter ->
            !unlockedChapters.contains(chapter.id) &&
                    chapter.unlockCondition(xp, level, streak, completedQuests)
        }
    }

    /**
     * Restituisce i capitoli già sbloccati dall'utente.
     *
     * @param unlockedIds L'insieme degli ID dei capitoli sbloccati.
     * @param avatar L'emoji del personaggio selezionato.
     * @return Lista dei capitoli sbloccati, ordinati secondo l'ordine originale.
     */
    fun getUnlockedChapters(unlockedIds: Set<String>, avatar: String): List<StoryChapter> {
        val allChapters = getChaptersForAvatar(avatar)
        return allChapters.filter { unlockedIds.contains(it.id) }
    }

    /**
     * Restituisce i capitoli ancora da sbloccare dall'utente.
     *
     * @param unlockedIds L'insieme degli ID dei capitoli sbloccati.
     * @param avatar L'emoji del personaggio selezionato.
     * @return Lista dei capitoli bloccati, ordinati secondo l'ordine originale.
     */
    fun getLockedChapters(unlockedIds: Set<String>, avatar: String): List<StoryChapter> {
        val allChapters = getChaptersForAvatar(avatar)
        return allChapters.filter { !unlockedIds.contains(it.id) }
    }
}