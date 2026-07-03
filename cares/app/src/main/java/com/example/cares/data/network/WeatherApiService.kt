// file: data/network/WeatherApiService.kt
package com.example.cares.data.network

import com.example.cares.data.models.weather.WeatherResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interfaccia del servizio API per il meteo, utilizzata da Retrofit per chiamare
 * gli endpoint di OpenWeatherMap.
 *
 * Fornisce due metodi principali per ottenere dati meteo:
 * - [getWeather]: recupero basato su coordinate geografiche (latitudine/longitudine).
 * - [getWeatherByCity]: recupero basato sul nome di una città.
 *
 * **Configurazione:**
 * - Unitá di misura: "metric" (temperatura in °C, velocità del vento in m/s).
 * - La base URL è configurata in [RetrofitClient].
 * - La conversione JSON è gestita da Gson.
 *
 * **API Key:**
 * Deve essere configurata in [RetrofitClient.API_KEY].
 * Per ottenere una chiave, registrarsi su https://openweathermap.org/api.
 *
 * @see RetrofitClient per la configurazione di Retrofit.
 * @see WeatherResponse per la struttura della risposta.
 */
interface WeatherApiService {

    /**
     * Recupera il meteo corrente in base alle coordinate geografiche.
     *
     * Questo metodo è utile quando si dispone della posizione precisa
     * dell'utente (es. tramite GPS) e si vuole ottenere il meteo
     * per quella specifica posizione.
     *
     * @param lat Latitudine (es. 41.9028 per Roma, 45.4642 per Milano).
     * @param lon Longitudine (es. 12.4964 per Roma, 9.1900 per Milano).
     * @param apiKey La chiave API di OpenWeatherMap.
     * @param units Unità di misura (default "metric" per Celsius e m/s).
     *              Valori possibili: "metric", "imperial", "standard".
     * @return [WeatherResponse] contenente i dati meteo correnti.
     */
    @GET("weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    /**
     * Recupera il meteo corrente per una città specifica.
     *
     * Questo metodo è utile quando si conosce il nome della città
     * ma non si hanno le coordinate precise. Supporta anche codici
     * paese per disambiguare città con lo stesso nome (es. "London,UK").
     *
     * @param city Nome della città (es. "Roma", "London", "New York").
     *             Può includere il codice paese (es. "London,UK").
     * @param apiKey La chiave API di OpenWeatherMap.
     * @param units Unità di misura (default "metric" per Celsius e m/s).
     *              Valori possibili: "metric", "imperial", "standard".
     * @return [WeatherResponse] contenente i dati meteo correnti.
     */
    @GET("weather")
    suspend fun getWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

// ================================ RETROFIT CLIENT ================================

/**
 * Oggetto singleton che fornisce l'istanza di [WeatherApiService]
 * per effettuare chiamate al servizio meteo OpenWeatherMap.
 *
 * **Configurazione:**
 * - Base URL: `https://api.openweathermap.org/data/2.5/`
 * - Convertitore JSON: Gson tramite [GsonConverterFactory]
 * - Timeout: default di Retrofit (30 secondi)
 *
 * **Thread-safety:**
 * L'istanza è lazy e thread-safe, creata al primo utilizzo e
 * condivisa tra tutte le chiamate per ridurre l'overhead.
 *
 * **Importante:**
 * La proprietà [API_KEY] deve essere sostituita con una chiave valida
 * ottenuta da https://openweathermap.org/api.
 * In produzione, si raccomanda di non hardcodare la chiave ma di
 * utilizzare un sistema di configurazione sicuro (es. BuildConfig, secrets.properties).
 *
 * @see WeatherApiService per i metodi disponibili.
 */
object RetrofitClient {

    /**
     * URL base dell'API OpenWeatherMap.
     */
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    /**
     * Chiave API di OpenWeatherMap.
     *
     * **ATTENZIONE:** Questa chiave è pubblica. In produzione, si consiglia di:
     * - Utilizzare un secrets.properties per memorizzare le credenziali.
     * - Oppure configurare la chiave tramite BuildConfig.
     * - Non esporre la chiave in repository pubblici.
     *
     * @see <a href="https://openweathermap.org/api">OpenWeatherMap API</a>
     */
    const val API_KEY = "07c4dae817e2191b7b2542e6a844a2db"

    /**
     * Istanza lazy di [WeatherApiService], creata al primo utilizzo.
     *
     * L'istanza è thread-safe e condivisa tra tutte le chiamate per ridurre
     * l'overhead di creazione di Retrofit multiplo.
     *
     * **Costruzione:**
     * 1. Configura la base URL dell'API.
     * 2. Aggiunge il convertitore Gson per la deserializzazione JSON.
     * 3. Crea l'istanza del servizio Retrofit.
     *
     * @return Istanza configurata di [WeatherApiService].
     */
    val instance: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}