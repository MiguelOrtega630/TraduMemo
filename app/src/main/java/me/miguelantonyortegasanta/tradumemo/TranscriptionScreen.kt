package me.miguelantonyortegasanta.tradumemo

import RecordToggleIconButtonWithCloudSTT
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import me.miguelantonyortegasanta.tradumemo.TranscriptionMode
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(navController: NavController) {

    var textoExtraido by rememberSaveable { mutableStateOf("") }

    // Mode state: selectable until first recording starts
    var mode by rememberSaveable { mutableStateOf(TranscriptionMode.TRANSCRIBE) }
    var modeLocked by rememberSaveable { mutableStateOf(false) }

    // Title state
    val defaultTitle = "Nueva transcripción"
    var title by rememberSaveable { mutableStateOf(defaultTitle) }
    var isEditingTitle by rememberSaveable { mutableStateOf(false) }
    var hasCustomTitle by rememberSaveable { mutableStateOf(false) }
    var titleFieldValue by remember { mutableStateOf(TextFieldValue(defaultTitle)) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleColor = if (hasCustomTitle) Color.Black else Color.Gray
    val iconColor = Color.Gray

    // Layout constants (bigger bottom padding to fit control + button)
    val overlayHeight = 120.dp
    val overlayBottomPadding = 24.dp
    val contentBottomPadding = overlayHeight + overlayBottomPadding

    var originalText by rememberSaveable { mutableStateOf("") }
    var translatedText by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // Si no hay texto, no guardamos nada
                            if (textoExtraido.isBlank()) {
                                navController.popBackStack()
                                return@IconButton
                            }

                            // Idioma fuente y destino según el modo
                            val sourceLanguageCode = when (mode) {
                                TranscriptionMode.TRANSCRIBE -> "es-CO"   // o "es-ES", según estés usando
                                TranscriptionMode.TRANSLATE  -> "en-US"   // por ejemplo, audio en inglés
                            }

                            val targetLanguageCode = when (mode) {
                                TranscriptionMode.TRANSCRIBE -> null      // no hay traducción como tal
                                TranscriptionMode.TRANSLATE  -> "es-CO"   // traducimos a español
                            }

                            TranscriptionRepository.saveTranscription(
                                title = title,
                                originalText = if (mode == TranscriptionMode.TRANSLATE) originalText else textoExtraido,
                                translatedText = if (mode == TranscriptionMode.TRANSLATE) translatedText else "",
                                mode = mode,
                                sourceLanguageCode = when (mode) {
                                    TranscriptionMode.TRANSCRIBE -> "es-CO"
                                    TranscriptionMode.TRANSLATE -> "en-US"
                                },
                                targetLanguageCode = when (mode) {
                                    TranscriptionMode.TRANSCRIBE -> null
                                    TranscriptionMode.TRANSLATE -> "es-CO"
                                },
                                onSuccess = { navController.popBackStack() },
                                onError = { e -> navController.popBackStack() }
                            )
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
                ,
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
            val backgroundColor = MaterialTheme.colorScheme.background
            val fadeColor = backgroundColor.copy(alpha = 0.9f)

            // Main content: mode label + scrollable text
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 🏷️ Mode label (always visible)
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

                // Selectable + scrollable text with fades
                SelectionContainer {
                    Text(
                        text = textoExtraido,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = contentBottomPadding)
                            .verticalScroll(scrollState)
                            .drawWithContent {
                                drawContent()

                                val gradientHeight = 80f
                                val topFade = Brush.verticalGradient(
                                    colors = listOf(backgroundColor, fadeColor, Color.Transparent),
                                    startY = 0f,
                                    endY = gradientHeight
                                )
                                val bottomFade = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, fadeColor, backgroundColor),
                                    startY = size.height - gradientHeight,
                                    endY = size.height
                                )

                                if (scrollState.value > 0) {
                                    drawRect(brush = topFade, size = size)
                                }
                                if (scrollState.canScrollForward) {
                                    drawRect(brush = bottomFade, size = size)
                                }
                            }
                    )
                }
            }

            // Bottom overlay: segmented control (when unlocked) + record button
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
                    RecordToggleIconButtonWithCloudSTT(
                        onTextRecognized = { recognizedText ->
                            // si estamos traduciendo, este recognizedText ya viene traducido
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


