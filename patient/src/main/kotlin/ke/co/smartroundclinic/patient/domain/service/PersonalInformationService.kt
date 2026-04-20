package ke.co.smartroundclinic.patient.domain.service

import ke.co.smartroundclinic.patient.domain.model.PersonalInformation
import ke.co.smartroundclinic.patient.domain.usecase.CreatePersonalInformationUseCase
import ke.co.smartroundclinic.patient.domain.usecase.GetPersonalInformationUseCase
import ke.co.smartroundclinic.patient.domain.usecase.UpdatePersonalInformationUseCase
import ke.co.smartroundclinic.patient.presentation.dto.request.UpdatePersonalInformationReq

class PersonalInformationService(
    private val createUseCase: CreatePersonalInformationUseCase,
    private val getUseCase: GetPersonalInformationUseCase,
    private val updateUseCase: UpdatePersonalInformationUseCase,
) {
    suspend fun create(model: PersonalInformation) = createUseCase(model)
    suspend fun get(patientId: String) = getUseCase(patientId)
    suspend fun update(patientId: String, req: UpdatePersonalInformationReq) = updateUseCase(patientId, req)
}
