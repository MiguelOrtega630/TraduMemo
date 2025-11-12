package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await


@Composable
fun MainScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    // Estados para manejar la carga de datos
    var recentTranscriptions by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }
    var recentChats by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, uid) {
        if (uid == null) {
            isLoading = false
            return@LaunchedEffect
        }

        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            isLoading = true
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(uid)

            coroutineScope {
                // Tarea para cargar transcripciones
                val transcriptionsDeferred = async {
                    try {
                        val snapshot = userRef.collection("transcriptions")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(5)
                            .get().await()

                        snapshot.documents.mapNotNull { doc ->
                            val data = doc.data
                            val timestamp = data?.get("timestamp") as? Timestamp
                            if (timestamp != null) {
                                LibraryItem(doc.id, data, ItemType.TRANSCRIPTION, timestamp)
                            } else {
                                null
                            }
                        }
                    } catch (e: Exception) {
                        println("ERROR GRAVE al cargar TRANSCRIPCIONES: ${e.message}")
                        emptyList<LibraryItem>()
                    }
                }

                // Tarea para cargar chats (CORREGIDA CON LA MAYÚSCULA)
                val chatsDeferred = async {
                    try {
                        // 1. APUNTAMOS A LA COLECCIÓN CORRECTA: "inkoChats" (con C mayúscula)
                        val snapshot = userRef.collection("inkoChats")
                            // 2. ORDENAMOS POR "updatedAt"
                            .orderBy("updatedAt", Query.Direction.DESCENDING)
                            .limit(5)
                            .get().await()

                        snapshot.documents.mapNotNull { doc ->
                            val data = doc.data
                            // 3. BUSCAMOS EL CAMPO "updatedAt" para usarlo como timestamp
                            val timestamp = data?.get("updatedAt") as? Timestamp
                            if (timestamp != null) {
                                LibraryItem(doc.id, data, ItemType.CHAT, timestamp)
                            } else {
                                println("DEBUG: Chat ${doc.id} descartado por no tener campo 'updatedAt'.")
                                null
                            }
                        }
                    } catch (e: Exception) {
                        println("ERROR GRAVE al cargar CHATS: ${e.message}")
                        emptyList<LibraryItem>()
                    }
                }

                // Esperamos a que ambas tareas terminen y actualizamos el estado
                recentTranscriptions = transcriptionsDeferred.await()
                recentChats = chatsDeferred.await()
            }

            isLoading = false
        }
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF4ECD8),
        bottomBar = {
            BottomNavigationBar(navController, "inicio")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("chat") },
                containerColor = Color(0xFFD2C2F0),
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Psychology, contentDescription = "Nuevo Chat con Inko")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 8.dp)
        ) {

            // --- SECCIÓN DE TRANSCRIPCIONES RECIENTES ---
            Text(
                text = "Transcripciones y traducciones recientes",
                color = Color(0xFFFF4436),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (recentTranscriptions.isEmpty()) {
                Text(
                    "No tienes transcripciones recientes.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.Gray
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentTranscriptions, key = { it.id }) { item ->
                        RecentItemCard(
                            item = item,
                            onClick = { navController.navigate("transcription/${item.id}") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- SECCIÓN DE CHATS RECIENTES ---
            Text(
                text = "Chats recientes",
                color = Color(0xFFFF4436),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            if (!isLoading && recentChats.isEmpty()) {
                Text(
                    "No tienes chats recientes con Inko.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.Gray
                )
            } else if (!recentChats.isEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentChats, key = { it.id }) { item ->
                        RecentItemCard(
                            item = item,
                            onClick = { navController.navigate("chat/${item.id}") }
                        )
                    }
                }
            }
        }
    }
}

// Composable reutilizable para las tarjetas de la pantalla principal
@Composable
fun RecentItemCard(item: LibraryItem, onClick: () -> Unit) {
    val title = item.data["title"] as? String ?: when (item.type) {
        ItemType.TRANSCRIPTION -> "(Sin título)"
        ItemType.CHAT -> "Chat con Inko"
    }
    val description = "Ábrelo para ver el contenido..."
    val cardColor = when (item.type) {
        ItemType.TRANSCRIPTION -> Color(0xFFF6E8DC)
        ItemType.CHAT -> Color(0xFFE0EDF8)
    }
    val icon = when (item.type) {
        ItemType.TRANSCRIPTION -> Icons.Default.Translate
        ItemType.CHAT -> Icons.Default.Forum
    }

    Card(
        modifier = Modifier
            .width(180.dp)
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}


@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String) {
    NavigationBar(containerColor = Color(0xFFF4ECD8)) {
        NavigationBarItem(
            selected = currentRoute == "inicio",
            onClick = { /* Ya estamos aquí */ },
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio", fontSize = 12.sp) }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                navController.navigate("transcription")
            },
            icon = { Icon(Icons.Default.NoteAdd, contentDescription = "Nueva Transcripción") },
            label = {
                Text(
                    "Nueva nota",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
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
