package com.prompthavenai.falcibuket.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableReference
import com.prompthavenai.falcibuket.data.model.CoffeeResult
import com.prompthavenai.falcibuket.data.model.ReadingEntry
import com.prompthavenai.falcibuket.data.model.TimeWindow
import com.prompthavenai.falcibuket.data.model.VisualObservation
import kotlinx.coroutines.tasks.await

data class ChatReply(val conversationId: String, val reply: String)

interface BuketBackend {
    suspend fun chat(message: String, conversationId: String?): ChatReply
    suspend fun analyzeCoffee(cupBase64: String, saucerBase64: String?, question: String?): CoffeeResult
    suspend fun loadLatestConversation(): Pair<String?, List<com.prompthavenai.falcibuket.data.model.ChatMessage>>
    suspend fun readings(limit: Int = 20): List<ReadingEntry>
    suspend fun memoryFacts(limit: Int = 6): List<String>
    suspend fun clearMemory()
}

object FirebaseGate {
    val configured: Boolean get() = runCatching { FirebaseAuth.getInstance().app }.isSuccess

    suspend fun ensureUid(): String {
        if (!configured) {
            throw IllegalStateException("AUTH_ERROR: Firebase bu sürümde yapılandırılmadı (google-services.json eksik).")
        }
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it.uid }
        val user = auth.signInAnonymously().await().user
            ?: throw IllegalStateException("AUTH_ERROR: anonymous sign-in başarısız")
        return user.uid
    }
}

class FirebaseBuketBackend : BuketBackend {

    override suspend fun chat(message: String, conversationId: String?): ChatReply {
        val payload = buildMap {
            put("message", message)
            conversationId?.let { put("conversationId", it) }
        }
        val result = callable("chatWithBuket").call(payload).await()
        val map = asMap(result.getData())
        return ChatReply(
            conversationId = map["conversationId"] as? String ?: "",
            reply = map["reply"] as? String ?: ""
        )
    }

    override suspend fun analyzeCoffee(
        cupBase64: String,
        saucerBase64: String?,
        question: String?
    ): CoffeeResult {
        val payload = buildMap {
            put("cupImageBase64", cupBase64)
            saucerBase64?.let { put("saucerImageBase64", it) }
            question?.takeIf { it.isNotBlank() }?.let { put("userQuestion", it) }
        }
        val result = callable("analyzeCoffeeReading").call(payload).await()
        return parseCoffeeResult(asMap(result.getData())["result"])
    }

    override suspend fun loadLatestConversation(): Pair<String?, List<com.prompthavenai.falcibuket.data.model.ChatMessage>> {
        val uid = FirebaseGate.ensureUid()
        val db = FirebaseFirestore.getInstance()
        val conversation = db.collection("users/$uid/conversations")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull() ?: return null to emptyList()
        val messages = conversation.reference
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(30)
            .get()
            .await()
        val list = messages.documents.mapNotNull { doc ->
            val role = doc.getString("role") ?: return@mapNotNull null
            val content = doc.getString("content") ?: return@mapNotNull null
            com.prompthavenai.falcibuket.data.model.ChatMessage(
                text = content,
                fromUser = role == "user"
            )
        }
        return conversation.id to list
    }

    override suspend fun readings(limit: Int): List<ReadingEntry> {
        val uid = FirebaseGate.ensureUid()
        val snap = FirebaseFirestore.getInstance()
            .collection("users/$uid/readings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        return snap.documents.mapNotNull { doc ->
            val map = doc.data ?: return@mapNotNull null
            val result = asMap(map["result"])
            ReadingEntry(
                id = doc.id,
                type = map["type"] as? String ?: "",
                question = map["question"] as? String,
                createdAtMillis = (map["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L,
                title = result["title"] as? String ?: "${map["type"]} Falı",
                summary = result["summary"] as? String ?: "",
                generalEnergy = result["generalEnergy"] as? String ?: "",
                love = result["love"] as? String ?: "",
                careerMoney = result["careerMoney"] as? String ?: "",
                nearFuture = result["nearFuture"] as? String ?: "",
                highlight = result["highlight"] as? String ?: "",
                visualObservations = parseObservations(result["visualObservations"]),
                timeWindows = parseTimeWindows(result["timeWindows"])
            )
        }
    }

    override suspend fun memoryFacts(limit: Int): List<String> {
        if (!FirebaseGate.configured) return emptyList()
        val uid = FirebaseGate.ensureUid()
        val snap = FirebaseFirestore.getInstance()
            .collection("users/$uid/memories")
            .orderBy("importance", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        return snap.documents.mapNotNull { d ->
            if (d.getBoolean("active") == false) null else d.getString("fact")
        }
    }

    override suspend fun clearMemory() {
        val uid = FirebaseGate.ensureUid()
        val db = FirebaseFirestore.getInstance()
        val memories = db.collection("users/$uid/memories").get().await()
        val batch = db.batch()
        memories.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    /**
     * Tüm çağrılabilir fonksiyonlar için tek merkezi kimlik kapısı: mevcut
     * anonim kullanıcı yeniden kullanılır, yoksa signInAnonymously() yapılır ve
     * ancak başarılı kimlik doğrulamadan sonra çağrı referansı döner. Böylece
     * hiçbir ekran kendi sign-in mantığını kopyalamaz ve hiçbir backend çağrısı
     * kimliksiz çalışmaz.
     */
    private suspend fun callable(name: String): HttpsCallableReference {
        FirebaseGate.ensureUid()
        return FirebaseFunctions.getInstance(BackendConfig.FUNCTIONS_REGION).getHttpsCallable(name)
    }

    private fun asMap(data: Any?): Map<*, *> = (data as? Map<*, *>) ?: emptyMap<Any, Any>()

    private fun parseCoffeeResult(raw: Any?): CoffeeResult {
        val map = asMap(raw)
        fun str(key: String) = map[key] as? String ?: ""
        return CoffeeResult(
            title = str("title").ifBlank { "Fincanın Sana Ne Söylüyor?" },
            summary = str("summary"),
            visualObservations = parseObservations(map["visualObservations"]),
            generalEnergy = str("generalEnergy"),
            love = str("love"),
            careerMoney = str("careerMoney"),
            nearFuture = str("nearFuture"),
            highlight = str("highlight"),
            timeWindows = parseTimeWindows(map["timeWindows"])
        )
    }

    private fun parseObservations(raw: Any?): List<VisualObservation> =
        (raw as? List<*>)?.mapNotNull { item ->
            val m = asMap(item)
            val obs = m["observation"] as? String ?: return@mapNotNull null
            VisualObservation(obs, m["interpretation"] as? String ?: "")
        } ?: emptyList()

    private fun parseTimeWindows(raw: Any?): List<TimeWindow> =
        (raw as? List<*>)?.mapNotNull { item ->
            val m = asMap(item)
            val topic = m["topic"] as? String ?: return@mapNotNull null
            TimeWindow(topic, m["window"] as? String ?: "")
        } ?: emptyList()
}

fun FirebaseFunctionsException.hasDetailCode(code: String): Boolean =
    (details as? Map<*, *>)?.get("code") == code
