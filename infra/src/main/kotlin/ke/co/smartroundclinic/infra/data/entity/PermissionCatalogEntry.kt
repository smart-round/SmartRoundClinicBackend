package ke.co.smartroundclinic.infra.data.entity

data class PermissionCatalogEntry(
    val key: String,
    val method: String,
    val path: String,
    val module: String,
)
