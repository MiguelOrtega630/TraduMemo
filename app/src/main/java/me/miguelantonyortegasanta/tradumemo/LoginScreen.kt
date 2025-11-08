package me.miguelantonyortegasanta.tradumemo

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.auth
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {

    val auth = Firebase.auth
    val activity = LocalView.current.context as Activity

    // --- ESTADOS DE INPUTS Y ERRORES ---
    var inputEmail by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4ECD8)),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding() // Añadido para que no tape el teclado
                .verticalScroll(rememberScrollState()) // Añadido para scroll
        ) {

            // Título principal
            Text(
                text = "TraduMemo",
                color = Color(0xFFFF4436),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Inicio de sesión",
                color = Color(0xFF2C2C2C),
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))


            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- CAMPO EMAIL ---
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = "Email")
                        },
                        supportingText = {
                            if (emailError.isNotEmpty()) {
                                Text(text = emailError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        isError = emailError.isNotEmpty(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Email,
                            autoCorrect = false,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // --- CAMPO CONTRASEÑA ---
                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Contraseña")
                        },
                        supportingText = {
                            if (passwordError.isNotEmpty()) {
                                Text(text = passwordError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        isError = passwordError.isNotEmpty(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Password,
                            autoCorrect = false,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // --- ERROR GENERAL DE LOGIN ---
                    if (loginError.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = loginError,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // --- BOTÓN INICIAR SESIÓN ---
                    Button(
                        onClick = {
                            // --- LÓGICA DE VALIDACIÓN ---
                            val isValidEmail: Boolean = validateEmail(inputEmail).first
                            val isValidPassword = validatePassword(inputPassword).first
                            emailError = validateEmail(inputEmail).second
                            passwordError = validatePassword(inputPassword).second

                            // --- LÓGICA DE FIREBASE ---
                            if (isValidEmail && isValidPassword) {
                                loginError = "" // Limpiar error previo
                                auth.signInWithEmailAndPassword(inputEmail, inputPassword)
                                    .addOnCompleteListener(activity) { task ->
                                        if (task.isSuccessful) {
                                            // Éxito: Navegar a Main
                                            navController.navigate("main") {
                                                // Limpiar toda la pila de navegación
                                                popUpTo(navController.graph.startDestinationId) {
                                                    inclusive = true
                                                }
                                            }
                                        } else {
                                            // Error: Mostrar mensaje
                                            loginError = when (task.exception) {
                                                is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrecta"
                                                is FirebaseAuthInvalidUserException -> "No existe una cuenta con este correo"
                                                else -> "Error al iniciar sesión. Intenta de nuevo"
                                            }
                                        }
                                    }
                            } else {
                                loginError = "Por favor, corrige los errores."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                    ) {
                        Text("Iniciar Sesión", color = Color.White)
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(onClick = { /* Lógica para recuperar contraseña */ }) {
                        Text("¿Se te olvidó la contraseña?", color = Color(0xFF2C2C2C))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // --- BOTÓN PARA IR A REGISTRO ---
            TextButton(onClick = { navController.navigate("register") }) {
                Text(
                    text = "¿No tienes cuenta? Regístrate",
                    color = Color(0xFF2C2C2C)
                )
            }
        }
    }
}


// --- Funciones de Validación (Necesarias para la lógica) ---

private fun validateEmail(email: String): Pair<Boolean, String> {
    val emailRegex = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()
    return if (email.isBlank()) {
        Pair(false, "El correo no puede estar vacío")
    } else if (!email.matches(emailRegex)) {
        Pair(false, "Formato de correo inválido")
    } else {
        Pair(true, "")
    }
}

private fun validatePassword(password: String): Pair<Boolean, String> {
    return if (password.length < 6) {
        Pair(false, "La contraseña debe tener al menos 6 caracteres")
    } else if (password.isBlank()) {
        Pair(false, "La contraseña no puede estar vacía")
    }
    else {
        Pair(true, "")
    }
}