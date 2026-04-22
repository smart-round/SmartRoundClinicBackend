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

private fun Route.isAuthenticated(): Boolean {
    var node: Route? = this
    while (node != null) {
        if (node.selector is AuthenticationRouteSelector) return true
        node = node.parent
    }
    return false
}

private fun methodToAction(method: String) = when (method) {
    "GET"           -> "read"
    "POST"          -> "write"
    "PUT", "PATCH"  -> "update"
    "DELETE"        -> "delete"
    else            -> "write"
}

fun Application.syncPermissionCatalog(
    routing: Route,
    afterSync: (suspend Application.(List<PermissionCatalogEntry>) -> Unit)? = null,
) {
    val db = getKoin().get<com.mongodb.kotlin.client.coroutine.MongoDatabase>(qualifier = named("adminDb"))
    val catalog = db.getCollection<PermissionCatalogEntry>(MongoDBConstants.ADMIN_PERMISSIONS_CATALOG)

    launch(Dispatchers.IO) {
        try {
            val seen = mutableSetOf<String>()
            val entries = mutableListOf<PermissionCatalogEntry>()

            fun walk(node: Route) {
                val method = (node.selector as? HttpMethodRouteSelector)?.method?.value
                if (method != null && node.isAuthenticated()) {
                    val path = buildPathTemplate(node)
                    val segments = path.trimStart('/').split("/")
                    val module = segments.getOrNull(0) ?: "root"
                    val controller = segments.getOrNull(1) ?: module
                    val action = methodToAction(method)
                    val key = "$module:$controller:$action"
                    if (seen.add(key)) {
                        entries.add(PermissionCatalogEntry(key = key, module = module, controller = controller, action = action))
                    }
                }
                node.children.forEach { walk(it) }
            }

            walk(routing)

            catalog.deleteMany(Document())
            if (entries.isNotEmpty()) {
                catalog.insertMany(entries)
            }

            logger.info("Permission catalog synced — ${entries.size} coarse permissions indexed")
            afterSync?.invoke(this@syncPermissionCatalog, entries)
        } catch (e: Exception) {
            logger.error("Permission catalog sync failed: ${e.message}")
        }
    }
}
