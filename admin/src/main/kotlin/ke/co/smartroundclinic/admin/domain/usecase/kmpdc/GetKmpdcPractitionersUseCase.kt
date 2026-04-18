package ke.co.smartroundclinic.admin.domain.usecase.kmpdc

import ke.co.smartroundclinic.admin.domain.model.DoctorLicenceStatus
import ke.co.smartroundclinic.admin.domain.model.KmpdcRegisterType
import ke.co.smartroundclinic.admin.domain.repository.KmpdcRepository
import ke.co.smartroundclinic.admin.presentation.dto.response.KmpdcPageResult
import ke.co.smartroundclinic.admin.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse
import kotlin.math.ceil

class GetKmpdcPractitionersUseCase(private val repository: KmpdcRepository) {
    suspend operator fun invoke(
        registerType: KmpdcRegisterType?,
        status: DoctorLicenceStatus?,
        page: Int,
        size: Int,
    ): DefaultResponse<KmpdcPageResult?> =
        repository.getPractitioners(registerType, status, page, size)
            .toDefaultResponse { pair ->
                pair?.let { (items, total) ->
                    KmpdcPageResult(
                        items = items.map { it.toRes() },
                        total = total,
                        page = page,
                        size = size,
                        pages = ceil(total.toDouble() / size).toLong(),
                    )
                }
            }
}
