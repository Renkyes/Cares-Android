// file: data/manager/FirebaseAuthManager.kt
package com.example.cares.data.manager

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Estensione per l'accesso al DataStore delle preferenze di autenticazione.
 */
private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

/**
 * Gestisce l'autenticazione dell'utente tramite Firebase.
 *
 * La strategia adottata è la seguente:
 * 1. Tenta il login anonimo con Firebase Auth.
 * 2. In caso di successo, utilizza l'UID fornito da Firebase come identificatore univoco.
 * 3. In caso di fallimento (es. su emulatori senza Google Play Services o assenza di rete),
 *    genera e persiste un ID utente locale, garantendo comunque il funzionamento dell'app.
 *
 * L'ID utente corrente viene mantenuto in memoria e può essere recuperato in qualsiasi momento
 * tramite [getCurrentUserId].
 */
object FirebaseAuthManager {

    // ================================ PROPRIETÀ ================================

    /** Istanza di FirebaseAuth per le operazioni di autenticazione. */
    private val auth: FirebaseAuth = Firebase.auth

    /**
     * Chiave per salvare l'ID utente locale nelle preferenze.
     * Utilizzato solo in modalità di fallback.
     */
    private const val LOCAL_USER_ID_KEY = "local_user_id"

    /**
     * ID utente corrente, che può essere:
     * - l'UID di Firebase (se l'autenticazione è riuscita)
     * - un identificatore locale persistente (in caso di fallback)
     */
    private var currentUserId: String? = null

    // ================================ API PUBBLICHE ================================

    /**
     * Esegue il login anonimo con Firebase.
     *
     * Se l'operazione ha successo, l'UID di Firebase viene salvato come ID utente corrente
     * e persistito nelle preferenze locali per eventuali usi futuri.
     * In caso di eccezione, viene generato un ID locale univoco (formato "local_user_<timestamp>")
     * e salvato in DataStore come fallback.
     *
     * @param context Il contesto necessario per accedere al DataStore in caso di fallback.
     * @return [Result.success] contenente l'ID utente (Firebase UID o locale),
     *         oppure [Result.failure] solo se si verifica un errore imprevisto durante il salvataggio.
     */
    suspend fun signInAnonymously(context: Context): Result<String> {
        return try {
            // Tenta il login anonimo su Firebase
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid ?: ""
            currentUserId = uid
            // Persiste l'UID per future sessioni (utile in caso di riavvio)
            saveLocalUserId(context, uid)
            Result.success(uid)
        } catch (_: Exception) {
            // Fallback: genera e salva un ID locale
            val localId = getOrCreateLocalUserId(context)
            currentUserId = localId
            Result.success(localId)
        }
    }

    /**
     * Restituisce l'ID utente corrente, se disponibile.
     *
     * La priorità è la seguente:
     * 1. ID in memoria (impostato dopo [signInAnonymously])
     * 2. UID di Firebase (se l'utente è già autenticato)
     *
     * @return L'ID utente come stringa, oppure `null` se nessuna autenticazione è stata effettuata.
     */
    fun getCurrentUserId(): String? {
        return currentUserId ?: auth.currentUser?.uid
    }

    /**
     * Effettua il logout dell'utente corrente.
     *
     * Rimuove l'autenticazione corrente e resetta lo stato interno.
     * L'ID utente memorizzato in memoria viene mantenuto per consentire
     * un eventuale ripristino, ma può essere sovrascritto da un nuovo login.
     */
    fun signOut() {
        auth.signOut()
        // Nota: currentUserId non viene resettato per permettere
        // un eventuale recupero dell'ID anche dopo il logout
    }

    /**
     * Esegue il login con email e password.
     *
     * @param email Email dell'utente.
     * @param password Password dell'utente.
     * @return [Result.success] contenente l'utente autenticato,
     *         oppure [Result.failure] con l'errore specifico.
     */
    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                currentUserId = user.uid
                Result.success(user)
            } else {
                Result.failure(Exception("Utente non trovato"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registra un nuovo utente con email e password.
     *
     * @param email Email del nuovo utente.
     * @param password Password del nuovo utente.
     * @return [Result.success] contenente l'utente creato,
     *         oppure [Result.failure] con l'errore specifico.
     */
    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                currentUserId = user.uid
                Result.success(user)
            } else {
                Result.failure(Exception("Errore durante la creazione dell'utente"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ================================ SUPPORTO PER ID LOCALE ================================

    /**
     * Recupera l'ID locale salvato nelle preferenze, oppure ne crea uno nuovo.
     *
     * @param context Il contesto per l'accesso al DataStore.
     * @return L'ID locale esistente o quello appena generato.
     */
    private suspend fun getOrCreateLocalUserId(context: Context): String {
        val prefs = context.dataStore
        val key = stringPreferencesKey(LOCAL_USER_ID_KEY)

        // Legge il valore attuale dalle preferenze
        val existing = prefs.data.map { it[key] }.first()
        if (existing != null) {
            return existing
        }

        // Genera un nuovo ID se non esiste
        val newId = "local_user_${System.currentTimeMillis()}"
        prefs.edit { it[key] = newId }
        return newId
    }

    /**
     * Salva l'ID utente (locale o Firebase) nelle preferenze.
     *
     * @param context Il contesto per l'accesso al DataStore.
     * @param userId  L'ID utente da salvare.
     */
    private suspend fun saveLocalUserId(context: Context, userId: String) {
        val prefs = context.dataStore
        val key = stringPreferencesKey(LOCAL_USER_ID_KEY)
        prefs.edit { it[key] = userId }
    }
}