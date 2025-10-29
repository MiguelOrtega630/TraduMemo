package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MainScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Transcripciones recientes", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.navigate("library") }, modifier = Modifier.fillMaxWidth()) {
            Text("Ir a Biblioteca")
        }
        Button(onClick = { navController.navigate("transcription") }, modifier = Modifier.fillMaxWidth()) {
            Text("Abrir Transcripción")
        }
        Button(onClick = { navController.navigate("chat") }, modifier = Modifier.fillMaxWidth()) {
            Text("Chat con IA")
        }
    }
}