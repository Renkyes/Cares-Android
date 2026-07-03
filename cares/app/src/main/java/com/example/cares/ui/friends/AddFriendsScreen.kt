// file: ui/friends/AddFriendScreen.kt
package com.example.cares.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.cares.data.manager.FirebaseAuthManager
import com.example.cares.data.models.LeaderboardEntry
import com.example.cares.data.repository.CaresRepository
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import com.example.cares.ui.theme.GlassCard
import com.example.cares.ui.animation.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ================================ VIEWMODEL ================================

/**
 * ViewModel per la schermata di aggiunta amici.
 *
 * **Responsabilità:**
 * - Gestire la ricerca di utenti tramite il repository.
 * - Applicare debounce alla ricerca per ottimizzare le chiamate API.
 * - Gestire lo stato di caricamento e i risultati della ricerca.
 * - Gestire l'invio delle richieste di amicizia.
 *
 * **Flusso di ricerca:**
 * 1. L'utente digita nel campo di ricerca.
 * 2. La funzione [searchUsers] applica un debounce di 500ms.
 * 3. Viene eseguita la ricerca tramite [repository.searchUsers].
 * 4. I risultati vengono pubblicati in [searchResults].
 *
 * @param repository Repository per l'accesso ai dati e alle operazioni di rete.
 */
class AddFriendViewModel(
    private val repository: CaresRepository
) : ViewModel() {

    // ================================ STATO ================================

    /** Risultati della ricerca utenti. */
    private val _searchResults = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val searchResults: StateFlow<List<LeaderboardEntry>> = _searchResults.asStateFlow()

    /** Indica se la ricerca è in corso. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Indica se l'utente corrente è in modalità locale (non registrato). */
    private val _isLocalUser = MutableStateFlow(false)
    val isLocalUser: StateFlow<Boolean> = _isLocalUser.asStateFlow()

    /** Query di ricerca corrente. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Job per la ricerca con debounce. */
    private var searchJob: kotlinx.coroutines.Job? = null

    // ================================ INIZIALIZZAZIONE ================================

    init {
        val userId = FirebaseAuthManager.getCurrentUserId()
        _isLocalUser.value = userId?.startsWith("local_user_") == true
    }

    // ================================ METODI PUBBLICI ================================

    /**
     * Cerca utenti per username (parziale).
     *
     * **Comportamento:**
     * - Se la query è < 2 caratteri, resetta i risultati.
     * - Applica un debounce di 500ms per evitare chiamate API eccessive.
     * - Cancella la ricerca precedente se in corso.
     *
     * @param query La stringa di ricerca.
     */
    fun searchUsers(query: String) {
        _searchQuery.value = query
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            _isLoading.value = true
            val results = repository.searchUsers(query)
            _searchResults.value = results
            _isLoading.value = false
        }
    }

    /**
     * Invia una richiesta di amicizia a un utente.
     *
     * @param userId ID dell'utente destinatario.
     * @return `true` se la richiesta è stata inviata con successo.
     */
    suspend fun sendFriendRequest(userId: String): Boolean {
        return repository.sendFriendRequest(userId)
    }

    // ================================ FACTORY ================================

    companion object {
        /**
         * Factory per creare il ViewModel con il repository.
         */
        fun factory(repository: CaresRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AddFriendViewModel::class.java)) {
                        return AddFriendViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

// ================================ SCHERMATA ================================

/**
 * Schermata per aggiungere nuovi amici tramite ricerca.
 *
 * **Caratteristiche:**
 * - Campo di ricerca con debounce per cercare utenti.
 * - Visualizzazione dei risultati in una lista con card glassmorphism.
 * - Gestione degli stati: caricamento, nessun risultato, utente locale.
 * - Dialog di conferma per l'invio della richiesta di amicizia.
 *
 * **Flusso:**
 * 1. L'utente digita nel campo di ricerca.
 * 2. Vengono mostrati i risultati della ricerca.
 * 3. L'utente clicca su "Aggiungi" per inviare una richiesta.
 * 4. Un dialog conferma l'invio o mostra un errore.
 *
 * @param navController Controller di navigazione.
 * @param viewModel ViewModel per la gestione della logica.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 */
@Composable
fun AddFriendScreen(
    navController: NavController,
    viewModel: AddFriendViewModel,
    isDarkTheme: Boolean = true
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLocalUser by viewModel.isLocalUser.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Gradients.mainBackground(isDarkTheme))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            // ================================ HEADER ================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Text("←", fontSize = 24.sp, color = Color.White)
                }
                Text(
                    text = "🔍 Aggiungi amico",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================================ CAMPO DI RICERCA ================================

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchUsers(it) },
                label = { Text("Cerca per nome") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Neon.Green,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    cursorColor = Neon.Green
                ),
                leadingIcon = { Text("🔍", fontSize = 18.sp) },
                enabled = !isLocalUser
            )

            // Messaggio per utenti locali (non registrati)
            if (isLocalUser) {
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), darkTheme = isDarkTheme) {
                    Text(
                        text = "🔒 Accedi con un account registrato per trovare amici.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================================ RISULTATI ================================

            when {
                // Stato: caricamento
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Neon.Green)
                    }
                }
                // Stato: nessun risultato
                searchQuery.length >= 2 && searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nessun utente trovato",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                // Stato: risultati
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { user ->
                            SearchResultItem(
                                user = user,
                                onAddClick = {
                                    viewModel.viewModelScope.launch {
                                        val success = viewModel.sendFriendRequest(user.userId)
                                        if (success) {
                                            showSuccessDialog = true
                                        } else {
                                            errorMessage = "Richiesta già inviata o errore"
                                        }
                                    }
                                },
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }
        }
    }

    // ================================ DIALOG ================================

    // Dialog di conferma richiesta inviata
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("✅ Richiesta inviata!") },
            text = { Text("La richiesta di amicizia è stata inviata.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    navController.navigateUp()
                }) {
                    Text("Ok")
                }
            }
        )
    }

    // Dialog di errore
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("❌ Errore") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("Ok")
                }
            }
        )
    }
}

// ================================ ITEM DEI RISULTATI ================================

/**
 * Item della lista dei risultati di ricerca.
 *
 * **Caratteristiche:**
 * - Mostra avatar, username, XP e livello dell'utente.
 * - Pulsante "Aggiungi" per inviare una richiesta di amicizia.
 * - Animazione di entrata con [AnimatedEntrance].
 *
 * @param user L'utente da visualizzare.
 * @param onAddClick Callback eseguito quando si clicca su "Aggiungi".
 * @param isDarkTheme Se `true`, applica il tema scuro.
 */
@Composable
fun SearchResultItem(
    user: LeaderboardEntry,
    onAddClick: () -> Unit,
    isDarkTheme: Boolean
) {
    AnimatedEntrance {
        GlassCard(modifier = Modifier.fillMaxWidth(), darkTheme = isDarkTheme) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Informazioni utente
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = user.avatar, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "⭐ ${user.xp} XP • 🏆 Lv.${user.level}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Pulsante aggiungi
                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Neon.Green,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "➕ Aggiungi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}