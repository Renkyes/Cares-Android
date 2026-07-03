// file: ui/friends/FriendsScreen.kt
package com.example.cares.ui.friends

import android.util.Log
import androidx.compose.animation.*
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
import com.example.cares.data.models.FriendRequest
import com.example.cares.data.models.LeaderboardEntry
import com.example.cares.data.repository.CaresRepository
import com.example.cares.ui.theme.Gradients
import com.example.cares.ui.theme.Neon
import com.example.cares.ui.theme.GlassCard
import com.example.cares.ui.animation.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ================================ FRIENDS VIEW MODEL ================================

/**
 * ViewModel per la schermata degli amici.
 *
 * **Responsabilità:**
 * - Gestire la lista degli amici dell'utente.
 * - Gestire le richieste di amicizia ricevute e inviate.
 * - Gestire le operazioni di accettazione, rifiuto e rimozione amici.
 * - Caricare i dati all'inizializzazione e dopo ogni operazione.
 *
 * **Flusso dei dati:**
 * 1. All'inizializzazione, carica amici e richieste da Firestore.
 * 2. L'utente può accettare/rifiutare richieste o rimuovere amici.
 * 3. Dopo ogni operazione, i dati vengono ricaricati automaticamente.
 *
 * @param repository Repository per l'accesso ai dati e alle operazioni di rete.
 */
class FriendsViewModel(
    private val repository: CaresRepository
) : ViewModel() {

    // ================================ STATO ================================

    /** Richieste di amicizia ricevute (in attesa). */
    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests.asStateFlow()

    /** Richieste di amicizia inviate (in attesa). */
    private val _sentRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val sentRequests: StateFlow<List<FriendRequest>> = _sentRequests.asStateFlow()

    /** Lista degli amici dell'utente. */
    private val _friends = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val friends: StateFlow<List<LeaderboardEntry>> = _friends.asStateFlow()

    /** Indica se i dati sono in fase di caricamento. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Indica se l'utente corrente è in modalità locale (non registrato). */
    private val _isLocalUser = MutableStateFlow(false)
    val isLocalUser: StateFlow<Boolean> = _isLocalUser.asStateFlow()

    // ================================ INIZIALIZZAZIONE ================================

    init {
        loadData()
    }

    // ================================ METODI PUBBLICI ================================

    /**
     * Carica tutti i dati: amici, richieste ricevute e inviate.
     *
     * **Flusso:**
     * 1. Imposta `isLoading = true`.
     * 2. Carica le richieste ricevute.
     * 3. Carica le richieste inviate.
     * 4. Carica la lista degli amici.
     * 5. Imposta `isLoading = false`.
     */
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = FirebaseAuthManager.getCurrentUserId()
            _isLocalUser.value = userId?.startsWith("local_user_") == true

            // Carica richieste ricevute
            val pending = repository.getPendingFriendRequests()
            Log.d("FriendsVM", "Pending requests: ${pending.size}")
            _pendingRequests.value = pending

            // Carica richieste inviate
            val sent = repository.getSentFriendRequests()
            Log.d("FriendsVM", "Sent requests: ${sent.size}")
            _sentRequests.value = sent

            // Carica lista amici
            val friendIds = repository.getFriends().first()
            if (friendIds.isNotEmpty()) {
                _friends.value = repository.getFriendsLeaderboard(friendIds.toList())
            } else {
                _friends.value = emptyList()
            }

            _isLoading.value = false
        }
    }

    /**
     * Accetta una richiesta di amicizia.
     *
     * @param requestId ID della richiesta da accettare.
     */
    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(requestId)
            loadData() // Ricarica i dati dopo l'operazione
        }
    }

    /**
     * Rifiuta una richiesta di amicizia.
     *
     * @param requestId ID della richiesta da rifiutare.
     */
    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            repository.rejectFriendRequest(requestId)
            loadData() // Ricarica i dati dopo l'operazione
        }
    }

    /**
     * Rimuove un amico dalla lista.
     *
     * @param friendId ID dell'amico da rimuovere.
     */
    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            repository.removeFriend(friendId)
            loadData() // Ricarica i dati dopo l'operazione
        }
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
                    if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
                        return FriendsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

// ================================ FRIENDS SCREEN ================================

/**
 * Schermata che mostra gli amici e le richieste di amicizia dell'utente.
 *
 * **Sezioni:**
 * 1. **Richieste ricevute**: richieste di amicizia in attesa da altri utenti.
 * 2. **Richieste inviate**: richieste inviate in attesa di risposta.
 * 3. **Amici**: lista degli amici attuali con XP e livello.
 *
 * **Funzionalità:**
 * - Accettare/rifiutare richieste ricevute.
 * - Visualizzare lo stato delle richieste inviate.
 * - Rimuovere amici dalla lista.
 * - Navigare alla schermata di aggiunta amici.
 *
 * @param navController Controller di navigazione.
 * @param viewModel ViewModel per la gestione della logica.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 */
@Composable
fun FriendsScreen(
    navController: NavController,
    viewModel: FriendsViewModel,
    isDarkTheme: Boolean = true
) {
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val sentRequests by viewModel.sentRequests.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLocalUser by viewModel.isLocalUser.collectAsState()

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
                Text(
                    text = "👥 Amici",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Pulsante per aggiungere amici
                IconButton(
                    onClick = { navController.navigate("add_friend") },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Neon.Green.copy(alpha = 0.2f))
                ) {
                    Text("➕", fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Messaggio per utenti locali (non registrati)
            if (isLocalUser) {
                GlassCard(modifier = Modifier.fillMaxWidth(), darkTheme = isDarkTheme) {
                    Text(
                        text = "🔒 Per utilizzare le funzionalità social, accedi con un account registrato.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ================================ CONTENUTO ================================

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Neon.Green)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ---- Richieste in arrivo ----
                    if (pendingRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = "📥 Richieste ricevute (${pendingRequests.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        items(pendingRequests) { request ->
                            PendingRequestItem(
                                request = request,
                                onAccept = { viewModel.acceptRequest(request.id) },
                                onReject = { viewModel.rejectRequest(request.id) },
                                isDarkTheme = isDarkTheme
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // ---- Richieste inviate ----
                    if (sentRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = "📤 Richieste inviate (${sentRequests.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        items(sentRequests) { request ->
                            SentRequestItem(
                                request = request,
                                isDarkTheme = isDarkTheme
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // ---- Amici ----
                    if (friends.isNotEmpty()) {
                        item {
                            Text(
                                text = "👫 I tuoi amici (${friends.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        items(friends) { friend ->
                            if (!friend.isCurrentUser) {
                                FriendItem(
                                    friend = friend,
                                    onRemove = { viewModel.removeFriend(friend.userId) },
                                    isDarkTheme = isDarkTheme
                                )
                            }
                        }
                    } else if (pendingRequests.isEmpty() && sentRequests.isEmpty()) {
                        // Stato: nessun amico e nessuna richiesta
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("👤", fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Non hai ancora amici. Cerca e aggiungi qualcuno!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { navController.navigate("add_friend") }
                                    ) {
                                        Text("🔍 Cerca utenti")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================================ COMPONENTI ================================

/**
 * Item per una richiesta di amicizia ricevuta.
 *
 * **Caratteristiche:**
 * - Mostra avatar e username del mittente.
 * - Pulsanti "Accetta" e "Rifiuta".
 * - Animazione di entrata con [AnimatedEntrance].
 *
 * @param request La richiesta di amicizia.
 * @param onAccept Callback eseguito quando si accetta la richiesta.
 * @param onReject Callback eseguito quando si rifiuta la richiesta.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 */
@Composable
fun PendingRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    isDarkTheme: Boolean
) {
    AnimatedEntrance {
        GlassCard(modifier = Modifier.fillMaxWidth(), darkTheme = isDarkTheme) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Informazioni del mittente
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = request.fromAvatar, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = request.fromUsername,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "📨 Richiesta di amicizia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                // Pulsanti di azione
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Neon.Green.copy(alpha = 0.2f))
                    ) {
                        Text("✅", fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = onReject,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFFF1744).copy(alpha = 0.2f))
                    ) {
                        Text("❌", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

/**
 * Item per una richiesta di amicizia inviata.
 *
 * **Caratteristiche:**
 * - Mostra avatar e username del destinatario.
 * - Stato "In attesa..." con colore arancione.
 * - Animazione di entrata con [AnimatedEntrance].
 *
 * @param request La richiesta di amicizia inviata.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 */
@Composable
fun SentRequestItem(
    request: FriendRequest,
    isDarkTheme: Boolean
) {
    AnimatedEntrance {
        GlassCard(modifier = Modifier.fillMaxWidth(), darkTheme = isDarkTheme) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Informazioni del destinatario
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = request.toAvatar, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = request.toUsername,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "⏳ In attesa...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Neon.Orange
                        )
                    }
                }
                // Icona di stato
                Text(
                    text = "📤",
                    fontSize = 24.sp
                )
            }
        }
    }
}

/**
 * Item per un amico nella lista.
 *
 * **Caratteristiche:**
 * - Mostra avatar, username, XP e livello dell'amico.
 * - Pulsante "Rimuovi" per rimuovere l'amico.
 * - Animazione di entrata con [AnimatedEntrance].
 *
 * @param friend L'amico da visualizzare.
 * @param onRemove Callback eseguito quando si rimuove l'amico.
 * @param isDarkTheme Se `true`, applica il tema scuro.
 */
@Composable
fun FriendItem(
    friend: LeaderboardEntry,
    onRemove: () -> Unit,
    isDarkTheme: Boolean
) {
    AnimatedEntrance {
        GlassCard(modifier = Modifier.fillMaxWidth(), darkTheme = isDarkTheme) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Informazioni dell'amico
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = friend.avatar, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = friend.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "⭐ ${friend.xp} XP • 🏆 Lv.${friend.level}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                // Pulsante rimuovi
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFF1744).copy(alpha = 0.1f))
                ) {
                    Text("✖️", fontSize = 16.sp)
                }
            }
        }
    }
}