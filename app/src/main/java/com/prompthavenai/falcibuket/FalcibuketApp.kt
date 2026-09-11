package com.prompthavenai.falcibuket

import android.app.Application
import com.google.firebase.FirebaseApp

class FalcibuketApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // google-services.json yoksa initializeApp başarısız olur; uygulama
        // görsel MVP modunda çalışmaya devam eder, backend çağrıları AUTH_ERROR verir.
        runCatching { FirebaseApp.initializeApp(this) }
    }
}
