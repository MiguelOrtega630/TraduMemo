package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.ui.text.style.TextAlign

@Composable
fun MainScreen(navController: NavController) {


    val transcripciones = listOf(
        "Cómo descubrí hacer tierra" to "La novela, primera parte. ¿Saben? Nunca creí...",
        "Hacer tierra arruinó mi vida" to "La novela, segunda parte. Estoy completamente...",
        "Hacer tierra es tarea" to "Final de la trilogía. Después de años intentándolo..."
    )

    val chats = listOf(
        "Como hacer tierra" to "No sé",
        "Como hacer tierra 2" to "No tampoco sé",
        "Como hacer tierra 3" to "Adivina"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF4ECD8),
        bottomBar = {
            BottomNavigationBar(navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("chat") },
                containerColor = Color(0xFFD2C2F0),
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar / Chat")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            Text(
                text = "Transcripciones recientes",
                color = Color(0xFFFF4436),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transcripciones) { (titulo, desc) ->
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .height(120.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                            Spacer(Modifier.height(4.dp))
                            Text(desc, fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))


            Text(
                text = "Chats recientes",
                color = Color(0xFFFF4436),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chats) { (titulo, desc) ->
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(100.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            Spacer(Modifier.height(4.dp))
                            Text(desc, fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar(containerColor = Color(0xFFF4ECD8)) {
        NavigationBarItem(
            selected = true,
            onClick = {  },
            icon = { Icon(Icons.Default.Attachment, contentDescription = "Inicio") },
            label = { Text("Inicio", fontSize = 12.sp) }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                navController.navigate("transcription")
            },
            icon = { Icon(Icons.Default.Note, contentDescription = "Transcripciones") },
            label = {
                Text(
                "Nueva \ntranscripción",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,    // Center the text inside its box
                modifier = Modifier.fillMaxWidth()  // Make sure it can center horizontally
            )}
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                navController.navigate("library")
            },
            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Biblioteca") },
            label = { Text("Biblioteca", fontSize = 12.sp) }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                navController.navigate("chat")
            },
            icon = { Icon(Icons.Default.BugReport, contentDescription = "Inko") },
            label = { Text("Inko", fontSize = 12.sp) }
        )
    }
}