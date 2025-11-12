package me.miguelantonyortegasanta.tradumemo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val OPENROUTER_MODEL = "openrouter/polaris-alpha"

private val httpClient by lazy { OkHttpClient() }

/**
 * Hace una llamada al LLM "Inko" usando OpenRouter.
 *
 * @param userMessage Mensaje/pregunta actual del usuario.
 * @param transcriptContext Texto de la transcripción o nota que servirá de contexto.
 * @param apiKey API key de OpenRouter (por defecto usa BuildConfig).
 */
suspend fun askInko(
    userMessage: String,
    transcriptContext: String,
    apiKey: String = BuildConfig.OPENROUTER_API_KEY
): String = withContext(Dispatchers.IO) {

    if (apiKey.isBlank()) {
        return@withContext "Error: falta configurar la clave de OpenRouter (OPENROUTER_API_KEY)."
    }

    val url = "https://openrouter.ai/api/v1/chat/completions"

    // Recortar contexto si es muy largo para no romper el modelo
    val safeContext = if (transcriptContext.length > 4000) {
        transcriptContext.take(4000) + "\n\n[Contexto truncado por longitud.]"
    } else transcriptContext


    val messagesJson = JSONArray().apply {
        put(
            JSONObject().apply {
                put("role", "system")
                put(
                    "content",
                    "Eres Inko, un asistente de estudio. Respondes en español, de forma clara y estructurada. " +
                            "Se pueden adjuntar notas/transcripciones para basar tus respuestas." +
                            "Si el usuario pregunta algo que no se puede responder con el contexto, dilo explícitamente."
                )
            }
        )
        put(
            JSONObject().apply {
                put("role", "system")
                put(
                    "content",
                    "Contexto de la transcripción/notas:\n$safeContext"
                )
            }
        )
        put(
            JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            }
        )
    }

    val root = JSONObject().apply {
        put("model", OPENROUTER_MODEL)
        put("messages", messagesJson)
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = root.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $apiKey")
        .post(body)
        .build()

    httpClient.newCall(request).execute().use { resp ->
        val respBody = resp.body?.string().orEmpty()

        if (!resp.isSuccessful) {
            return@withContext "Error LLM (${resp.code}): ${resp.message}\n$respBody"
        }

        try {
            val json = JSONObject(respBody)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.getJSONObject(0)
                val message = first.optJSONObject("message")
                val content = message?.optString("content", "").orEmpty()
                if (content.isNotBlank()) {
                    return@withContext content.trim()
                }
            }
            return@withContext "No recibí una respuesta válida del modelo."
        } catch (e: Exception) {
            return@withContext "Error al procesar la respuesta del modelo: ${e.message}\n$respBody"
        }
    }
}
