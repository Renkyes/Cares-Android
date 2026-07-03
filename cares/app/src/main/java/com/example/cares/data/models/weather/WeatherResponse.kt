// file: data/models/weather/WeatherResponse.kt
package com.example.cares.data.models.weather

/**
 * Risposta dell'API OpenWeatherMap per la richiesta meteo corrente.
 *
 * Questa data class rappresenta la struttura JSON restituita dall'endpoint
 * "weather" di OpenWeatherMap. Viene utilizzata per deserializzare la risposta
 * tramite Retrofit e Gson, per poi essere convertita nel modello di dominio
 * semplificato [Weather].
 *
 * **Struttura JSON tipica:**
 * ```
 * {
 *   "weather": [{"id": 800, "main": "Clear", "description": "cielo sereno", "icon": "01d"}],
 *   "main": {"temp": 25.5, "feels_like": 24.0, "humidity": 65},
 *   "name": "Roma"
 * }
 * ```
 *
 * @property weather Lista dei dettagli meteorologici (di solito un solo elemento).
 * @property main Dati principali di temperatura e umidità.
 * @property name Nome della città.
 *
 * @see WeatherItem
 * @see Main
 * @see Weather
 */
data class WeatherResponse(
    val weather: List<WeatherItem>,
    val main: Main,
    val name: String
)

/**
 * Dettaglio meteorologico individuale restituito dall'API.
 *
 * Rappresenta le condizioni meteo specifiche, inclusa la descrizione
 * testuale e il codice icona per la visualizzazione.
 *
 * **ID condizioni meteo comuni:**
 * - 800: cielo sereno
 * - 801: poche nuvole
 * - 802: nubi sparse
 * - 803: nubi spezzate
 * - 804: cielo coperto
 * - 500: pioggia leggera
 * - 501: pioggia moderata
 * - 502: pioggia forte
 * - 200: temporale con pioggia
 * - 600: neve leggera
 * - 601: neve
 * - 701: nebbia
 *
 * @property id Identificatore numerico della condizione meteo.
 * @property main Categoria principale della condizione meteo (es. "Clear", "Rain", "Clouds").
 *                Viene mappato al campo [Weather.condition].
 * @property description Descrizione testuale della condizione meteo (es. "cielo sereno").
 *                Viene mappato al campo [Weather.description].
 * @property icon Codice icona di OpenWeather (es. "01d", "02n").
 *                Viene mappato al campo [Weather.iconCode].
 *
 * **Formato icona:**
 * - `XXd`: icona per il giorno (day)
 * - `XXn`: icona per la notte (night)
 * - Dove `XX` è il codice numerico del tipo di meteo
 */
data class WeatherItem(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

/**
 * Dati principali di temperatura e umidità restituiti dall'API.
 *
 * Contiene le misurazioni fondamentali per la visualizzazione meteo.
 * Tutti i valori di temperatura sono in gradi Celsius quando la richiesta
 * specifica l'unità "metric" (come nel nostro caso).
 *
 * **Relazione tra i campi:**
 * - La temperatura percepita (`feelsLike`) può differire dalla temperatura
 *   effettiva (`temp`) a causa di fattori come vento, umidità e radiazione solare.
 * - In giornate molto umide o ventose, la differenza può essere significativa.
 *
 * @property temp Temperatura attuale in gradi Celsius (°C).
 *                Viene mappato al campo [Weather.temperature].
 * @property feelsLike Temperatura percepita in gradi Celsius (°C).
 *                     Può differire dalla temperatura effettiva a causa di vento e umidità.
 * @property humidity Umidità percentuale (0-100%).
 *                    Viene mappato al campo [Weather.humidity].
 *
 * @see Weather
 */
data class Main(
    val temp: Double,
    val feelsLike: Double,
    val humidity: Int
)