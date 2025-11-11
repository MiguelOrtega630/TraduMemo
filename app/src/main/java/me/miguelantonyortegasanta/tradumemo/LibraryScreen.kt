package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun LibraryScreen(navController: NavController) {
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
                IconButton(onClick = { /* agregar nueva transcripción */ }) {
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

@Composable
fun TranscriptionItem(
    data: Map<String, Any>,
    onDeleteClick: () -> Unit
) {
    val title = data["title"] as? String ?: "(Sin título)"
    val original = data["originalText"] as? String ?: ""
    val translated = data["translatedText"] as? String ?: ""
    val timestamp = (data["timestamp"] as? Timestamp)?.toDate()?.toString() ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6E8DC)),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 🔹 Avatar + textos
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE8DEF8),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(title.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2C2C2C)
                    )
                    if (original.isNotBlank()) {
                        Text(
                            text = original.take(60),
                            fontSize = 14.sp,
                            color = Color(0xFF4A4A4A)
                        )
                    }
                    if (translated.isNotBlank()) {
                        Text(
                            text = translated.take(60),
                            fontSize = 13.sp,
                            color = Color(0xFF6C6C6C)
                        )
                    }
                    Text(
                        text = "🗓 $timestamp",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // 🔹 Botones
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF4436))
                }
                IconButton(onClick = { /* editar */ }) {
                    Icon(Icons.Default.EditNote, contentDescription = "Editar", tint = Color(0xFF444444))
                }
                IconButton(onClick = { /* IA */ }) {
                    Icon(Icons.Default.BugReport, contentDescription = "IA", tint = Color(0xFF444444))
                }
                IconButton(onClick = { /* traducir */ }) {
                    Icon(Icons.Default.Translate, contentDescription = "Traducir", tint = Color(0xFF444444))
                }
            }
        }
    }
}
