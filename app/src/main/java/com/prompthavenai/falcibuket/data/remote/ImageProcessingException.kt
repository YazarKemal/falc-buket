package com.prompthavenai.falcibuket.data.remote

/**
 * Yerel görsel işleme hattında oluşan, sınıflandırılmış hata.
 * [code] kullanıcıya gösterilecek mesajı belirler; [stage] ve [slot] yalnızca
 * güvenli teşhis/telemetri içindir ve kullanıcıya gösterilmez.
 */
class ImageProcessingException(
    val code: FortuneErrorCode,
    val stage: String,
    val slot: String? = null,
    cause: Throwable? = null
) : Exception(buildString {
    append(stage)
    slot?.let { append(":").append(it) }
}, cause)
