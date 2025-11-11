package me.miguelantonyortegasanta.tradumemo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// Cambia esto por tu modelo favorito en OpenRouter
private const val OPENROUTER_MODEL = "openai/gpt-4o-mini" // por ejemplo

suspend fun askInko(
    userMessage: String,
    transcriptContext: String,
    apiKey: String = BuildConfig.OPENROUTER_API_KEY
): String = withContext(Dispatchers.IO) {

    val url = "https://openrouter.ai/api/v1/chat/completions"

    // OJO: si la transcripción es larguísima, mejor recortarla un poco
    val safeContext = if (transcriptContext.length > 4000) {
        transcriptContext.take(4000) + "\n\n[Contexto truncado]"
    } else transcriptContext

    val json = JSONObject().apply {
        put("model", OPENROUTER_MODEL)
        put("messages", listOf(
            JSONObject().apply {
                put("role", "system")
                put("content", "Eres Inko, un asistente de estudio. Responde en español, usando la transcripción dada como contexto. Si el usuario pregunta algo fuera del contexto, dilo explícitamente.")
            },
            JSONObject().apply {
                put("role", "system")
                put("content", "Contexto de la transcripción:\n$safeContext")
            },
            JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            }
        ))
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = json.toString().toRequestBody(mediaType)
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $apiKey")
        // Opcionales pero recomendados por OpenRouter:
        // .addHeader("HTTP-Referer", "https://tusitio.com")
        // .addHeader("X-Title", "Tradumemo")
        .post(body)
        .build()

    client.newCall(request).execute().use { resp ->
        val respBody = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
            return@withContext "Error LLM: ${resp.code} ${resp.message} - $respBody"
        }

        val root = JSONObject(respBody)
        val choices = root.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val msg = choices.getJSONObject(0).optJSONObject("message")
            return@withContext msg?.optString("content", "") ?: ""
        }

        return@withContext "No recibí respuesta válida del modelo."
    }
}
