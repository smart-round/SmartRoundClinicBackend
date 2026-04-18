package ke.co.smartroundclinic.admin.domain.usecase.kmpdc

import ke.co.smartroundclinic.admin.domain.repository.KmpdcRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.KmpdcRefreshRes
import ke.co.smartroundclinic.common.DefaultResponse

class RefreshKmpdcRegisterUseCase(private val repository: KmpdcRepository) {
    suspend operator fun invoke(): DefaultResponse<KmpdcRefreshRes?> =
        repository.refreshAll().toDefaultResponse(failedStatusCode = 500) { summary ->
            summary?.let {
                KmpdcRefreshRes(
                    newRecords     = it.newRecords,
                    totalScraped   = it.totalScraped,
                    failedRegisters = it.failedRegisters,
                )
            }
        }
}
