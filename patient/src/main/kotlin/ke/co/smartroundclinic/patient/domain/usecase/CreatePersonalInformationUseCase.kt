package ke.co.smartroundclinic.patient.domain.usecase

import ke.co.smartroundclinic.patient.data.entity.toEntity
import ke.co.smartroundclinic.patient.domain.model.PersonalInformation
import ke.co.smartroundclinic.patient.domain.repository.PersonalInformationRepository
import ke.co.smartroundclinic.patient.presentation.dto.response.PersonalInformationRes
import ke.co.smartroundclinic.patient.presentation.dto.response.toRes
import ke.co.smartroundclinic.common.DefaultResponse

class CreatePersonalInformationUseCase(private val repository: PersonalInformationRepository) {
    suspend operator fun invoke(model: PersonalInformation): DefaultResponse<PersonalInformationRes?> =
        repository.create(model.toEntity())
            .toDefaultResponse(successStatusCode = 201, failedStatusCode = 409) { it?.toModel()?.toRes() }
}
