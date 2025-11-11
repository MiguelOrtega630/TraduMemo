package me.miguelantonyortegasanta.tradumemo

import RecordButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth        // 🔹 nuevo
import com.google.firebase.firestore.FirebaseFirestore // 🔹 nuevo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(
    navController: NavController,
    docId: String? = null
) {

    var textoExtraido by rememberSaveable { mutableStateOf("") }

    // Estados del modo (Transcribir/Traducir)
    var mode by rememberSaveable { mutableStateOf(TranscriptionMode.TRANSCRIBE) }
    var modeLocked by rememberSaveable { mutableStateOf(false) }

    // Cosas del Título
    val defaultTitle = "Nueva transcripción"
    var title by rememberSaveable { mutableStateOf(defaultTitle) }
    var isEditingTitle by rememberSaveable { mutableStateOf(false) }
    var hasCustomTitle by rememberSaveable { mutableStateOf(false) }
    var titleFieldValue by remember { mutableStateOf(TextFieldValue(defaultTitle)) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleColor = if (hasCustomTitle) Color.Black else Color.Gray
    val iconColor = Color.Gray

    // Layout
    val overlayHeight = 120.dp
    val overlayBottomPadding = 24.dp
    val contentBottomPadding = overlayHeight + overlayBottomPadding

    var originalText by rememberSaveable { mutableStateOf("") }
    var translatedText by rememberSaveable { mutableStateOf("") }

    //Cargar una nota existente
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    var isLoadingExisting by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(docId) {
        if (docId != null) {
            val uid = auth.currentUser?.uid ?: return@LaunchedEffect

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("transcriptions")
                .document(docId)
                .get()
                .addOnSuccessListener { doc ->
                    val data = doc.data ?: return@addOnSuccessListener

                    val loadedTitle = data["title"] as? String ?: defaultTitle
                    title = loadedTitle
                    hasCustomTitle = loadedTitle != defaultTitle
                    titleFieldValue = TextFieldValue(loadedTitle)

                    val storedMode = when (data["mode"] as? String) {
                        "TRANSLATE" -> TranscriptionMode.TRANSLATE
                        else -> TranscriptionMode.TRANSCRIBE
                    }
                    mode = storedMode
                    modeLocked = true   // para que no cambien el modo al editar

                    originalText = data["originalText"] as? String ?: ""
                    translatedText = data["translatedText"] as? String ?: ""

                    // Lo que se ve en pantalla según modo
                    textoExtraido = if (storedMode == TranscriptionMode.TRANSCRIBE) {
                        originalText
                    } else {
                        translatedText
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // Si no hay contenido, no guardar
                            val hasContent = when (mode) {
                                TranscriptionMode.TRANSCRIBE -> textoExtraido.isNotBlank()
                                TranscriptionMode.TRANSLATE  -> translatedText.isNotBlank()
                            }
                            if (!hasContent) {
                                navController.popBackStack()
                                return@IconButton
                            }

                            val sourceLanguageCode = when (mode) {
                                TranscriptionMode.TRANSCRIBE -> "es-CO"
                                TranscriptionMode.TRANSLATE  -> "en-US"
                            }

                            val targetLanguageCode = when (mode) {
                                TranscriptionMode.TRANSCRIBE -> null
                                TranscriptionMode.TRANSLATE  -> "es-CO"
                            }

                            TranscriptionRepository.saveTranscription(
                                title = title,
                                originalText = if (mode == TranscriptionMode.TRANSLATE) originalText else textoExtraido,
                                translatedText = if (mode == TranscriptionMode.TRANSLATE) translatedText else "",
                                mode = mode,
                                sourceLanguageCode = sourceLanguageCode,
                                targetLanguageCode = targetLanguageCode,
                                docId = docId,
                                onSuccess = { navController.popBackStack() },
                                onError = { navController.popBackStack() }
                            )
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }

                },
                title = {
                    if (isEditingTitle) {
                        LaunchedEffect(isEditingTitle) {
                            if (isEditingTitle) {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }
                        TextField(
                            value = titleFieldValue,
                            onValueChange = { newValue ->
                                titleFieldValue = newValue
                                title = newValue.text
                                hasCustomTitle = newValue.text != defaultTitle
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = titleColor),
                            colors = TextFieldDefaults.textFieldColors(
                                containerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = titleColor,
                                focusedTextColor = titleColor,
                                unfocusedTextColor = titleColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    titleFieldValue = TextFieldValue(
                                        text = title,
                                        selection = TextRange(0, title.length)
                                    )
                                    isEditingTitle = true
                                }
                        ) {
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = titleColor
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar título",
                                tint = iconColor,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (isEditingTitle) {
                        TextButton(onClick = {
                            isEditingTitle = false
                            keyboardController?.hide()
                        }) { Text("OK") }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val scrollState = rememberScrollState()

            when {
                isLoadingExisting -> {
                    // 🔹 Cargando nota desde Firestore
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF4436))
                    }
                }
                loadError != null -> {
                    // 🔹 Error al cargar
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error al cargar la nota: $loadError",
                            color = Color.Red
                        )
                    }
                }
                else -> {
                    // 🔹 Contenido normal
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val modeText = when (mode) {
                            TranscriptionMode.TRANSCRIBE -> "Modo: Transcribir"
                            TranscriptionMode.TRANSLATE  -> "Modo: Traducir"
                        }
                        Text(
                            text = modeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        SelectionContainer {
                            Text(
                                text = when (mode) {
                                    TranscriptionMode.TRANSCRIBE -> textoExtraido
                                    TranscriptionMode.TRANSLATE  -> translatedText
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = contentBottomPadding)
                                    .verticalScroll(scrollState)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!modeLocked) {
                            ModeSegmentedControl(
                                mode = mode,
                                onModeChange = { if (!modeLocked) mode = it },
                                enabled = !modeLocked,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RecordButton(
                                onTextRecognized = { recognizedText ->
                                    if (mode == TranscriptionMode.TRANSLATE) {
                                        translatedText += if (translatedText.isNotBlank()) "\n$recognizedText" else recognizedText
                                    } else {
                                        textoExtraido += if (textoExtraido.isNotBlank()) "\n$recognizedText" else recognizedText
                                    }
                                },
                                onOriginalRecognized = { original ->
                                    if (mode == TranscriptionMode.TRANSLATE) {
                                        originalText += if (originalText.isNotBlank()) "\n$original" else original
                                    }
                                },
                                onRecordingStart = {
                                    if (!modeLocked) modeLocked = true
                                },
                                languageCode = when (mode) {
                                    TranscriptionMode.TRANSCRIBE -> "es-CO"  // audio en español
                                    TranscriptionMode.TRANSLATE  -> "en-US"  // audio en inglés
                                },
                                translate = (mode == TranscriptionMode.TRANSLATE),
                                translationSourceLanguageCode = "en",
                                translationTargetLanguageCode = "es"
                            )
                        }
                    }
                }
            }
        }
    }
}
