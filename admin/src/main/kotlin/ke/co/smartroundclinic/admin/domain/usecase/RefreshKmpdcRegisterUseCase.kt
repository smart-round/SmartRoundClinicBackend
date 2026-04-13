package ke.co.smartroundclinic.admin.domain.usecase

import ke.co.smartroundclinic.admin.domain.repository.KmpdcRepository
import ke.co.smartroundclinic.common.DefaultResponse

class RefreshKmpdcRegisterUseCase(private val repository: KmpdcRepository) {
    suspend operator fun invoke(): DefaultResponse<Int?> =
        repository.refreshAll().toDefaultResponse(
            failedStatusCode = 500,
        ) { it }
}
