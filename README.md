# Cares - App per il Benessere e la Gamification

## 📖 Descrizione del Progetto

**Cares** è un'applicazione Android innovativa che combina il monitoraggio del benessere personale con elementi di gamification per motivare gli utenti a mantenere uno stile di vita sano. L'app trasforma le attività quotidiane in "missioni" che l'utente può completare per guadagnare esperienza, sbloccare badge e avanzare in una storia interattiva personalizzata.

### 🎯 Obiettivi Principali

- **Motivare** gli utenti a mantenere abitudini sane attraverso la gamification
- **Tracciare** i progressi personali con statistiche dettagliate
- **Coinvolgere** gli utenti con una storia interattiva che si sblocca progressivamente
- **Creare** una community attraverso funzionalità social (amici, classifica)
- **Personalizzare** l'esperienza con avatar e temi

## ✨ Funzionalità Principali

### 🏠 Home
- Dashboard personalizzata con statistiche utente (XP, livello, streak)
- Missione giornaliera principale con timer e tracking passi
- Obiettivo giornaliero personalizzato (step o durata)
- Meteo con suggerimenti attività
- Diario delle emozioni
- Ricompensa settimanale (7 giorni consecutivi)

### 📋 Missioni
- **Missioni Giornaliere**: 7 missioni generate in base al profilo utente
- **Missioni Settimanali**: 5 missioni più complesse con ricompense speciali
- **Missioni Mensili**: 3 missioni ad alta difficoltà con ricompense speciali
- **Tipi di Verifica**:
  - Foto 📸 (scatta una foto)
  - Testo ✏️ (inserisci un testo)
  - Riconoscimento Oggetti 🔍 (ML Kit)
- **Missioni a Step**: Progresso graduale con controllo temporale
- **Ricompense Speciali**:
  - Scudo Streak 🛡️
  - Boost XP ⚡
  - Frammenti di Storia 📖

### 👥 Social
- **Amici**: Aggiungi e gestisci amici
- **Classifica**: Confronta i progressi con amici e utenti globali
- **Richieste di Amicizia**: Gestisci richieste in entrata e uscita
- **Profili Pubblici**: Visualizza i profili di altri utenti

### 🏅 Gamification
- **Sistema XP e Livelli**: Guadagna XP completando missioni
- **Streak**: Mantieni una serie di giorni consecutivi
- **Badge**: Sblocca badge per traguardi raggiunti
- **Storia Interattiva**: 8 capitoli per avatar che si sbloccano con i progressi
- **Inventario**: 
  - Scudi Streak (salvano la streak)
  - Boost XP (moltiplicano l'XP)
  - Frammenti di Storia (sbloccano capitoli)

### 🎨 Personalizzazione
- **Avatar**: 3 personaggi (Mago, Eroe, Elfo)
- **Temi Colore**: 7 temi disponibili (Verde, Blu, Viola, Arancione, Rosa, Rosso, Teal)
- **Tema Scuro/Chiaro**: Supporto completo
- **Musica di Sottofondo**: Attivabile/disattivabile

### 📊 Monitoraggio
- **Contatore Passi**: Tracking automatico con persistenza
- **Diario delle Emozioni**: Tracciamento dell'umore giornaliero
- **Statistiche**: XP, livello, streak, missioni completate
- **Meteo**: Condizioni meteo con suggerimenti attività

## 🏗️ Architettura

### Pattern Utilizzati
- **MVVM** (Model-View-ViewModel) per la separazione delle responsabilità
- **Repository Pattern** per l'accesso ai dati
- **Dependency Injection** manuale tramite ViewModel Factory
- **StateFlow** per la gestione reattiva dello stato

### Tecnologie e Librerie

| Tecnologia | Utilizzo |
|------------|----------|
| **Kotlin** | Linguaggio principale |
| **Jetpack Compose** | UI dichiarativa |
| **Firebase** | Autenticazione e Firestore |
| **DataStore** | Persistenza locale |
| **WorkManager** | Schedulazione notifiche |
| **ML Kit** | Riconoscimento oggetti e testo |
| **Coil** | Caricamento immagini |
| **Accompanist** | Permessi e gestione stato |
| **Google Play Services** | Localizzazione |

## 📁 Struttura del Progetto

```
app/src/main/java/com/example/cares/
├── data/
│   ├── manager/          # Gestori di funzionalità
│   │   ├── FirebaseAuthManager.kt
│   │   ├── MusicPlayerManager.kt
│   │   ├── NotificationHelper.kt
│   │   ├── ReminderScheduler.kt
│   │   ├── StepCounterManager.kt
│   │   ├── StoryManager.kt
│   │   └── UserPreferencesManager.kt
│   ├── models/           # Data classes
│   │   ├── FriendRequest.kt
│   │   ├── InventoryItem.kt
│   │   ├── LeaderboardEntry.kt
│   │   ├── PublicUserProfile.kt
│   │   └── weather/
│   │       ├── Weather.kt
│   │       └── WeatherResponse.kt
│   ├── network/          # API e networking
│   │   └── WeatherApiService.kt
│   └── repository/       # Repository
│       ├── CaresRepository.kt
│       └── WeatherRepository.kt
├── ui/
│   ├── animation/        # Animazioni e transizioni
│   ├── celebration/      # Screens di celebrazione
│   ├── debug/            # Strumenti di debug
│   ├── friends/          # Gestione amici
│   ├── home/             # Schermata principale
│   ├── leaderboard/      # Classifica
│   ├── main/             # Activity principale
│   ├── onboarding/       # Flusso di onboarding
│   ├── profile/          # Profilo utente
│   ├── quests/           # Missioni
│   ├── settings/         # Impostazioni
│   ├── splash/           # Splash e loading
│   ├── story/            # Storia interattiva
│   └── theme/            # Tema e stili
├── utils/                # Utility e helper
│   ├── BadgeSystem.kt
│   ├── DailyQuestManager.kt
│   └── PermissionManager.kt
└── viewmodel/            # ViewModels
    ├── LeaderboardViewModel.kt
    └── PublicProfileViewModel.kt
```

## 🚀 Guida all'Installazione

### Prerequisiti
- **Android Studio** (versione Hedgehog o superiore)
- **JDK 17** o superiore
- **Android SDK** (API 24+)
- **Firebase Account** (gratuita)

### Passaggi

1. **Clona il repository**
```bash
git clone https://github.com/tuoprogetto/cares.git
cd cares
```

2. **Configura Firebase**
   - Crea un progetto su [Firebase Console](https://console.firebase.google.com/)
   - Aggiungi il tuo progetto Android
   - Scarica il file `google-services.json` e mettilo nella cartella `app/`
   - Abilita **Email/Password Authentication**
   - Crea un database **Firestore** in modalità test

3. **Configura le chiavi API**
   - Ottieni una chiave API da [OpenWeatherMap](https://openweathermap.org/api)
   - Sostituisci `API_KEY` in `WeatherApiService.kt`

4. **Build e Run**
```bash
./gradlew build
./gradlew installDebug
```

## ⚙️ Configurazione

### File di Configurazione Principali

| File | Descrizione |
|------|-------------|
| `build.gradle.kts` | Dipendenze e configurazione build |
| `AndroidManifest.xml` | Permessi e componenti |
| `google-services.json` | Configurazione Firebase |
| `ic_*.xml` | Icone e risorse grafiche |
| `strings.xml` | Stringhe localizzate |

### Permessi Richiesti

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
```

## 🔄 Flussi dell'Applicazione

### Flusso di Onboarding
1. **Splash Screen** → 2. **Loading Screen** → 3. **Welcome Screen** → 4. **Login/Register** → 5. **Character Customization** → 6. **Goal Selection** → 7. **Preferences** → 8. **Home**

### Flusso Principale
1. **Login** → **Home**
2. Completa missioni → Guadagna XP e badge
3. Sblocca capitoli della storia
4. Aggiungi amici → Confronta progressi in classifica
5. Personalizza tema e impostazioni

## 🗄️ Data Persistence

### Strategie di Persistenza

| Dato | Modalità |
|------|----------|
| Preferenze Utente | DataStore |
| Missioni Generate | DataStore (cache) |
| Profilo Utente | Firestore |
| Amici e Richieste | Firestore |
| Statistiche di Gioco | DataStore + Firestore |
| Meteo | Cache in memoria (30 min) |
| Tracciamento Passi | DataStore + Sensori |

## 🎨 Personalizzazione del Tema

### Temi Disponibili
- **Verde** (default) - `GreenTheme`
- **Blu** - `BlueTheme`
- **Viola** - `PurpleTheme`
- **Arancione** - `OrangeTheme`
- **Rosa** - `PinkTheme`
- **Rosso** - `RedTheme`
- **Teal** - `TealTheme`

### Utilizzo
```kotlin
// Per cambiare tema
CaresTheme(darkTheme = isDark, theme = AppTheme.PURPLE) {
    // Contenuto
}
```

## 🧪 Testing

### Test Manuali
- **Debug Screen**: Accessibile dalle Impostazioni
  - Reset Streak, Badge, Missioni
  - Simula Avanzamento Giorno
  - Sincronizza Profilo
  - Aggiungi Oggetti Inventario

### Test da Eseguire
1. **Onboarding**: Completamento flusso
2. **Missioni**: Completamento e verifica
3. **Streak**: Mantenimento e salvataggio
4. **Badge**: Sblocco e visualizzazione
5. **Amici**: Aggiunta e accettazione
6. **Classifica**: Caricamento e ordinamento
7. **Inventario**: Utilizzo oggetti

## 🔒 Privacy e Sicurezza

### Dati Personali
- **Email e Password**: Autenticazione Firebase (criptati)
- **Dati Utente**: Memorizzati in Firestore (accesso solo all'utente)
- **Posizione**: Usata solo per meteo, non persistente

### Permessi
- Richiesti in modo contestuale (al momento dell'uso)
- Spiegazione della necessità di ogni permesso

## 🤝 Contributi

### Come Contribuire
1. **Fork** il repository
2. Crea un **branch** per la tua feature
3. **Commit** delle modifiche
4. **Push** sul tuo fork
5. Apri una **Pull Request**

### Linee Guida
- Segui lo stile di codice Kotlin
- Aggiungi commenti per funzioni complesse
- Aggiorna la documentazione
- Testa le modifiche

## 📝 Changelog

### Versione 1.0.0
- ✅ Onboarding completo
- ✅ Sistema missioni (giornaliere/settimanali/mensili)
- ✅ Gamification (XP, livello, streak, badge)
- ✅ Storia interattiva con 8 capitoli per avatar
- ✅ Sistema amici e classifica
- ✅ Inventario con oggetti speciali
- ✅ Tracciamento passi
- ✅ Diario delle emozioni
- ✅ Temi colore e tema scuro
- ✅ Notifiche e reminder
- ✅ Musica di sottofondo

## 📧 Contatti

**Autore**: [Nome Autore]
**Email**: [email@example.com]
**Repository**: [https://github.com/tuoprogetto/cares](https://github.com/tuoprogetto/cares)

## 📄 Licenza

Questo progetto è distribuito sotto la licenza **MIT**.
Vedi il file `LICENSE` per maggiori dettagli.

---

## 🙏 Ringraziamenti

- **Firebase** per autenticazione e database
- **OpenWeatherMap** per i dati meteo
- **Google ML Kit** per il riconoscimento oggetti
- **Jetpack Compose** per l'UI moderna
- **Tutti i contributori** del progetto

---

*"Un piccolo passo oggi, un grande salto domani."* 🚀
