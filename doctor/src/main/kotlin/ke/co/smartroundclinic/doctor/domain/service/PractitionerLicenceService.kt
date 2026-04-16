package ke.co.smartroundclinic.doctor.domain.service

import ke.co.smartroundclinic.doctor.domain.usecase.licence.AddLicenceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.DeleteLicenceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.GetLicenceByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.GetMyLicencesUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.licence.UpdateLicenceUseCase
import ke.co.smartroundclinic.doctor.domain.model.PractitionerLicence

class PractitionerLicenceService(
    private val addUseCase: AddLicenceUseCase,
    private val updateUseCase: UpdateLicenceUseCase,
    private val deleteUseCase: DeleteLicenceUseCase,
    private val getMyLicencesUseCase: GetMyLicencesUseCase,
    private val getByIdUseCase: GetLicenceByIdUseCase,
) {
    suspend fun add(contentType:String, licence: ByteArray, model: PractitionerLicence) =
        addUseCase(contentType = contentType, licence = licence, model = model)
    suspend fun update(id: String, doctorId: String, licenceName: String?) =
        updateUseCase(id, doctorId, licenceName)
    suspend fun delete(id: String, doctorId: String) = deleteUseCase(id, doctorId)
    suspend fun getAll(doctorId: String) = getMyLicencesUseCase(doctorId)
    suspend fun getById(id: String, doctorId: String) = getByIdUseCase(id, doctorId)
}