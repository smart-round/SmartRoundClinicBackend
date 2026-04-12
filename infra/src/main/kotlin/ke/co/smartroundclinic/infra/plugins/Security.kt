package ke.co.smartroundclinic.infra.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.http.*
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull

fun Application.configureSecurity() {
    // Read JWT credentials from environment variables
    val jwtAudience = System.getenv("JWT_AUDIENCE")?.trim() ?: "jwt-audience"
    val jwtDomain = System.getenv("JWT_DOMAIN")?.trim() ?: "https://jwt-provider-domain/"
    val jwtRealm = System.getenv("JWT_REALM")?.trim() ?: "ktor sample app"
    val jwtSecret = System.getenv("JWT_SECRET")?.trim() ?: "secret"
    authentication {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                val role = credential.payload.getClaim("role")?.asString()
                if (credential.payload.audience.contains(jwtAudience) && role != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { defaultScheme, realm ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    Resource.Error(data = null, message = "Token is not valid or has expired")
                        .toDefaultResponse(failedStatusCode = HttpStatusCode.Unauthorized.value){null}
                )
            }
        }
    }
}


// Helper extension to extract role from token
fun ApplicationCall.getRole(): String? {
    return principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
}

// Reusable guard function
suspend fun ApplicationCall.requireRole(vararg allowedRoles: String, block: suspend () -> Unit) {
    val role = getRole()
    if (role != null && allowedRoles.contains(role)) {
        block()
    } else {
        respond(
            HttpStatusCode.Forbidden,
            Resource.Error<Nothing>(message = "Access denied: insufficient permissions")
                .toDefaultResponse(HttpStatusCode.Forbidden.value){null}
        )
    }
}

/**
 * Validates that [contentType] is a supported image format (PNG, JPEG, WebP).
 * Throws [UnsupportedFileFormatException] — caught by StatusPages — if not.
 * Returns the resolved file extension on success.
 */
fun requireImageContentType(contentType: String): String =
    imageExtensionOrNull(contentType) ?: throw UnsupportedFileFormatException(contentType)

suspend fun ApplicationCall.getUserId(): String? {
    val userId = principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
    if (userId == null) {
        respond(
            status = HttpStatusCode.Unauthorized,
            message = Resource.Error(data = null, message = "Token is not valid or has expired")
                .toDefaultResponse(failedStatusCode = HttpStatusCode.Unauthorized.value) { null }
        )
    }
    return userId
}