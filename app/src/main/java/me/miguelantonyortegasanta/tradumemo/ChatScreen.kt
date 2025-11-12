package me.miguelantonyortegasanta.tradumemo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val fromUser: Boolean,
    val text: String
)

data class SelectedContext(
    val id: String,
    val title: String,
    val text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    sessionIdArg: String? = null
) {
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var sessionId by rememberSaveable { mutableStateOf(sessionIdArg) }

    val selectedContexts = rememberSaveable(
        saver = listSaver(
            save = { list: MutableList<SelectedContext> ->
                list.map { listOf(it.id, it.title, it.text) }
            },
            restore = { saved ->
                mutableStateListOf<SelectedContext>().apply {
                    saved.forEach { triple ->
                        val l = triple as List<*>
                        add(
                            SelectedContext(
                                id = l[0] as String,
                                title = l[1] as String,
                                text = l[2] as String
                            )
                        )
                    }
                }
            }
        )
    ) {
        mutableStateListOf<SelectedContext>()
    }

    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val pickedDocId: String? = savedStateHandle?.get("pickedDocId")

    LaunchedEffect(pickedDocId) {
        val id = pickedDocId ?: return@LaunchedEffect

        savedStateHandle?.set("pickedDocId", null)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("transcriptions").document(id)
            .get()
            .addOnSuccessListener { doc ->
                val data = doc.data ?: return@addOnSuccessListener
                val title = (data["title"] as? String)?.takeIf { it.isNotBlank() } ?: "Nota sin título"
                val mode = data["mode"] as? String
                val original = data["originalText"] as? String ?: ""
                val translated = data["translatedText"] as? String ?: ""

                val contextText = if (mode == "TRANSLATE") translated else original

                if (selectedContexts.none { it.id == id }) {
                    selectedContexts.add(
                        SelectedContext(
                            id = id,
                            title = title,
                            text = contextText
                        )
                    )
                }
            }
    }

    // Si venimos con sessionIdArg (chat existente), cargamos su historial
    LaunchedEffect(sessionIdArg) {
        val existingId = sessionIdArg
        if (existingId != null) {
            ChatRepository.loadMessages(
                sessionId = existingId,
                onResult = { loaded ->
                    messages.clear()
                    messages.addAll(loaded)
                    // Si por alguna razón no hay mensajes, añadimos bienvenida y la guardamos
                    if (messages.isEmpty()) {
                        val welcome = ChatMessage(
                            fromUser = false,
                            text = "¡Hola! Soy Inko, tu asistente de estudio. " +
                                    "Puedo ayudarte a entender y trabajar con tus transcripciones. " +
                                    "Añade notas con el botón + y luego haz tus preguntas."
                        )
                        messages.add(welcome)
                        ChatRepository.appendMessage(
                            sessionId = existingId,
                            fromUser = false,
                            text = welcome.text
                        )
                    }
                }
            )
        } else {
            //Chat nuevo
            if (messages.isEmpty()) {
                messages.add(
                    ChatMessage(
                        fromUser = false,
                        text = "¡Hola! Soy Inko, tu asistente de estudio. " +
                                "Puedo ayudarte a entender y trabajar con tus transcripciones. " +
                                "Añade notas con el botón + y luego haz tus preguntas."
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inko") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("libraryPicker") }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir contexto")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF4ECD8),
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF4ECD8)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            //Carrusel de notas
            if (selectedContexts.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(selectedContexts, key = { it.id }) { ctx ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color.Gray),
                            color = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ctx.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.DarkGray
                                )
                                IconButton(
                                    onClick = { selectedContexts.remove(ctx) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar contexto",
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Lista de mensajes
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val bubbleColor = if (msg.fromUser) Color(0xFFD2C2F0) else Color.White

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.fromUser)
                            Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = bubbleColor,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 1.dp,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                modifier = Modifier.padding(10.dp),
                                color = Color.Black,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Input + enviar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Chatea con Inko") },
                    modifier = Modifier.weight(1f),
                    enabled = !isSending
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        val trimmed = input.trim()
                        if (trimmed.isEmpty() || isSending) return@FloatingActionButton

                        // Si aún no hay sesión, creamos una cuando se envía el primer mensaje
                        if (sessionId == null) {
                            val generatedTitle = "Chat " + SimpleDateFormat(
                                "dd/MM HH:mm",
                                Locale.getDefault()
                            ).format(Date())
                            sessionId = ChatRepository.createSession(title = generatedTitle)
                        }

                        val sid = sessionId

                        // Mensaje del usuario (si hay sesión, lo guardamos en Firestore)
                        val userMsg = ChatMessage(fromUser = true, text = trimmed)
                        messages.add(userMsg)
                        if (sid != null) {
                            ChatRepository.appendMessage(
                                sessionId = sid,
                                fromUser = true,
                                text = trimmed
                            )
                        }

                        input = ""
                        isSending = true

                        val combinedContext = selectedContexts.joinToString("\n\n") { ctx ->
                            "Nota: ${ctx.title}\n${ctx.text}"
                        }

                        scope.launch {
                            val respuesta = try {
                                askInko(
                                    userMessage = trimmed,
                                    transcriptContext = combinedContext
                                )
                            } catch (e: Exception) {
                                "Ocurrió un error al hablar con Inko: ${e.message}"
                            }

                            val botMsg = ChatMessage(fromUser = false, text = respuesta)
                            messages.add(botMsg)
                            if (sid != null) {
                                ChatRepository.appendMessage(
                                    sessionId = sid,
                                    fromUser = false,
                                    text = respuesta
                                )
                            }

                            isSending = false
                        }
                    },
                    containerColor = if (isSending) Color.Gray else Color(0xFFD2C2F0),
                    modifier = Modifier.size(48.dp),
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                            color = Color.DarkGray
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    }
}
