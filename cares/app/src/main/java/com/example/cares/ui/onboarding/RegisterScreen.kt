// file: ui/onboarding/RegisterScreen.kt
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
import com.example.cares.ui.theme.Neon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ================================ REGISTER SCREEN ================================

/**
 * Schermata di registrazione per i nuovi utenti.
 *
 * **Scopo:**
 * Permette ai nuovi utenti di creare un account con email e password.
 * Dopo la registrazione, l'utente viene reindirizzato alla personalizzazione del personaggio.
 *
 * **Caratteristiche:**
 * - Campi email, password e conferma password con validazione.
 * - Validazione delle password (minimo 6 caratteri, corrispondenza).
 * - Gestione degli errori con messaggi specifici.
 * - Indicatore di caricamento durante la registrazione.
 * - Supporto per la tastiera (imePadding, keyboardActions).
 * - Sfondo con immagine e overlay scuro.
 * - Link per tornare alla schermata di login.
 *
 * **Flusso di utilizzo:**
 * 1. L'utente inserisce email, password e conferma password.
 * 2. Premere "Registrati" o invio sulla tastiera.
 * 3. FirebaseAuthManager tenta la creazione dell'account.
 * 4. In caso di successo, procede alla personalizzazione del personaggio.
 * 5. In caso di errore, mostra un messaggio specifico.
 *
 * @param onRegisterSuccess Callback eseguito dopo la registrazione riuscita.
 * @param onBackToLogin Callback per tornare alla schermata di login.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // ================================ FUNZIONE REGISTRAZIONE ================================

    /**
     * Esegue la registrazione utilizzando FirebaseAuthManager.
     *
     * **Validazioni:**
     * - Email non vuota e valida (formato).
     * - Password non vuota e almeno 6 caratteri.
     * - Password e conferma password corrispondono.
     *
     * **Gestione errori:**
     * - Email già registrata.
     * - Email non valida.
     * - Password troppo debole.
     * - Errore di connessione.
     */
    fun performRegistration() {
        // ---- Validazioni ----
        when {
            email.isBlank() -> {
                errorMessage = "Inserisci un'email"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                errorMessage = "Inserisci un'email valida"
                return
            }
            password.isBlank() -> {
                errorMessage = "Inserisci una password"
                return
            }
            password.length < 6 -> {
                errorMessage = "La password deve avere almeno 6 caratteri"
                return
            }
            password != confirmPassword -> {
                errorMessage = "Le password non coincidono"
                return
            }
        }

        // ---- Tentativo di registrazione ----
        isLoading = true
        errorMessage = null

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = FirebaseAuthManager.createUserWithEmailAndPassword(email, password)

                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (result.isSuccess) {
                        Toast.makeText(context, "✅ Registrazione completata!", Toast.LENGTH_SHORT).show()
                        email = ""
                        password = ""
                        confirmPassword = ""
                        onRegisterSuccess()
                    } else {
                        val exception = result.exceptionOrNull()
                        errorMessage = when {
                            // Email già registrata
                            exception?.message?.contains("email address is already in use") == true ||
                                    exception?.message?.contains("EMAIL_EXISTS") == true ->
                                "❌ Questa email è già registrata"
                            // Email non valida
                            exception?.message?.contains("invalid email") == true ||
                                    exception?.message?.contains("INVALID_EMAIL") == true ->
                                "❌ Email non valida"
                            // Password troppo debole
                            exception?.message?.contains("weak password") == true ||
                                    exception?.message?.contains("WEAK_PASSWORD") == true ->
                                "❌ Password troppo debole"
                            // Altri errori
                            else ->
                                "❌ Errore durante la registrazione: ${exception?.message ?: "Riprova"}"
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
                contentDescription = "Sfondo Registrazione",
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
                    text = "Crea il tuo account",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Unisciti alla community di Cares!",
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
                            ),
                            supportingText = {
                                Text(
                                    text = "Minimo 6 caratteri",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ---- Campo Conferma Password ----
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            label = { Text("Conferma Password") },
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
                                onGo = { performRegistration() }
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

                        // ---- Pulsante Registrati ----
                        Button(
                            onClick = { performRegistration() },
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
                                    text = "Registrati",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Link per tornare al Login ----
                TextButton(
                    onClick = onBackToLogin,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "← Torna al Login",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}