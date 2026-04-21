package ke.co.smartroundclinic.infra

import io.ktor.server.application.Application
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
 * Walks the fully-mounted Ktor routing tree (passed in after all routes are registered),
 * derives a permission key per leaf route in the format "METHOD:/path/{param}", then
 * replaces the permissions_catalog collection in MongoDB with the current set.
 *
 * Call from Application.module() after all feature modules are mounted, passing the
 * return value of routing { }.
 *
 * On every restart after adding a route, the new key automatically appears in the catalog.
 */
fun Application.syncPermissionCatalog(routing: Route) {
    val db = getKoin().get<com.mongodb.kotlin.client.coroutine.MongoDatabase>(qualifier = named("adminDb"))
    val catalog = db.getCollection<PermissionCatalogEntry>(MongoDBConstants.ADMIN_PERMISSIONS_CATALOG)

    launch(Dispatchers.IO) {
        try {
            val entries = mutableListOf<PermissionCatalogEntry>()

            fun walk(node: Route) {
                val method = (node.selector as? HttpMethodRouteSelector)?.method?.value
                if (method != null) {
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

            // Replace entire catalog so removed routes don't linger between deployments.
            catalog.deleteMany(Document())
            if (entries.isNotEmpty()) {
                catalog.insertMany(entries)
            }

            logger.info("Permission catalog synced — ${entries.size} routes indexed")
        } catch (e: Exception) {
            logger.error("Permission catalog sync failed: ${e.message}")
        }
    }
}
