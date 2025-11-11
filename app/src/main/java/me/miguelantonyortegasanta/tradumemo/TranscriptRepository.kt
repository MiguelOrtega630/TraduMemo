package me.miguelantonyortegasanta.tradumemo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

object TranscriptionRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun saveTranscription(
        title: String,
        originalText: String,
        translatedText: String,
        mode: TranscriptionMode,
        sourceLanguageCode: String,
        targetLanguageCode: String?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError(IllegalStateException("No logged-in user"))
            return
        }

        val data = hashMapOf(
            "title" to title,
            "originalText" to originalText,
            "translatedText" to translatedText,
            "mode" to mode.name,
            "sourceLanguageCode" to sourceLanguageCode,
            "targetLanguageCode" to targetLanguageCode,
            "timestamp" to Timestamp.now()
        )

        db.collection("users")
            .document(uid)
            .collection("transcriptions")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }
}
