package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun LibraryScreen(navController: NavController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Biblioteca", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        repeat(5) { index ->
            Text(
                text = "Charla ${index + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("transcription") }
                    .padding(8.dp)
            )
            Divider()
        }
    }
}