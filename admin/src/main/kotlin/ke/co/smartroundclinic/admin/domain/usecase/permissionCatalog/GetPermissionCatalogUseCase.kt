package ke.co.smartroundclinic.admin.domain.usecase.permissionCatalog

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.admin.domain.repository.PermissionCatalogRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.data.entity.PermissionCatalogEntry

class GetPermissionCatalogUseCase(private val repository: PermissionCatalogRepository) {
    suspend operator fun invoke(): DefaultResponse<List<PermissionCatalogEntry>?> =
        when (val result = repository.getAll()) {
            is Resource.Success -> result.toDefaultResponse(HttpStatusCode.OK.value) { it ?: emptyList() }
            is Resource.Error -> result.toDefaultResponse(HttpStatusCode.InternalServerError.value) { null }
        }
}
