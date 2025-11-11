import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import me.miguelantonyortegasanta.tradumemo.BuildConfig
import androidx.compose.material3.CircularProgressIndicator
import me.miguelantonyortegasanta.tradumemo.translateText
import android.content.Context
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import java.io.File
import java.io.FileOutputStream


// --- Recorder helper (AudioRecord -> WAV bytes) ---
class SimpleRecorder(
    private val sampleRate: Int = 16000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private var outStream = ByteArrayOutputStream()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        stop() // por seguridad
        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, // mejor fuente para voz
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize.coerceAtLeast(sampleRate * 2)
        )

        // Activar procesamiento de audio del sistema (si está disponible)
        recorder?.let { rec ->
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor.create(rec.audioSessionId)
            }
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl.create(rec.audioSessionId)
            }
        }

        outStream = ByteArrayOutputStream()
        recorder?.startRecording()   // 🎙️ aquí sigue igual, comienza la captura

        recordingThread = Thread {
            val buffer = ShortArray(bufferSize / 2)
            try {
                while (recorder != null && recorder!!.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = recorder!!.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        // convertir short -> little endian bytes
                        for (i in 0 until read) {
                            val s = buffer[i]
                            outStream.write(s.toInt() and 0xFF)
                            outStream.write((s.toInt() shr 8) and 0xFF)
                        }
                    }
                }
            } catch (_: Throwable) { /* ignore */ }
        }.also { it.start() }
    }

    fun stopAndGetWav(): ByteArray {
        try {
            recorder?.stop()
        } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        recordingThread?.interrupt()
        recordingThread = null

        val pcmBytes = outStream.toByteArray()
        return pcmToWav(pcmBytes, sampleRate, 1, 16)
    }

    fun stop() {
        try {
            recorder?.stop()
        } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        recordingThread?.interrupt()
        recordingThread = null
    }

    // WAV header + PCM (little endian)
    private fun pcmToWav(pcm: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = pcm.size + 36
        val header = ByteArrayOutputStream()
        val dos = DataOutputStream(header)
        try {
            dos.writeBytes("RIFF")
            dos.writeInt(Integer.reverseBytes(totalDataLen))
            dos.writeBytes("WAVE")
            dos.writeBytes("fmt ")
            dos.writeInt(Integer.reverseBytes(16)) // Subchunk1Size for PCM
            dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // AudioFormat = 1 (PCM)
            dos.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
            dos.writeInt(Integer.reverseBytes(sampleRate))
            dos.writeInt(Integer.reverseBytes(byteRate))
            dos.writeShort(java.lang.Short.reverseBytes((channels * bitsPerSample / 8).toShort()).toInt())
            dos.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
            dos.writeBytes("data")
            dos.writeInt(Integer.reverseBytes(pcm.size))
            dos.write(pcm)
            dos.flush()
            return header.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
            return pcm // fallback (raro)
        } finally {
            try { dos.close() } catch (_: Exception) {}
        }
    }
}



// --- Network: envia WAV base64 a Google Speech-to-Text (REST v1) ---
suspend fun sendWavToGoogleSpeech(
    wavBytes: ByteArray,
    languageCode: String = "es-CO"
): String {
    return withContext(Dispatchers.IO) {
        // URL con API key (la tuya debe llegar vía BuildConfig o variable segura)
        val apiKey = BuildConfig.GOOGLE_API_KEY
        val url = "https://speech.googleapis.com/v1/speech:recognize?key=$apiKey"

        // base64 encode (sin saltos)
        val audioBase64 = Base64.encodeToString(wavBytes, Base64.NO_WRAP)

        val json = JSONObject().apply {
            put("config", JSONObject().apply {
                put("encoding", "LINEAR16")
                put("sampleRateHertz", 16000)
                put("languageCode", languageCode)
                put("enableAutomaticPunctuation", true)
                put("useEnhanced", true)
                put("model", "default")
            })
            put("audio", JSONObject().apply {
                put("content", audioBase64)
            })
        }

        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                // devuelves error legible
                return@withContext "ERROR: ${resp.code} ${resp.message} - $respBody"
            }
            // parse simple: results[0].alternatives[0].transcript
            val root = JSONObject(respBody)
            val results = root.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val first = results.getJSONObject(0)
                val alts = first.optJSONArray("alternatives")
                if (alts != null && alts.length() > 0) {
                    return@withContext alts.getJSONObject(0).optString("transcript", "")
                }
            }
            return@withContext ""
        }
    }
}

@Composable
fun RecordButton(
    onTextRecognized: (String) -> Unit,
    onRecordingStart: (() -> Unit)? = null,
    languageCode: String = "es-CO",
    translate: Boolean = false,
    translationSourceLanguageCode: String = "en",
    translationTargetLanguageCode: String = "es",
    onOriginalRecognized: ((String) -> Unit)? = null
) {
    var isToggled by rememberSaveable { mutableStateOf(false) }
    var isProcessing by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { SimpleRecorder() }

    // Máxima duración
    val MAX_RECORDING_MS = 60_000L // 60 segundos

    // Permiso de micrófono
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* no-op */ }
    )
    LaunchedEffect(Unit) {
        val ok = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!ok) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Auto-stop cuando se llega al máximo
    LaunchedEffect(isToggled) {
        if (isToggled) {
            kotlinx.coroutines.delay(MAX_RECORDING_MS)
            if (isToggled) {
                isToggled = false
                isProcessing = true
                scope.launch {
                    val wav = withContext(Dispatchers.Default) { recorder.stopAndGetWav() }
                    //DEBUG: Guardar wav en memoria
                    //debugSaveWav(context, wav)
                    try {
                        //Speech-to-Text
                        val transcript = sendWavToGoogleSpeech(wav, languageCode)

                        onOriginalRecognized?.invoke(transcript)

                        //Traducción opcional
                        val finalText = if (translate) {
                            translateText(
                                text = transcript,
                                sourceLanguageCode = translationSourceLanguageCode,
                                targetLanguageCode = translationTargetLanguageCode
                            )
                        } else {
                            transcript
                        }

                        onTextRecognized(finalText)
                    } catch (e: Exception) {
                        onTextRecognized("Error: ${e.message}")
                    } finally {
                        isProcessing = false
                    }
                }
            }
        }
    }

    // UI
    if (isProcessing) {
        CircularProgressIndicator(
            color = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
    } else {
        FilledIconButton(
            onClick = {
                isToggled = !isToggled
                if (isToggled) {
                    // Empieza a grabar
                    onRecordingStart?.invoke()
                    recorder.start()
                } else {
                    // Dejar de grabar y procesar
                    isProcessing = true
                    scope.launch {
                        val wav = withContext(Dispatchers.Default) { recorder.stopAndGetWav() }

                        // 🔹 DEBUG: guardar el WAV SIEMPRE que pares manualmente
                        //debugSaveWav(context, wav)

                        try {
                            // 🔹 1) Speech-to-Text
                            val transcript = sendWavToGoogleSpeech(wav, languageCode)

                            onOriginalRecognized?.invoke(transcript)

                            // 🔹 2) Traducción opcional
                            val finalText = if (translate) {
                                translateText(
                                    text = transcript,
                                    sourceLanguageCode = translationSourceLanguageCode,
                                    targetLanguageCode = translationTargetLanguageCode
                                )
                            } else {
                                transcript
                            }

                            onTextRecognized(finalText)
                        } catch (e: Exception) {
                            onTextRecognized("Error: ${e.message}")
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isToggled) Color.Gray else Color.Red,
                contentColor = Color.White
            ),
            modifier = Modifier.size(80.dp),
        ) {
            Icon(
                imageVector = if (isToggled) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                contentDescription = if (isToggled) "Detener" else "Grabar",
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

//DEBUG: Guardar wav en memoria
fun debugSaveWav(context: Context, wavBytes: ByteArray): File {
    val dir = context.getExternalFilesDir(null) ?: context.filesDir
    val file = File(dir, "debug_recording_${System.currentTimeMillis()}.wav")

    FileOutputStream(file).use { fos ->
        fos.write(wavBytes)
    }

    android.util.Log.d("DEBUG_WAV", "Guardado WAV en: ${file.absolutePath}")
    return file
}
