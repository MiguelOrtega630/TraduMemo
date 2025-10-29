package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun LoginScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("TraduMemo", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Email") })
        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Contraseña") })
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.navigate("main") }, modifier = Modifier.fillMaxWidth()) {
            Text("Iniciar sesión")
        }
        TextButton(onClick = { navController.navigate("register") }) {
            Text("¿No tienes cuenta? Regístrate", color = MaterialTheme.colorScheme.primary)
        }
    }
}