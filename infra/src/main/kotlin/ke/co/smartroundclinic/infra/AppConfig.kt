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
    val accessKeyId: String = require("CLOUDFLARE_R2_ACCESS_KEY_ID"),
    val secretAccessKey: String = require("CLOUDFLARE_R2_SECRET_ACCESS_KEY"),
    val endpointUrl: String = require("CLOUDFLARE_R2_BASE_URL"),
    val bucket: String = require("CLOUDFLARE_R2_BUCKET"),
) {
    init {
        check(accessKeyId.length == 32) {
            "CLOUDFLARE_R2_ACCESS_KEY_ID must be 32 characters (got ${accessKeyId.length}). " +
            "Generate an R2-specific token from Cloudflare Dashboard -> R2 -> Manage R2 API Tokens."
        }
    }
}

data class StaticAssetsConfig(
    val kenyanBanksJsonUrl: String = require("KENYAN_BANKS_JSON_URL"),
)

object AppConfig {
    val mongo = MongoDBConfig()
    val resend = ResendConfig()
    val jwt = JwtConfig()
    val r2 = R2Config()
    val staticAssets = StaticAssetsConfig()
}
