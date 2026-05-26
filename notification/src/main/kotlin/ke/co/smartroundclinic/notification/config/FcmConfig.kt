package ke.co.smartroundclinic.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import ke.co.smartroundclinic.infra.EnvLoader
import java.io.FileInputStream

class FcmConfig {
    val messaging: FirebaseMessaging by lazy {
        if (FirebaseApp.getApps().isEmpty()) {
            val path = EnvLoader.get("FCM_SERVICE_ACCOUNT_PATH") ?: "smart-round-clinic-service-account-key.json"
            val credentials = GoogleCredentials.fromStream(FileInputStream(path))
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
