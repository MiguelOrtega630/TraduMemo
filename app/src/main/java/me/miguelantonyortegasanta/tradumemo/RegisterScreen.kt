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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {

    val auth = Firebase.auth
    val activity = LocalView.current.context as Activity

    // --- ESTADO DE LOS INPUTS ---
    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var inputPasswordConfirmation by remember { mutableStateOf("") }
    var rememberPassword by remember { mutableStateOf(false) } // De tu UI original

    // --- ESTADO DE LOS ERRORES ---
    var nameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var passwordConfirmationError by remember { mutableStateOf("") }
    var registerError by remember { mutableStateOf("") }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4ECD8)), // Fondo beige
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


            Text(
                text = "TraduMemo",
                color = Color(0xFFFF4436), // Rojo del logo
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Registro",
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

                    // --- CAMPO NOMBRE ---
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nombre") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Nombre")
                        },
                        supportingText = {
                            if (nameError.isNotEmpty()) {
                                Text(text = nameError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        isError = nameError.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // --- CAMPO CONFIRMAR CONTRASEÑA ---
                    OutlinedTextField(
                        value = inputPasswordConfirmation,
                        onValueChange = { inputPasswordConfirmation = it },
                        label = { Text("Confirmar Contraseña") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Confirmar Contraseña")
                        },
                        supportingText = {
                            if (passwordConfirmationError.isNotEmpty()) {
                                Text(text = passwordConfirmationError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        isError = passwordConfirmationError.isNotEmpty(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberPassword,
                            onCheckedChange = { rememberPassword = it }
                        )
                        Text("Guardar Contraseña")
                    }

                    // --- ERROR GENERAL DE REGISTRO ---
                    if (registerError.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = registerError,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // --- BOTÓN REGISTRARSE ---
                    Button(
                        onClick = {
                            // --- LÓGICA DE VALIDACIÓN ---
                            val isValidName = validateName(inputName).first
                            val isValidEmail = validateEmail(inputEmail).first
                            val isValidPassword = validatePassword(inputPassword).first
                            val isValidConfirmPassword =
                                validateConfirmPassword(inputPassword, inputPasswordConfirmation).first

                            nameError = validateName(inputName).second
                            emailError = validateEmail(inputEmail).second
                            passwordError = validatePassword(inputPassword).second
                            passwordConfirmationError =
                                validateConfirmPassword(inputPassword, inputPasswordConfirmation).second

                            // --- LÓGICA DE FIREBASE ---
                            if (isValidName && isValidEmail && isValidPassword && isValidConfirmPassword) {
                                registerError = "" // Limpiar error previo
                                auth.createUserWithEmailAndPassword(inputEmail, inputPassword)
                                    .addOnCompleteListener(activity) { task ->
                                        if (task.isSuccessful) {
                                            // Éxito: Navegar a Login
                                            navController.navigate("login") {
                                                // Limpiar la pila para que no pueda volver a Register
                                                popUpTo(navController.graph.startDestinationId) {
                                                    inclusive = true
                                                }
                                            }
                                        } else {
                                            // Error: Mostrar mensaje
                                            registerError = when (task.exception) {
                                                is FirebaseAuthInvalidCredentialsException -> "El formato del correo es inválido"
                                                is FirebaseAuthUserCollisionException -> "El correo ya está registrado"
                                                else -> "Error al registrarse. Intenta de nuevo."
                                            }
                                        }
                                    }
                            } else {
                                registerError = "Por favor, corrige los errores."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                    ) {
                        Text("Registrarse", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // --- BOTÓN PARA IR A LOGIN ---
            TextButton(onClick = { navController.popBackStack() }) { // popBackStack() vuelve a la pantalla anterior (Login)
                Text(
                    text = "¿Ya tienes cuenta? Inicia Sesión",
                    color = Color(0xFF2C2C2C)
                )
            }
        }
    }
}

// --- Funciones de Validación (Añadidas para completar la lógica) ---

private fun validateName(name: String): Pair<Boolean, String> {
    return if (name.isBlank()) {
        Pair(false, "El nombre no puede estar vacío")
    } else {
        Pair(true, "")
    }
}

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
    } else {
        Pair(true, "")
    }
}

private fun validateConfirmPassword(password: String, confirm: String): Pair<Boolean, String> {
    return if (confirm.isBlank()) {
        Pair(false, "Confirma tu contraseña")
    } else if (password != confirm) {
        Pair(false, "Las contraseñas no coinciden")
    } else {
        Pair(true, "")
    }
}