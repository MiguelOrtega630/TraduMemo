package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun TranscriptionScreen(navController: NavController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Como descubrir hacer tierra", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Aquí iría el texto transcrito. Esta pantalla solo muestra el diseño visual, " +
                    "sin funciones reales."
        )
    }
}