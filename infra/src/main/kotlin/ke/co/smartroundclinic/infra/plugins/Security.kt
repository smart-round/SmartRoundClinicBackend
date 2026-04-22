package ke.co.smartroundclinic.infra.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.http.HttpMethod
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.PathSegmentConstantRouteSelector
import io.ktor.server.routing.PathSegmentParameterRouteSelector
import io.ktor.server.routing.PathSegmentTailcardRouteSelector
import io.ktor.server.routing.PathSegmentWildcardRouteSelector
import io.ktor.server.routing.Route
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.EnvLoader
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull

private fun require(key: String): String =
    EnvLoader.get(key) ?: throw IllegalStateException("$key is required")

private const val SUPER_ADMIN_ROLE = "SUPER_ADMIN"
private const val ADMIN_ROLE = "ADMIN"

fun Application.configureSecurity() {
    val jwtAudience = require("JWT_AUDIENCE")
    val jwtDomain = require("JWT_DOMAIN")
    val jwtRealm = require("JWT_REALM")
    val jwtSecret = require("JWT_SECRET")
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
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    Resource.Error(data = null, message = "Token is not valid or has expired")
                        .toDefaultResponse(failedStatusCode = HttpStatusCode.Unauthorized.value) { null }
                )
            }
        }
    }
}

fun ApplicationCall.getRole(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()

// Returns the permissions claim as a list of coarse keys e.g. ["admin:read", "patient:write"].
fun ApplicationCall.getPermissions(): List<String> {
    val raw = principal<JWTPrincipal>()?.payload?.getClaim("permissions")?.asString()
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

// Derives "module:action" from the actual request.
// GET → read, everything else (POST/PUT/PATCH/DELETE) → write.
private fun ApplicationCall.permissionKey(): String {
    val segments = request.path().trimStart('/').split("/")
    val module = segments.getOrNull(0) ?: ""
    val controller = segments.getOrNull(1) ?: module
    val action = when (request.httpMethod) {
        HttpMethod.Get              -> "read"
        HttpMethod.Post             -> "write"
        HttpMethod.Put, HttpMethod.Patch -> "update"
        HttpMethod.Delete           -> "delete"
        else                        -> "write"
    }
    return "$module:$controller:$action"
}

// Role guard.
// - SUPER_ADMIN bypasses all checks.
// - ADMIN: derives module:action from the request, checks against JWT permissions.
// - DOCTOR / PATIENT: role check only, no permission lookup.
suspend fun ApplicationCall.requireRole(vararg allowedRoles: String, block: suspend () -> Unit) {
    val role = getRole()

    if (role == SUPER_ADMIN_ROLE) { block(); return }

    if (role == null || !allowedRoles.contains(role)) {
        respondForbidden(); return
    }

    if (role == ADMIN_ROLE) {
        if (getPermissions().contains(permissionKey())) block() else respondForbidden()
        return
    }

    // DOCTOR, PATIENT — role check is sufficient.
    block()
}

// Explicit coarse permission guard. Keys use "module:action" format e.g. "admin:read".
suspend fun ApplicationCall.requirePermission(vararg keys: String, block: suspend () -> Unit) {
    val role = getRole()
    if (role == SUPER_ADMIN_ROLE) { block(); return }
    val permissions = getPermissions()
    if (keys.any { permissions.contains(it) }) block() else respondForbidden()
}

private suspend fun ApplicationCall.respondForbidden() {
    respond(
        HttpStatusCode.Forbidden,
        Resource.Error<Nothing>(message = "Access denied: insufficient permissions")
            .toDefaultResponse(HttpStatusCode.Forbidden.value) { null }
    )
}

// Walks up the route parent chain to reconstruct the full path template.
// Used by PermissionCatalogSync to derive route keys.
internal fun buildPathTemplate(route: Route): String {
    val segments = mutableListOf<String>()
    var node: Route? = route
    while (node != null) {
        when (val sel = node.selector) {
            is PathSegmentConstantRouteSelector -> segments.add(0, sel.value)
            is PathSegmentParameterRouteSelector -> segments.add(0, "{${sel.name}}")
            is PathSegmentWildcardRouteSelector -> segments.add(0, "*")
            is PathSegmentTailcardRouteSelector -> segments.add(0, "{${sel.name}...}")
            else -> { }
        }
        node = node.parent
    }
    return if (segments.isEmpty()) "/" else "/" + segments.joinToString("/")
}

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
