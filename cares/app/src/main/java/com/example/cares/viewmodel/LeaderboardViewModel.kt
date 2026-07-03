// file: viewmodel/LeaderboardViewModel.kt
package com.example.cares.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cares.data.models.LeaderboardEntry
import com.example.cares.data.repository.CaresRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ================================ LEADERBOARD VIEW MODEL ================================

/**
 * ViewModel per la schermata della classifica (Leaderboard).
 *
 * **Responsabilità:**
 * - Caricare i dati della classifica dal repository (globale o amici).
 * - Gestire l'ordinamento (XP, Streak, Livello, Missioni).
 * - Gestire la modalità di visualizzazione (Globale / Amici).
 * - Calcolare e mantenere la posizione dell'utente corrente.
 *
 * **Flusso di utilizzo:**
 * 1. All'inizializzazione, carica la classifica globale.
 * 2. L'utente può cambiare modalità (amici) o ordinamento tramite l'UI.
 * 3. La posizione dell'utente viene ricalcolata automaticamente dopo ogni aggiornamento.
 * 4. Il refresh manuale è disponibile tramite [refresh] o [syncAndRefresh].
 *
 * **Cache:**
 * - I dati vengono caricati da Firestore (o fallback locale) tramite il repository.
 * - Non c'è caching interno oltre ai Flow; i dati vengono ricaricati ad ogni refresh.
 *
 * @param repository Il repository per l'accesso ai dati (Firestore + locali).
 */
class LeaderboardViewModel(
    private val repository: CaresRepository
) : ViewModel() {

    // ================================ TIPI ENUM ================================

    /**
     * Tipi di ordinamento disponibili per la classifica.
     * Determinano il campo in base al quale vengono ordinati gli utenti.
     *
     * @property XP Ordina per esperienza (XP) decrescente.
     * @property STREAK Ordina per streak (giorni consecutivi) decrescente.
     * @property LEVEL Ordina per livello decrescente.
     * @property QUESTS Ordina per numero di missioni completate decrescente.
     */
    enum class SortType {
        XP,
        STREAK,
        LEVEL,
        QUESTS
    }

    /**
     * Modalità di visualizzazione della classifica.
     * Determina l'origine dei dati da mostrare.
     *
     * @property GLOBAL Classifica globale di tutti gli utenti.
     * @property FRIENDS Classifica limitata agli amici dell'utente corrente.
     */
    enum class ViewMode {
        GLOBAL,
        FRIENDS
    }

    // ================================ STATO DELLA CLASSIFICA ================================

    /** Lista degli utenti in classifica, già ordinata secondo il criterio corrente. */
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    /** Flag che indica se i dati sono in fase di caricamento. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Posizione (1-based) dell'utente corrente nella classifica.
     * - Se l'utente è trovato, il valore è compreso tra 1 e la dimensione della lista.
     * - Se l'utente non è trovato, il valore è -1.
     */
    private val _currentUserPosition = MutableStateFlow(-1)
    val currentUserPosition: StateFlow<Int> = _currentUserPosition.asStateFlow()

    /** Tipo di ordinamento attualmente selezionato. Default: XP. */
    private val _currentSort = MutableStateFlow(SortType.XP)
    val currentSort: StateFlow<SortType> = _currentSort.asStateFlow()

    /**
     * Etichetta descrittiva del tipo di ordinamento corrente, con emoji.
     * Es: "🏅 XP", "🔥 Streak", "🏆 Livello", "📋 Missioni".
     */
    private val _sortLabel = MutableStateFlow("🏅 XP")
    val sortLabel: StateFlow<String> = _sortLabel.asStateFlow()

    /** Modalità di visualizzazione corrente. Default: GLOBAL. */
    private val _viewMode = MutableStateFlow(ViewMode.GLOBAL)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    /** Etichetta descrittiva della modalità di visualizzazione corrente, con emoji. */
    private val _viewModeLabel = MutableStateFlow("🌍 Globale")
    val viewModeLabel: StateFlow<String> = _viewModeLabel.asStateFlow()

    /**
     * Cache dei dati grezzi (prima dell'ordinamento).
     * Viene popolata da [loadLeaderboard] e utilizzata da [sortAndUpdate] per il riordinamento.
     */
    private var allEntries: List<LeaderboardEntry> = emptyList()

    // ================================ INIZIALIZZAZIONE ================================

    init {
        loadLeaderboard()
    }

    // ================================ METODI PUBBLICI ================================

    /**
     * Carica i dati della classifica dal repository in base alla modalità di visualizzazione.
     *
     * **Flusso:**
     * 1. Se la modalità è `FRIENDS`, carica la classifica degli amici tramite [CaresRepository.getFriendsLeaderboard].
     * 2. Altrimenti, carica la classifica globale tramite [CaresRepository.getGlobalLeaderboard].
     * 3. Memorizza i dati grezzi in [allEntries] e applica l'ordinamento corrente.
     * 4. Lo stato di caricamento viene gestito automaticamente.
     *
     * **Nota:** La chiamata al repository è sospendibile e viene eseguita in un coroutine.
     */
    fun loadLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            val filterByFriends = _viewMode.value == ViewMode.FRIENDS

            val data = if (filterByFriends) {
                val friends = repository.getFriends().first()
                repository.getFriendsLeaderboard(friends.toList())
            } else {
                repository.getGlobalLeaderboard()
            }

            allEntries = data
            sortAndUpdate()
            _isLoading.value = false
        }
    }

    /**
     * Sincronizza i dati dell'utente corrente su Firestore.
     * Utile per forzare l'aggiornamento dei propri dati in classifica dopo un cambiamento.
     */
    fun syncUserData() {
        viewModelScope.launch {
            repository.syncUserData()
        }
    }

    /**
     * Forza la sincronizzazione dei dati su Firestore e ricarica la classifica.
     * Aggiunge un piccolo ritardo (500ms) per assicurarsi che Firestore abbia completato la scrittura.
     */
    fun syncAndRefresh() {
        viewModelScope.launch {
            repository.syncUserData()
            delay(500L) // Attesa per la propagazione su Firestore
            loadLeaderboard()
        }
    }

    /**
     * Ricarica la classifica (alias per [loadLeaderboard]).
     * Utile per il refresh manuale dall'UI.
     */
    fun refresh() {
        loadLeaderboard()
    }

    /**
     * Cambia la modalità di visualizzazione e ricarica i dati.
     *
     * @param viewMode La nuova modalità ([ViewMode.GLOBAL] o [ViewMode.FRIENDS]).
     */
    fun setViewMode(viewMode: ViewMode) {
        _viewMode.value = viewMode
        _viewModeLabel.value = when (viewMode) {
            ViewMode.GLOBAL -> "🌍 Globale"
            ViewMode.FRIENDS -> "👥 Amici"
        }
        loadLeaderboard()
    }

    /**
     * Cambia il tipo di ordinamento e riordina i dati correnti.
     *
     * @param sortType Il nuovo tipo di ordinamento ([SortType.XP], [SortType.STREAK], ecc.).
     */
    fun setSortType(sortType: SortType) {
        _currentSort.value = sortType
        _sortLabel.value = when (sortType) {
            SortType.XP -> "🏅 XP"
            SortType.STREAK -> "🔥 Streak"
            SortType.LEVEL -> "🏆 Livello"
            SortType.QUESTS -> "📋 Missioni"
        }
        sortAndUpdate()
    }

    // ================================ FUNZIONI PRIVATE ================================

    /**
     * Riordina la lista in base al tipo di ordinamento corrente e aggiorna la posizione dell'utente.
     *
     * **Logica:**
     * 1. Ordina [allEntries] utilizzando il comparatore corrispondente a [SortType] corrente.
     * 2. Aggiorna [leaderboard] con la lista ordinata.
     * 3. Cerca l'indice dell'utente corrente (isCurrentUser = true) e calcola la posizione 1-based.
     * 4. Se non trovato, imposta -1.
     */
    private fun sortAndUpdate() {
        val sorted = when (_currentSort.value) {
            SortType.XP -> allEntries.sortedByDescending { it.xp }
            SortType.STREAK -> allEntries.sortedByDescending { it.streak }
            SortType.LEVEL -> allEntries.sortedByDescending { it.level }
            SortType.QUESTS -> allEntries.sortedByDescending { it.completedQuests }
        }
        _leaderboard.value = sorted

        // Calcola la posizione dell'utente corrente (1-based)
        val position = sorted.indexOfFirst { it.isCurrentUser }
        _currentUserPosition.value = if (position != -1) position + 1 else -1
    }

    // ================================ FACTORY ================================

    companion object {
        /**
         * Crea una Factory per il ViewModel, necessaria per l'iniezione del repository
         * nelle schermate che utilizzano `viewModel(factory = ...)`.
         *
         * **Utilizzo:**
         * ```
         * val viewModel: LeaderboardViewModel = viewModel(
         *     factory = LeaderboardViewModel.factory(repository)
         * )
         * ```
         *
         * @param repository Il repository da iniettare nel ViewModel.
         * @return Una Factory per [LeaderboardViewModel].
         */
        fun factory(repository: CaresRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
                        return LeaderboardViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}