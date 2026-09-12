package com.prompthavenai.falcibuket.data.remote

/**
 * Single source of truth for the FalcıBuket backend endpoints.
 *
 * The Cloud Functions in the `functions` module are deployed to
 * [FUNCTIONS_REGION]. Android must resolve callables through the same region
 * explicitly; the SDK default is `us-central1` and would fail.
 */
object BackendConfig {
    const val FUNCTIONS_REGION = "europe-west1"

    /**
     * Coffee vision callable zaman aşımı. Hiyerarşi:
     * provider (150 sn) < function (180 sn) < bu istemci değeri (210 sn).
     */
    const val COFFEE_CALLABLE_TIMEOUT_SECONDS = 210L
}
