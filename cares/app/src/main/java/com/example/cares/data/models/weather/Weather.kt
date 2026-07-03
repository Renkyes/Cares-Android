// file: data/models/weather/Weather.kt
package com.example.cares.data.models.weather

/**
 * Modello dati che rappresenta le condizioni meteorologiche.
 *
 * Utilizzato per visualizzare il meteo corrente nella Home e per suggerire
 * attività fisica in base alle condizioni esterne.
 *
 * **Utilizzo tipico:**
 * - Visualizzazione delle condizioni meteo correnti nella UI.
 * - Determinazione di suggerimenti per attività fisica (es. "Giornata ideale per correre!").
 * - Scelta dell'icona appropriata in base alle condizioni.
 *
 * @property city Nome della città.
 * @property temperature Temperatura attuale in gradi Celsius (°C).
 * @property condition Categoria meteo principale (es. "Clear", "Rain", "Clouds").
 *                    Utilizzata per determinare l'emoji e il suggerimento attività.
 * @property description Descrizione estesa delle condizioni meteorologiche
 *                      (es. "cielo sereno", "pioggia leggera").
 * @property iconCode Codice icona di OpenWeather (es. "01d", "02n").
 *                    Può essere utilizzato per caricare l'icona dall'API.
 * @property humidity Umidità percentuale (0-100%).
 *
 * **Icone OpenWeather comuni:**
 * - `01d` / `01n`: cielo sereno
 * - `02d` / `02n`: poche nuvole
 * - `03d` / `03n`: nubi sparse
 * - `04d` / `04n`: nubi spezzate
 * - `09d` / `09n`: pioggia leggera
 * - `10d` / `10n`: pioggia
 * - `11d` / `11n`: temporale
 * - `13d` / `13n`: neve
 * - `50d` / `50n`: nebbia
 */
data class Weather(
    val city: String,
    val temperature: Double,
    val condition: String,
    val description: String,
    val iconCode: String,
    val humidity: Int
)