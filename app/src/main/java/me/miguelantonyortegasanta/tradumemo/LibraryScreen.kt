package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun LibraryScreen(navController: NavController) {
    val charlas = List(12) { index -> "Charla ${index + 1}" }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF4ECD8),
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFFF4ECD8)
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {  }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8EDE0))
        ) {
            // ---- Título ----
            Text(
                text = "Biblioteca",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 12.dp),
                color = Color(0xFFFF4436),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )


            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(charlas) { charla ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF6E8DC), shape = MaterialTheme.shapes.medium)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8DEF8),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("A", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = charla,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            fontSize = 16.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.EditNote, contentDescription = "Editar")
                            Icon(Icons.Default.BugReport, contentDescription = "IA")
                            Icon(Icons.Default.Translate, contentDescription = "Traducir")
                        }
                    }
                }
            }
        }
    }
}