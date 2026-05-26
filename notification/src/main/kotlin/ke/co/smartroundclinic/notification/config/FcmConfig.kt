package ke.co.smartroundclinic.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import ke.co.smartroundclinic.infra.EnvLoader
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

class FcmConfig {
    private val logger = LoggerFactory.getLogger(FcmConfig::class.java)

    val messaging: FirebaseMessaging? by lazy {
        val json = EnvLoader.get("FCM_SERVICE_ACCOUNT_JSON")
        if (json == null) {
            logger.warn("FCM_SERVICE_ACCOUNT_JSON not set — push notifications disabled")
            return@lazy null
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                val stream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
                val credentials = GoogleCredentials.fromStream(stream)
                    .createScoped("https://www.googleapis.com/auth/firebase.messaging")
                FirebaseApp.initializeApp(
                    FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build()
                )
            }
            FirebaseMessaging.getInstance()
        } catch (e: Exception) {
            logger.error("FCM initialization failed — push notifications disabled: ${e.message}")
            null
        }
    }
}
