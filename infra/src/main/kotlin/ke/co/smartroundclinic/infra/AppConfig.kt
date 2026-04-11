package ke.co.smartroundclinic.infra

private fun require(key: String): String =
    EnvLoader.get(key) ?: throw IllegalStateException("$key is required")

data class MongoDBConfig(
    val user: String = require("MONGODB_USER"),
    val password: String = require("MONGODB_PASSWORD"),
    val host: String = require("MONGODB_HOST"),
    val port: String? = EnvLoader.get("MONGODB_PORT")
) {
    val connectionString: String by lazy {
        val protocol = if (host.contains("mongodb.net")) "mongodb+srv" else "mongodb"
        if (protocol == "mongodb+srv") {
            "$protocol://$user:$password@$host"
        } else {
            val portPart = if (port.isNullOrBlank()) "" else ":$port"
            "$protocol://$user:$password@$host$portPart"
        }
    }
}

data class ResendConfig(
    val baseUrl: String = require("RESEND_BASE_URL"),
    val apiKey: String = require("RESEND_API_KEY"),
    val onboardingEmail: String = require("RESEND_ONBOARDING_EMAIL"),
    val onboardingTemplateId: String = require("RESEND_ONBOARDING_TEMPLATE_ID")
)

data class JwtConfig(
    val secret: String = require("JWT_SECRET"),
    val refreshSecret: String = require("REFRESH_SECRET"),
    val issuer: String = require("JWT_DOMAIN"),
    val audience: String = require("JWT_AUDIENCE")
)

object AppConfig {
    val mongo = MongoDBConfig()
    val resend = ResendConfig()
    val jwt = JwtConfig()
}