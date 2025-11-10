package me.miguelantonyortegasanta.tradumemo

import me.miguelantonyortegasanta.tradumemo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

suspend fun translateText(
    text: String,
    sourceLanguageCode: String,
    targetLanguageCode: String
): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GOOGLE_API_KEY
    val url = "https://translation.googleapis.com/language/translate/v2?key=$apiKey"

    val json = JSONObject().apply {
        put("q", text)
        put("source", sourceLanguageCode)
        put("target", targetLanguageCode)
        put("format", "text")
    }

    val client = OkHttpClient()
    val body = json.toString()
        .toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder().url(url).post(body).build()

    client.newCall(request).execute().use { resp ->
        val respBody = resp.body?.string().orEmpty()
        android.util.Log.d("TradumemoAPI", "Respuesta API: $respBody")
        if (!resp.isSuccessful) return@withContext "ERROR: ${resp.code} ${resp.message}"

        val root = JSONObject(respBody)
        val translations = root
            .optJSONObject("data")
            ?.optJSONArray("translations")
        return@withContext translations
            ?.optJSONObject(0)
            ?.optString("translatedText", "")
            ?: ""
    }
}
