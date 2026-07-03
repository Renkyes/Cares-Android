// file: data/repository/WeatherRepository.kt
package com.example.cares.data.repository

import android.content.Context
import com.example.cares.data.network.RetrofitClient
import com.example.cares.data.models.weather.Weather

/**
 * Repository per il recupero dei dati meteorologici da OpenWeatherMap.
 *
 * **Responsabilità:**
 * - Effettuare chiamate API per ottenere il meteo corrente.
 * - Gestire il caching in memoria per ridurre le chiamate di rete.
 * - Fornire un meccanismo di fallback in caso di errori di rete o API.
 *
 * **Strategia di caching:**
 * - I dati vengono mantenuti in memoria per 30 minuti ([CACHE_DURATION]).
 * - La cache viene invalidata automaticamente dopo la scadenza temporale.
 * - In caso di errore, viene restituito l'ultimo dato in cache (anche se scaduto)
 *   oppure un oggetto [Weather] fittizio tramite [getFallbackWeather].
 *
 * **Gestione dei parametri di ricerca:**
 * - `city`: priorità massima, ricerca per nome città.
 * - `lat` + `lon`: ricerca per coordinate geografiche.
 * - Nessun parametro: default "Roma".
 *
 * **Fallback:**
 * - Se la chiamata API fallisce e non c'è cache, viene restituito un oggetto
 *   [Weather] con dati fittizi (Casa tua, 22°C, cielo sereno) per garantire
 *   il funzionamento dell'app anche in assenza di connessione.
 *
 * @param context Il contesto per eventuali future estensioni (es. localizzazione).
 */
class WeatherRepository(private val context: Context) {

    // ================================ PROPRIETÀ ================================

    /** Servizio API per le chiamate a OpenWeatherMap. */
    private val apiService = RetrofitClient.instance

    /** Chiave API per l'autenticazione a OpenWeatherMap. */
    private val apiKey = RetrofitClient.API_KEY

    /**
     * Ultimo meteo scaricato, conservato in memoria per il caching.
     * Può essere `null` se non è mai stata effettuata una chiamata di successo.
     */
    private var cachedWeather: Weather? = null

    /**
     * Timestamp dell'ultimo download del meteo (System.currentTimeMillis()).
     * Utilizzato per determinare se la cache è ancora valida.
     */
    private var lastFetchTime: Long = 0L

    /** Durata della cache: 30 minuti (in millisecondi). */
    private val CACHE_DURATION = 30 * 60 * 1000L

    // ================================ API PUBBLICHE ================================

    /**
     * Recupera il meteo corrente, utilizzando la cache se disponibile e fresca.
     *
     * **Flusso di esecuzione:**
     * 1. Controlla se esiste una cache valida (scaricata meno di [CACHE_DURATION] fa).
     * 2. Se sì, restituisce i dati in cache.
     * 3. Se no, effettua una chiamata API utilizzando i parametri forniti.
     * 4. In caso di successo, salva il risultato in cache e lo restituisce.
     * 5. In caso di errore, restituisce l'ultima cache disponibile (anche se scaduta)
     *    o un oggetto [Weather] fittizio tramite fallback.
     *
     * **Priorità dei parametri:**
     * - Se `city` è fornito, viene usato per la ricerca (priorità massima).
     * - Se `lat` e `lon` sono forniti, vengono usati per la ricerca.
     * - Se nessun parametro è fornito, viene usata la città di default "Roma".
     *
     * @param lat Latitudine (opzionale).
     * @param lon Longitudine (opzionale).
     * @param city Nome della città (opzionale, priorità su lat/lon).
     * @return Un oggetto [Weather] se disponibile, `null` in caso di errore persistente.
     */
    suspend fun getWeather(
        lat: Double? = null,
        lon: Double? = null,
        city: String? = null
    ): Weather? {
        // 1. Controllo cache valida
        if (cachedWeather != null && System.currentTimeMillis() - lastFetchTime < CACHE_DURATION) {
            return cachedWeather
        }

        // 2. Chiamata API
        return try {
            val response = if (city != null) {
                // Priorità alla città
                apiService.getWeatherByCity(city, apiKey)
            } else if (lat != null && lon != null) {
                // Coordinate geografiche
                apiService.getWeather(lat, lon, apiKey)
            } else {
                // Default: Roma
                apiService.getWeatherByCity("Roma", apiKey)
            }

            // 3. Mappatura della risposta al modello di dominio
            val weather = Weather(
                city = response.name,
                temperature = response.main.temp,
                condition = response.weather.firstOrNull()?.main ?: "Unknown",
                description = response.weather.firstOrNull()?.description ?: "",
                iconCode = response.weather.firstOrNull()?.icon ?: "",
                humidity = response.main.humidity
            )

            // 4. Aggiornamento cache
            cachedWeather = weather
            lastFetchTime = System.currentTimeMillis()
            weather

        } catch (e: Exception) {
            // 5. Gestione errori: log e fallback
            e.printStackTrace()

            // Se esiste una cache (anche scaduta), la restituisce
            cachedWeather ?: getFallbackWeather()
        }
    }

    // ================================ FUNZIONI PRIVATE ================================

    /**
     * Fornisce un oggetto [Weather] di fallback per garantire il funzionamento dell'app
     * anche in assenza di connessione o in caso di errori API.
     *
     * **Criteri per i valori di fallback:**
     * - Città: "Casa tua" (generico, non associato a una località specifica)
     * - Temperatura: 22°C (temperatura ambiente piacevole)
     * - Condizioni: "Clear" (cielo sereno)
     * - Descrizione: "bel tempo" (non tecnica, amichevole)
     * - Umidità: 50% (valore medio)
     *
     * @return Un oggetto [Weather] con dati fittizi realistici.
     */
    private fun getFallbackWeather(): Weather {
        return Weather(
            city = "Casa tua",
            temperature = 22.0,
            condition = "Clear",
            description = "bel tempo",
            iconCode = "01d",
            humidity = 50
        )
    }
}