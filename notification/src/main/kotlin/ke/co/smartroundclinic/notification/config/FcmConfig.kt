package ke.co.smartroundclinic.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import ke.co.smartroundclinic.infra.EnvLoader
import java.io.ByteArrayInputStream
import java.io.FileInputStream

class FcmConfig {
    val messaging: FirebaseMessaging by lazy {
        if (FirebaseApp.getApps().isEmpty()) {
            // Prefer inline JSON from env var (for cloud deployments like Dokploy)
            // Fall back to a file path for local development
            val json = EnvLoader.get("FCM_SERVICE_ACCOUNT_JSON")
            val stream = if (!json.isNullOrBlank()) {
                ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
            } else {
                val path = EnvLoader.get("FCM_SERVICE_ACCOUNT_PATH") ?: "smart-round-clinic-service-account-key.json"
                FileInputStream(path)
            }
            val credentials = GoogleCredentials.fromStream(stream)
                .createScoped("https://www.googleapis.com/auth/firebase.messaging")
            FirebaseApp.initializeApp(
                FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build()
            )
        }
        FirebaseMessaging.getInstance()
    }
}
