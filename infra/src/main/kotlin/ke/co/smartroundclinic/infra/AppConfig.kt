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

data class R2Config(
    // R2 S3-compatible Access Key ID (32 chars) — generate from Cloudflare Dashboard → R2 → Manage R2 API Tokens.
    // This is NOT the same as a general Cloudflare API token (cfat_...).
    val accessKeyId: String = require("CLOUDFLARE_R2_ACCESS_KEY_ID"),
    val secretAccessKey: String = require("CLOUDFLARE_R2_SECRET_ACCESS_KEY"),
    // Used both as the S3-compatible API endpoint and as the base for public object URLs.
    val endpointUrl: String = require("CLOUDFLARE_R2_BASE_URL"),
    val customDomain: String = require("CLOUDFLARE_R2_CUSTOM_DOMAIN")
) {
    init {
        check(accessKeyId.length == 32) {
            "CLOUDFLARE_R2_ACCESS_KEY_ID must be 32 characters (got ${accessKeyId.length}). " +
            "Generate an R2-specific token from Cloudflare Dashboard → R2 → Manage R2 API Tokens."
        }
    }
}

object AppConfig {
    val mongo = MongoDBConfig()
    val resend = ResendConfig()
    val jwt = JwtConfig()
    val r2 = R2Config()
}