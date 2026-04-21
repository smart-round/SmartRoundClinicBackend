package ke.co.smartroundclinic.admin.seeder

import io.ktor.server.application.Application
import io.ktor.util.logging.KtorSimpleLogger
import ke.co.smartroundclinic.admin.domain.repository.PolicyGroupRepository
import ke.co.smartroundclinic.infra.data.entity.PermissionCatalogEntry
import org.koin.ktor.ext.getKoin

private val logger = KtorSimpleLogger("DefaultPolicyGroupSeeder")

private fun moduleToGroupName(module: String): String =
    module.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) } + " Management"

suspend fun Application.seedDefaultPolicyGroups(catalog: List<PermissionCatalogEntry>) {
    val repository = getKoin().get<PolicyGroupRepository>()
    val groupsByModule = catalog.groupBy { it.module }
    var seeded = 0
    groupsByModule.forEach { (module, entries) ->
        try {
            repository.upsertByName(
                name = moduleToGroupName(module),
                description = "Access to $module resources",
                permissions = entries.map { it.key },
                createdBy = "system",
            )
            seeded++
        } catch (e: Exception) {
            logger.error("Failed to seed policy group for module '$module': ${e.message}")
        }
    }
    logger.info("Default policy groups seeded — $seeded/${groupsByModule.size} modules synced")
}
