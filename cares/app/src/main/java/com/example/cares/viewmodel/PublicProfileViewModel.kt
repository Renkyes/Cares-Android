// file: viewmodel/PublicProfileViewModel.kt
package com.example.cares.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cares.data.models.PublicUserProfile
import com.example.cares.data.repository.CaresRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ================================ PUBLIC PROFILE VIEW MODEL ================================

/**
 * ViewModel per la schermata del profilo pubblico di un utente.
 *
 * **Scopo:**
 * Caricare e gestire i dati del profilo pubblico di un utente specifico,
 * visualizzabile da altri utenti dell'applicazione.
 *
 * **Responsabilità:**
 * - Caricare il profilo pubblico dell'utente specificato dal repository.
 * - Esporre lo stato di caricamento e i dati del profilo come StateFlow.
 * - Fornire un metodo [refresh] per ricaricare i dati.
 *
 * **Flusso di utilizzo:**
 * 1. Il ViewModel viene creato con un `userId` specifico.
 * 2. All'inizializzazione, carica il profilo tramite [loadProfile].
 * 3. I dati vengono recuperati da Firestore tramite [CaresRepository.getPublicUserProfile].
 * 4. Lo stato di caricamento ([isLoading]) viene aggiornato durante il processo.
 * 5. L'UI osserva [profile] e [isLoading] per mostrare i dati o lo stato di caricamento.
 *
 * **Differenze con il profilo privato:**
 * - Il profilo privato ([ProfileViewModel]) contiene impostazioni e dati sensibili.
 * - Il profilo pubblico ([PublicProfileViewModel]) contiene solo statistiche condivisibili.
 *
 * @param repository Repository per l'accesso ai dati.
 * @param userId ID dell'utente di cui visualizzare il profilo pubblico.
 */
class PublicProfileViewModel(
    private val repository: CaresRepository,
    private val userId: String
) : ViewModel() {

    // ================================ STATO ================================

    /**
     * Dati del profilo pubblico dell'utente.
     * `null` se il profilo non è stato ancora caricato o l'utente non esiste.
     */
    private val _profile = MutableStateFlow<PublicUserProfile?>(null)
    val profile: StateFlow<PublicUserProfile?> = _profile.asStateFlow()

    /** Indica se i dati sono in fase di caricamento. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ================================ INIZIALIZZAZIONE ================================

    init {
        loadProfile()
    }

    // ================================ METODI PUBBLICI ================================

    /**
     * Ricarica i dati del profilo.
     * Utile dopo un aggiornamento dei dati dell'utente (es. nuovo badge, nuovo capitolo).
     */
    fun refresh() {
        loadProfile()
    }

    // ================================ FUNZIONI PRIVATE ================================

    /**
     * Carica il profilo pubblico dell'utente dal repository.
     * Aggiorna lo stato di caricamento durante l'operazione.
     */
    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _profile.value = repository.getPublicUserProfile(userId)
            _isLoading.value = false
        }
    }

    // ================================ FACTORY ================================

    companion object {
        /**
         * Crea una Factory per il ViewModel, necessaria per l'iniezione del repository
         * e dell'ID utente nelle schermate che utilizzano `viewModel(factory = ...)`.
         *
         * **Utilizzo:**
         * ```
         * val viewModel: PublicProfileViewModel = viewModel(
         *     factory = PublicProfileViewModel.factory(repository, userId)
         * )
         * ```
         *
         * @param repository Il repository da iniettare nel ViewModel.
         * @param userId L'ID dell'utente di cui visualizzare il profilo.
         * @return Una Factory per [PublicProfileViewModel].
         */
        fun factory(repository: CaresRepository, userId: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(PublicProfileViewModel::class.java)) {
                        return PublicProfileViewModel(repository, userId) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}