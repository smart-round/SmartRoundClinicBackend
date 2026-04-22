package ke.co.smartroundclinic.infra.data.entity

data class PermissionCatalogEntry(
    val key: String,        // "admin:commission-rates:read"
    val module: String,     // "admin"
    val controller: String, // "commission-rates"
    val action: String,     // "read" | "write" | "update" | "delete"
)
