package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Estructura de datos unificada para manejar tanto chats como transcripciones.
data class LibraryItem(
    val id: String,
    val data: Map<String, Any>,
    val type: ItemType, // Para saber si es un CHAT o una TRANSCRIPTION
    val timestamp: Timestamp
)

enum class ItemType {
    TRANSCRIPTION,
    CHAT
}


@Composable
fun LibraryScreen(navController: NavController, pickerMode: Boolean = false) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    // Estado para la lista unificada
    var libraryItems by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Estado para saber qué item borrar (id y tipo)
    var itemToDelete by remember { mutableStateOf<LibraryItem?>(null) }


    // LaunchedEffect para cargar los datos de Firestore
    LaunchedEffect(uid) {
        if (uid == null) {
            error = "No hay usuario autenticado."
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        try {
            // 1. Obtener Transcripciones
            val transcriptionsSnapshot = userRef.collection("transcriptions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()

            val transcriptionsList = transcriptionsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data
                val timestamp = data?.get("timestamp") as? Timestamp
                if (timestamp != null) {
                    LibraryItem(doc.id, data, ItemType.TRANSCRIPTION, timestamp)
                } else {
                    null
                }
            }

            // 2. Obtener Chats con Inko
            val chatsSnapshot = userRef.collection("chats")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()

            val chatsList = chatsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data
                val timestamp = data?.get("timestamp") as? Timestamp
                if (timestamp != null) {
                    LibraryItem(doc.id, data, ItemType.CHAT, timestamp)
                } else {
                    null
                }
            }

            // 3. Combinar y ordenar las listas por fecha
            libraryItems = (transcriptionsList + chatsList).sortedByDescending { it.timestamp }
            isLoading = false

        } catch (e: Exception) {
            error = e.message ?: "Error al cargar los datos."
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF4ECD8),
        bottomBar = {
            // El BottomBar ahora debe ofrecer crear una nueva transcripción o un nuevo chat.
            // Por simplicidad, mantendremos el botón de "Agregar" que lleva a transcripción,
            // pero podrías expandirlo con un menú si lo deseas.
            BottomAppBar(containerColor = Color(0xFFF4ECD8)) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                Spacer(Modifier.weight(1f))
                // Podrías añadir otro botón para "Nuevo Chat" aquí
                // IconButton(onClick = { navController.navigate("chat") }) { ... }
                IconButton(onClick = {
                    navController.navigate("transcription")
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Transcripción")
                }
            }
        }
    ) { padding ->

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF4436))
                }
            }

            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = Color.Red)
                }
            }

            libraryItems.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tu biblioteca está vacía.", color = Color.Gray)
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF8EDE0))
                ) {

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
                        items(libraryItems, key = { it.id }) { item ->
                            when (item.type) {
                                ItemType.TRANSCRIPTION -> {
                                    TranscriptionItem(
                                        data = item.data,
                                        onDeleteClick = {
                                            itemToDelete = item
                                            showDialog = true
                                        },
                                        onOpenClick = {
                                            if (pickerMode) {
                                                navController.previousBackStackEntry
                                                    ?.savedStateHandle
                                                    ?.set("pickedDocId", item.id)
                                                navController.popBackStack()
                                            } else {
                                                navController.navigate("transcription/${item.id}")
                                            }
                                        }
                                    )
                                }
                                ItemType.CHAT -> {
                                    // Nuevo Composable para los chats
                                    ChatItem(
                                        data = item.data,
                                        onDeleteClick = {
                                            itemToDelete = item
                                            showDialog = true
                                        },
                                        onOpenClick = {
                                            // Asegúrate de tener una ruta de navegación para los chats
                                            navController.navigate("chat/${item.id}")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diálogo de confirmación para borrar (ahora genérico)
        if (showDialog && itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("¿Seguro que deseas borrar este elemento?") },
                text = { Text("Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val item = itemToDelete
                            if (uid != null && item != null) {
                                val collectionPath = if (item.type == ItemType.TRANSCRIPTION) "transcriptions" else "chats"
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(uid)
                                    .collection(collectionPath)
                                    .document(item.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        libraryItems = libraryItems.filterNot { it.id == item.id }
                                    }
                            }
                            showDialog = false
                            itemToDelete = null
                        }
                    ) {
                        Text("Sí, borrar", color = Color(0xFFFF4436))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                        itemToDelete = null
                    }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}

// Composable para un item de transcripción (modificado ligeramente)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranscriptionItem(
    data: Map<String, Any>,
    onDeleteClick: () -> Unit,
    onOpenClick: () -> Unit,
) {
    val title = data["title"] as? String ?: "(Sin título)"
    val ts = data["timestamp"] as? Timestamp
    val formattedDate = ts?.toDate()?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: ""
    val mode = data["mode"] as? String
    val isTranslated = mode == "TRANSLATE"

    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .combinedClickable(
                    onClick = { onOpenClick() },
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF6E8DC)),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C2C2C), maxLines = 1)
                    if (formattedDate.isNotBlank()) {
                        Text(text = formattedDate, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                // Icono para indicar si es traducción
                if (isTranslated) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Traducción",
                        tint = Color(0xFF444444),
                        modifier = Modifier.size(18.dp).padding(start = 8.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            DropdownMenuItem(text = { Text("Eliminar nota") }, onClick = {
                showMenu = false
                onDeleteClick()
            })
        }
    }
}

// ¡NUEVO! Composable para mostrar un item de chat en la lista
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItem(
    data: Map<String, Any>,
    onDeleteClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    // Asumimos que los chats tienen un título y un timestamp
    val title = data["title"] as? String ?: "Chat con Inko"
    val ts = data["timestamp"] as? Timestamp
    val formattedDate = ts?.toDate()?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: ""

    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .combinedClickable(
                    onClick = { onOpenClick() },
                    onLongClick = { showMenu = true }
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0EDF8)), // Un color diferente para distinguirlos
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C2C2C), maxLines = 1)
                    if (formattedDate.isNotBlank()) {
                        Text(text = formattedDate, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                // Icono distintivo para los chats
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Chat con Inko",
                    tint = Color(0xFF444444),
                    modifier = Modifier.size(18.dp).padding(start = 8.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            DropdownMenuItem(text = { Text("Eliminar chat") }, onClick = {
                showMenu = false
                onDeleteClick()
            })
        }
    }
}



