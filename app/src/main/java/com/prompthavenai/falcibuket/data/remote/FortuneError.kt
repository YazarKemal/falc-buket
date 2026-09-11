package com.prompthavenai.falcibuket.data.remote

enum class FortuneErrorCode {
    NO_NETWORK, AUTH_ERROR, IMAGE_TOO_LARGE, IMAGE_INVALID,
    AI_TIMEOUT, AI_RATE_LIMIT, AI_SERVER_ERROR, UNKNOWN
}

data class FortuneError(
    val code: FortuneErrorCode,
    val userMessage: String
)

object FortuneErrorMapper {

    fun classify(functionsCode: String?, detailCode: String?): FortuneErrorCode = when {
        detailCode == "IMAGE_TOO_LARGE" -> FortuneErrorCode.IMAGE_TOO_LARGE
        detailCode == "IMAGE_INVALID" -> FortuneErrorCode.IMAGE_INVALID
        functionsCode == "UNAUTHENTICATED" || functionsCode == "PERMISSION_DENIED" -> FortuneErrorCode.AUTH_ERROR
        functionsCode == "DEADLINE_EXCEEDED" || functionsCode == "CANCELLED" -> FortuneErrorCode.AI_TIMEOUT
        functionsCode == "RESOURCE_EXHAUSTED" -> FortuneErrorCode.AI_RATE_LIMIT
        functionsCode == "UNAVAILABLE" || functionsCode == "INTERNAL" -> FortuneErrorCode.AI_SERVER_ERROR
        else -> FortuneErrorCode.UNKNOWN
    }

    fun userMessage(code: FortuneErrorCode): String = when (code) {
        FortuneErrorCode.NO_NETWORK ->
            "İnternet bağlantısı bulunamadı. Bağlantını kontrol edip tekrar dener misin?"
        FortuneErrorCode.AUTH_ERROR ->
            "Buket şu anda seni tanıyamadı. Uygulamayı yeniden başlatıp tekrar deneyebilirsin."
        FortuneErrorCode.IMAGE_TOO_LARGE ->
            "Fotoğraf çok büyük görünüyor. Kameradan veya galeriden daha küçük bir fotoğraf seçip tekrar dene."
        FortuneErrorCode.IMAGE_INVALID ->
            "Fotoğraf açılamadı. Lütfen fincanın net göründüğü bir fotoğraf seç."
        FortuneErrorCode.AI_TIMEOUT ->
            "Buket biraz fazla düşündü ve yanıt gecikti. Tekrar denersen yaklaşımın daha keskin olacak."
        FortuneErrorCode.AI_RATE_LIMIT ->
            "Bugün çok fazla istek geldi gibi görünüyor. Biraz sonra tekrar denersen Buket hazır olacak."
        FortuneErrorCode.AI_SERVER_ERROR ->
            "Buket şu anda fincanını yorumlayamadı. Fotoğrafların net olduğundan emin olup tekrar deneyebilirsin."
        FortuneErrorCode.UNKNOWN ->
            "Beklenmeyen bir durum oldu. Lütfen tekrar deneyin."
    }

    fun fromThrowable(t: Throwable, networkAvailable: Boolean = true): FortuneError {
        if (!networkAvailable) return FortuneError(FortuneErrorCode.NO_NETWORK, userMessage(FortuneErrorCode.NO_NETWORK))
        val code: String? = (t as? com.google.firebase.functions.FirebaseFunctionsException)?.code?.name
        val detail: String? = (t as? com.google.firebase.functions.FirebaseFunctionsException)?.details?.let {
            (it as? Map<*, *>)?.get("code") as? String
        }
        val classified = classify(code, detail)
        return FortuneError(classified, userMessage(classified))
    }
}
