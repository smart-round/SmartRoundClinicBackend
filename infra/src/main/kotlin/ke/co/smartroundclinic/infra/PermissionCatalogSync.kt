package ke.co.smartroundclinic.infra

import io.ktor.server.application.Application
import io.ktor.server.auth.AuthenticationRouteSelector
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.Route
import io.ktor.util.logging.KtorSimpleLogger
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.infra.data.entity.PermissionCatalogEntry
import ke.co.smartroundclinic.infra.plugins.buildPathTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.Document
import org.koin.core.qualifier.named
import org.koin.ktor.ext.getKoin

private val logger = KtorSimpleLogger("PermissionCatalogSync")

/**
 * Only routes nested inside an authenticate("auth-jwt") block are catalogued.
 * Public endpoints (sign-in, sign-up, health, metrics) are excluded.
 */
private fun Route.isAuthenticated(): Boolean {
    var node: Route? = this
    while (node != null) {
        if (node.selector is AuthenticationRouteSelector) return true
        node = node.parent
    }
    return false
}

fun Application.syncPermissionCatalog(
    routing: Route,
    afterSync: (suspend Application.(List<PermissionCatalogEntry>) -> Unit)? = null,
) {
    val db = getKoin().get<com.mongodb.kotlin.client.coroutine.MongoDatabase>(qualifier = named("adminDb"))
    val catalog = db.getCollection<PermissionCatalogEntry>(MongoDBConstants.ADMIN_PERMISSIONS_CATALOG)

    launch(Dispatchers.IO) {
        try {
            val entries = mutableListOf<PermissionCatalogEntry>()

            fun walk(node: Route) {
                val method = (node.selector as? HttpMethodRouteSelector)?.method?.value
                if (method != null && node.isAuthenticated()) {
                    val path = buildPathTemplate(node)
                    val module = path.trimStart('/').split("/").firstOrNull() ?: "root"
                    entries.add(PermissionCatalogEntry(
                        key = "$method:$path",
                        method = method,
                        path = path,
                        module = module,
                    ))
                }
                node.children.forEach { walk(it) }
            }

            walk(routing)

            catalog.deleteMany(Document())
            if (entries.isNotEmpty()) {
                catalog.insertMany(entries)
            }

            logger.info("Permission catalog synced — ${entries.size} authenticated routes indexed")
            afterSync?.invoke(this@syncPermissionCatalog, entries)
        } catch (e: Exception) {
            logger.error("Permission catalog sync failed: ${e.message}")
        }
    }
}
