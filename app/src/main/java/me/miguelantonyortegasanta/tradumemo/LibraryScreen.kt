package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem


@Composable
fun LibraryScreen(navController: NavController, pickerMode: Boolean = false) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    var transcriptions by remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var transcriptionToDelete by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(Unit) {
        if (uid == null) {
            error = "No hay usuario autenticado"
            isLoading = false
            return@LaunchedEffect
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("transcriptions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                transcriptions = result.documents.mapNotNull { doc ->
                    doc.id to (doc.data ?: emptyMap())
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                error = e.message
                isLoading = false
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF4ECD8),
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFF4ECD8)) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    navController.navigate("transcription")
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar")
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

            transcriptions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes transcripciones guardadas aún.", color = Color.Gray)
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
                        items(transcriptions) { (docId, data) ->
                            TranscriptionItem(
                                data = data,
                                onDeleteClick = {
                                    transcriptionToDelete = docId
                                    showDialog = true
                                },
                                onOpenClick = {
                                    if (pickerMode) {
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("pickedDocId", docId)

                                        navController.popBackStack()
                                    } else {
                                        navController.navigate("transcription/$docId")
                                    }
                                }

                            )
                        }
                    }
                }
            }
        }


        if (showDialog && transcriptionToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("¿Seguro que deseas borrar esta transcripción?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            val id = transcriptionToDelete
                            transcriptionToDelete = null
                            if (uid != null && id != null) {
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(uid)
                                    .collection("transcriptions")
                                    .document(id)
                                    .delete()
                                    .addOnSuccessListener {
                                        transcriptions = transcriptions.filterNot { it.first == id }
                                    }
                            }
                        }
                    ) {
                        Text("Sí", color = Color(0xFFFF4436))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranscriptionItem(
    data: Map<String, Any>,
    onDeleteClick: () -> Unit,
    onOpenClick: () -> Unit,
) {
    val title = data["title"] as? String ?: "(Sin título)"
    val ts = (data["timestamp"] as? Timestamp)?.toDate()
    val formattedDate = ts?.let {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
    } ?: ""

    val mode = data["mode"] as? String
    val isTranslated = mode == "TRANSLATE"

    val hasInko = false

    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Título + fecha
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2C2C2C),
                        maxLines = 1
                    )
                    if (formattedDate.isNotBlank()) {
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slot Inko
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasInko) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Inko",
                                tint = Color(0xFF444444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTranslated) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Traducción",
                                tint = Color(0xFF444444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            DropdownMenuItem(
                text = { Text("Eliminar nota") },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                }
            )
        }
    }
}



