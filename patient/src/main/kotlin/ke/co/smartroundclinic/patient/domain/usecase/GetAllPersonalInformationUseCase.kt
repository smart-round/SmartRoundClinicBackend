package ke.co.smartroundclinic.patient.domain.usecase

import ke.co.smartroundclinic.patient.domain.repository.PersonalInformationRepository
import ke.co.smartroundclinic.patient.presentation.dto.response.PersonalInformationRes
import ke.co.smartroundclinic.patient.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class GetAllPersonalInformationUseCase(private val repository: PersonalInformationRepository) {
    suspend operator fun invoke(): DefaultResponse<List<PersonalInformationRes>?> =
        repository.getAll()
            .toDefaultResponse(failedStatusCode = 404) { list -> list?.map { it.toModel().toRes() } }
}
