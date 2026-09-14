package com.prompthavenai.falcibuket.data.model

/**
 * Sınırlı OOM kurtarma politikası: normal deneme → OutOfMemoryError →
 * TEK düşük-çözünürlük denemesi → yine OOM ise kontrollü başarısızlık.
 *
 * Döngü yoktur; yalnızca OOM yeniden-denemeyi tetikler. Diğer hatalar
 * çağıranın (deneme gövdesinin) sorumluluğundadır.
 */
object CoffeeAtlasRetryPolicy {

    const val EVENT_OOM_RETRY = "ATLAS_OOM_RETRY"
    const val EVENT_OOM_FINAL_FAILURE = "ATLAS_OOM_FINAL_FAILURE"

    enum class Outcome { NORMAL_OK, RETRY_OK, FINAL_FAILURE }

    class Run<T : Any>(val outcome: Outcome, val value: T?)

    fun <T : Any> run(normal: () -> T, retry: () -> T, onEvent: (String) -> Unit): Run<T> {
        val first = attempt(normal)
        if (first != null) return Run(Outcome.NORMAL_OK, first)
        onEvent(EVENT_OOM_RETRY)
        val second = attempt(retry)
        if (second != null) return Run(Outcome.RETRY_OK, second)
        onEvent(EVENT_OOM_FINAL_FAILURE)
        return Run(Outcome.FINAL_FAILURE, null)
    }

    private fun <T : Any> attempt(block: () -> T): T? =
        try {
            block()
        } catch (e: OutOfMemoryError) {
            null
        }
}
