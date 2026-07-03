// file: ui/story/StoryScreen.kt
package com.example.cares.ui.story

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cares.data.manager.StoryChapter
import com.example.cares.data.manager.StoryManager
import com.example.cares.data.manager.UserPreferencesManager
import com.example.cares.data.repository.CaresRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ================================ STORY VIEW MODEL ================================

/**
 * ViewModel per la schermata della storia (StoryScreen).
 *
 * **Responsabilità:**
 * - Caricare l'elenco dei capitoli sbloccati dall'utente dal repository.
 * - Esporre lo stato di caricamento e i capitoli sbloccati come StateFlow.
 * - Fornire un metodo [refresh] per ricaricare i dati dopo un aggiornamento dei progressi.
 *
 * **Flusso:**
 * 1. All'inizializzazione, carica i capitoli sbloccati tramite [loadChapters].
 * 2. I dati vengono letti da DataStore tramite [CaresRepository.getUnlockedChapters].
 * 3. Lo stato di caricamento ([isLoading]) viene aggiornato durante il processo.
 *
 * @param repository Repository per l'accesso ai dati utente (capitoli sbloccati).
 */
class StoryViewModel(
    private val repository: CaresRepository
) : ViewModel() {

    // ================================ STATO ================================

    /** Insieme degli ID dei capitoli sbloccati. */
    private val _unlockedChapters = MutableStateFlow<Set<String>>(emptySet())
    val unlockedChapters: StateFlow<Set<String>> = _unlockedChapters.asStateFlow()

    /** Indica se i dati sono in fase di caricamento. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Avatar dell'utente per determinare i capitoli corretti. */
    private val _avatar = MutableStateFlow("🦸")
    val avatar: StateFlow<String> = _avatar.asStateFlow()

    // ================================ INIZIALIZZAZIONE ================================

    init {
        loadData()
    }

    // ================================ METODI PUBBLICI ================================

    /**
     * Carica i dati dell'utente: avatar e capitoli sbloccati.
     */
    private fun loadData() {
        viewModelScope.launch {
            val prefs = repository.getAllPreferences().first()
            _avatar.value = prefs[UserPreferencesManager.SELECTED_AVATAR] ?: "🦸"
            _unlockedChapters.value = repository.getUnlockedChapters().first()
            _isLoading.value = false
        }
    }

    /**
     * Ricarica i dati (utile dopo il completamento di missioni o sblocco di nuovi capitoli).
     */
    fun refresh() {
        loadData()
    }

    // ================================ FACTORY ================================

    companion object {
        fun factory(repository: CaresRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(StoryViewModel::class.java)) {
                        return StoryViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

// ================================ STORY SCREEN ================================

/**
 * Schermata che mostra tutti i capitoli della storia dell'utente.
 *
 * **Contenuto:**
 * - Header con titolo "📖 La Tua Avventura".
 * - Contatore dei capitoli sbloccati (es. "Hai sbloccato 3 capitoli su 8").
 * - Lista di tutti i capitoli, divisi in:
 *   - **Sbloccati**: visualizzati per primi, con icona, titolo e contenuto completo.
 *   - **Bloccati**: visualizzati dopo, con lucchetto e messaggio di incentivo.
 *
 * **Layout:**
 * - Utilizza una [LazyColumn] per lo scrolling efficiente di tutti i capitoli.
 * - I capitoli sbloccati e bloccati sono in una singola lista concatenata.
 *
 * **Stato:**
 * - Durante il caricamento mostra un indicatore di caricamento.
 * - I dati provengono da [StoryViewModel] e vengono aggregati tramite [StoryManager].
 *
 * @param viewModel Il ViewModel della schermata.
 */
@Composable
fun StoryScreen(viewModel: StoryViewModel) {
    // ================================ STATO ================================
    val unlockedIds by viewModel.unlockedChapters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val avatar by viewModel.avatar.collectAsState()

    // Ottiene i capitoli sbloccati e bloccati per l'avatar corrente
    val unlockedChapters = StoryManager.getUnlockedChapters(unlockedIds, avatar)
    val lockedChapters = StoryManager.getLockedChapters(unlockedIds, avatar)

    // ================================ UI ================================

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            // ---- Stato: caricamento ----
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Caricamento storia...")
            }
        } else {
            // ---- Contenuto principale ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // ---- Header ----
                Text(
                    text = "📖 La Tua Avventura",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // ---- Contatore capitoli sbloccati ----
                Text(
                    text = "Hai sbloccato ${unlockedChapters.size} capitoli su ${StoryManager.getChaptersForAvatar(avatar).size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ---- Lista capitoli ----
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Capitoli sbloccati (primi)
                    items(unlockedChapters) { chapter ->
                        ChapterItem(chapter, isUnlocked = true)
                    }
                    // Capitoli bloccati (dopo)
                    items(lockedChapters) { chapter ->
                        ChapterItem(chapter, isUnlocked = false)
                    }
                }
            }
        }
    }
}

// ================================ CHAPTER ITEM ================================

/**
 * Singolo capitolo nella lista della storia.
 *
 * **Aspetto:**
 * - **Sbloccato**: Card con sfondo surfaceVariant, mostra icona, titolo e contenuto completo.
 * - **Bloccato**: Card con sfondo semitrasparente, mostra lucchetto e titolo, con messaggio di incentivo.
 *
 * **Elevazione:**
 * - Sbloccato: 4.dp (maggiore risalto).
 * - Bloccato: 1.dp (minore risalto).
 *
 * @param chapter Il capitolo da visualizzare.
 * @param isUnlocked Se `true` il capitolo è sbloccato e viene mostrato con contenuto completo.
 */
@Composable
fun ChapterItem(
    chapter: StoryChapter,
    isUnlocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnlocked) 4.dp else 1.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isUnlocked) {
                // ---- Capitolo sbloccato ----
                Text(
                    text = "${chapter.icon} ${chapter.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = chapter.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            } else {
                // ---- Capitolo bloccato ----
                Text(
                    text = "🔒 ${chapter.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Continua ad allenarti per sbloccare questo capitolo! 💪",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}