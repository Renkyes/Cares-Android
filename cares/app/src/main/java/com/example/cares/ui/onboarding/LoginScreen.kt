// file: ui/onboarding/LoginScreen.kt
package com.example.cares.ui.onboarding

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cares.R
import com.example.cares.data.manager.FirebaseAuthManager
import com.example.cares.data.repository.CaresRepository
import com.example.cares.ui.theme.Neon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ================================ LOGIN SCREEN ================================

/**
 * Schermata di login per gli utenti registrati.
 *
 * **Scopo:**
 * Permette agli utenti esistenti di accedere con email e password.
 * Dopo il login, i dati dell'utente vengono caricati da Firestore.
 *
 * **Caratteristiche:**
 * - Campi email e password con validazione.
 * - Gestione degli errori con messaggi specifici.
 * - Indicatore di caricamento durante l'autenticazione.
 * - Supporto per la tastiera (imePadding, keyboardActions).
 * - Sfondo con immagine e overlay scuro.
 * - Link per passare alla schermata di registrazione.
 *
 * **Flusso di utilizzo:**
 * 1. L'utente inserisce email e password.
 * 2. Premere "Accedi" o invio sulla tastiera.
 * 3. FirebaseAuthManager tenta l'autenticazione.
 * 4. In caso di successo, carica i dati dell'utente e procede.
 * 5. In caso di errore, mostra un messaggio specifico.
 *
 * @param onLoginClicked Callback eseguito dopo il login riuscito.
 * @param onRegisterClicked Callback per passare alla schermata di registrazione.
 */
@Composable
fun LoginScreen(
    onLoginClicked: () -> Unit,
    onRegisterClicked: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val repository = remember(context) { CaresRepository(context) }

    // ================================ FUNZIONE LOGIN ================================

    /**
     * Esegue il login utilizzando FirebaseAuthManager.
     *
     * **Validazioni:**
     * - Email e password non vuote.
     * - Email valida (formato).
     * - Password almeno 6 caratteri.
     *
     * **Gestione errori:**
     * - Credenziali errate.
     * - Troppi tentativi.
     * - Account disabilitato.
     * - Errore di connessione.
     */
    fun performLogin(repository: CaresRepository) {
        // ---- Validazioni ----
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Inserisci email e password"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Inserisci un'email valida"
            return
        }
        if (password.length < 6) {
            errorMessage = "La password deve avere almeno 6 caratteri"
            return
        }

        // ---- Tentativo di login ----
        isLoading = true
        errorMessage = null

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = FirebaseAuthManager.signInWithEmailAndPassword(email, password)

                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (result.isSuccess) {
                        val userId = FirebaseAuthManager.getCurrentUserId()
                        if (userId != null) {
                            repository.loadUserDataFromFirestore(userId)
                        }
                        Toast.makeText(context, "✅ Login effettuato!", Toast.LENGTH_SHORT).show()
                        email = ""
                        password = ""
                        onLoginClicked()
                    } else {
                        val exception = result.exceptionOrNull()
                        errorMessage = when {
                            // Credenziali errate
                            exception?.message?.contains("no user record") == true ||
                                    exception?.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ||
                                    exception?.message?.contains("invalid-login-credentials") == true ->
                                "❌ Email o password errati"
                            // Troppi tentativi
                            exception?.message?.contains("too many requests") == true ->
                                "⏳ Troppi tentativi. Riprova più tardi"
                            // Account disabilitato
                            exception?.message?.contains("user-disabled") == true ->
                                "🚫 Account disabilitato"
                            // Altri errori
                            else ->
                                "❌ Errore: ${exception?.message ?: "Riprova più tardi"}"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "📡 Errore di connessione. Verifica la rete"
                    e.printStackTrace()
                }
            }
        }
    }

    // ================================ UI ================================

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // ---- Immagine di sfondo ----
            Image(
                painter = painterResource(id = R.drawable.ic_welcome_screen),
                contentDescription = "Sfondo Login",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // ---- Overlay scuro ----
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            // ---- Contenuto con imePadding ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .statusBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ---- Titolo ----
                Text(
                    text = "Ben tornato!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Accedi per continuare la tua avventura",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // ---- Card con sfondo solido ----
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A2E) // Blu scuro solido
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ---- Campo Email ----
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Neon.Green,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = Neon.Green,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            enabled = !isLoading,
                            isError = errorMessage != null,
                            singleLine = true,
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ---- Campo Password ----
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Go
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Neon.Green,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = Neon.Green,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            enabled = !isLoading,
                            isError = errorMessage != null,
                            singleLine = true,
                            keyboardActions = KeyboardActions(
                                onGo = { performLogin(repository) }
                            )
                        )

                        // ---- Messaggio di errore ----
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ---- Pulsante Accedi ----
                        Button(
                            onClick = { performLogin(repository) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Neon.Green,
                                contentColor = Color.Black,
                                disabledContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Accedi",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Link registrazione ----
                TextButton(
                    onClick = onRegisterClicked,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Non hai un account? 📝 Registrati",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}