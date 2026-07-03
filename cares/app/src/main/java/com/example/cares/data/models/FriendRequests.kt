// file: data/models/FriendRequest.kt
package com.example.cares.data.models

/**
 * Rappresenta una richiesta di amicizia tra due utenti.
 *
 * Questo modello viene utilizzato per gestire il flusso di richieste di amicizia
 * nell'applicazione, includendo:
 * - Invio di nuove richieste di amicizia.
 * - Visualizzazione delle richieste in sospeso.
 * - Accettazione o rifiuto delle richieste ricevute.
 * - Tracciamento dello stato di ogni richiesta.
 *
 * **Flusso tipico:**
 * 1. L'utente A invia una richiesta a B → stato `PENDING`.
 * 2. L'utente B visualizza la richiesta nella lista delle notifiche.
 * 3. L'utente B accetta → stato `ACCEPTED`, i due diventano amici.
 * 4. L'utente B rifiuta → stato `REJECTED`, la richiesta viene archiviata.
 *
 * @property id Identificatore univoco della richiesta (generato da Firestore).
 * @property fromUserId ID dell'utente che ha inviato la richiesta.
 * @property toUserId ID dell'utente destinatario della richiesta.
 * @property status Stato corrente della richiesta ([RequestStatus]).
 * @property timestamp Momento in cui la richiesta è stata creata (millisecondi).
 * @property fromUsername Nome utente del mittente (cached per ridurre letture DB).
 * @property fromAvatar Avatar del mittente (cached per ridurre letture DB).
 * @property toUsername Nome utente del destinatario (cached per ridurre letture DB).
 * @property toAvatar Avatar del destinatario (cached per ridurre letture DB).
 *
 * @see RequestStatus
 */
data class FriendRequest(
    val id: String = "",
    val fromUserId: String,
    val toUserId: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val fromUsername: String = "",
    val fromAvatar: String = "🧙",
    val toUsername: String = "",
    val toAvatar: String = "🧙"
) {

    /**
     * Enum che rappresenta i possibili stati di una richiesta di amicizia.
     *
     * @property PENDING Richiesta in attesa di risposta.
     * @property ACCEPTED Richiesta accettata (i due utenti sono ora amici).
     * @property REJECTED Richiesta rifiutata (l'utente ha scelto di non accettarla).
     */
    enum class RequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    /**
     * Converte la richiesta in una mappa per il salvataggio su Firestore.
     *
     * Tutti i campi vengono serializzati in tipi compatibili con Firestore
     * (String, Number, Boolean, Map, List).
     *
     * @return Mappa con tutti i campi della richiesta.
     */
    fun toMap(): Map<String, Any> = mapOf(
        "fromUserId" to fromUserId,
        "toUserId" to toUserId,
        "status" to status.name,
        "timestamp" to timestamp,
        "fromUsername" to fromUsername,
        "fromAvatar" to fromAvatar,
        "toUsername" to toUsername,
        "toAvatar" to toAvatar
    )

    companion object {
        /**
         * Crea un'istanza di [FriendRequest] a partire da una mappa e un ID.
         *
         * Utilizzato per deserializzare i dati da Firestore.
         * I campi mancanti o con tipi non validi vengono sostituiti con valori di default.
         *
         * @param id L'ID del documento su Firestore.
         * @param map La mappa contenente i dati della richiesta.
         * @return Un'istanza di [FriendRequest] popolata con i dati della mappa.
         */
        fun fromMap(id: String, map: Map<String, Any>): FriendRequest {
            return FriendRequest(
                id = id,
                fromUserId = map["fromUserId"] as? String ?: "",
                toUserId = map["toUserId"] as? String ?: "",
                status = try {
                    RequestStatus.valueOf(map["status"] as? String ?: "PENDING")
                } catch (_: Exception) {
                    RequestStatus.PENDING
                },
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                fromUsername = map["fromUsername"] as? String ?: "",
                fromAvatar = map["fromAvatar"] as? String ?: "🧙",
                toUsername = map["toUsername"] as? String ?: "",
                toAvatar = map["toAvatar"] as? String ?: "🧙"
            )
        }
    }
}