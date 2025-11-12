package me.miguelantonyortegasanta.tradumemo

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class ChatSessionSummary(
    val id: String,
    val title: String,
    val lastUpdated: Timestamp?
)

object ChatRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun userChatsCollection() =
        auth.currentUser?.uid?.let { uid ->
            db.collection("users")
                .document(uid)
                .collection("inkoChats")
        }

    private fun messagesCollection(sessionId: String) =
        userChatsCollection()
            ?.document(sessionId)
            ?.collection("messages")

    //Crear una nueva sesión de chat y devuelve su ID.
    fun createSession(
        title: String? = null
    ): String? {
        val chatsCol = userChatsCollection() ?: return null
        val docRef = chatsCol.document()
        val now = Timestamp.now()
        val data = hashMapOf(
            "title" to (title ?: "Nuevo chat"),
            "createdAt" to now,
            "updatedAt" to now
        )
        docRef.set(data)
        return docRef.id
    }


    //Agrega un mensaje a una sesión dada y actualiza el updatedAt de la sesión.
    fun appendMessage(
        sessionId: String,
        fromUser: Boolean,
        text: String,
        onError: (Exception) -> Unit = {}
    ) {
        val col = messagesCollection(sessionId) ?: return
        val now = Timestamp.now()
        val data = hashMapOf(
            "fromUser" to fromUser,
            "text" to text,
            "timestamp" to now
        )

        col.add(data)
            .addOnFailureListener(onError)

        // Actualizamos updatedAt en la sesión
        userChatsCollection()
            ?.document(sessionId)
            ?.update("updatedAt", now)
    }


    //Cargar mensajes de una sesión.
    fun loadMessages(
        sessionId: String,
        onResult: (List<ChatMessage>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        val col = messagesCollection(sessionId) ?: return
        col.orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    val text = doc.getString("text") ?: return@mapNotNull null
                    val fromUser = doc.getBoolean("fromUser") ?: false
                    ChatMessage(fromUser = fromUser, text = text)
                }
                onResult(list)
            }
            .addOnFailureListener(onError)
    }

    //TODO: Listar sesiones
    fun listSessions(
        onResult: (List<ChatSessionSummary>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        val col = userChatsCollection() ?: return
        col.orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    ChatSessionSummary(
                        id = doc.id,
                        title = doc.getString("title") ?: "Chat sin título",
                        lastUpdated = doc.getTimestamp("updatedAt")
                    )
                }
                onResult(list)
            }
            .addOnFailureListener(onError)
    }
}
