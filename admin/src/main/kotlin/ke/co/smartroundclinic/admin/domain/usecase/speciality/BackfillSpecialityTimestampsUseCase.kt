package ke.co.smartroundclinic.admin.domain.usecase.speciality

import ke.co.smartroundclinic.admin.domain.repository.SpecialityRepository
import ke.co.smartroundclinic.common.DefaultResponse

class BackfillSpecialityTimestampsUseCase(
    private val repository: SpecialityRepository,
) {
    suspend operator fun invoke(): DefaultResponse<Long?> =
        repository.backfillAllTimestamps().toDefaultResponse { it }
}
