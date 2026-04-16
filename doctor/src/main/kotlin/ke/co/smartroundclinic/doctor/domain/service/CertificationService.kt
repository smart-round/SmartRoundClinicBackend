package ke.co.smartroundclinic.doctor.domain.service

import ke.co.smartroundclinic.doctor.domain.model.PractitionerCertification
import ke.co.smartroundclinic.doctor.domain.usecase.certification.AddCertificationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.DeleteCertificationUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.GetCertificationByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.GetMyCertificationsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.certification.UpdateCertificationUseCase

class CertificationService(
    private val addUseCase: AddCertificationUseCase,
    private val updateUseCase: UpdateCertificationUseCase,
    private val deleteUseCase: DeleteCertificationUseCase,
    private val getMyUseCase: GetMyCertificationsUseCase,
    private val getByIdUseCase: GetCertificationByIdUseCase,
) {
    suspend fun add(contentType:String, licence: ByteArray, model: PractitionerCertification) =
        addUseCase(contentType = contentType, licence = licence, model =model)
    suspend fun update(id: String, doctorId: String, name: String?, date: String?) =
        updateUseCase(id, doctorId, name, date)
    suspend fun delete(id: String, doctorId: String) = deleteUseCase(id, doctorId)
    suspend fun getAll(doctorId: String) = getMyUseCase(doctorId)
    suspend fun getById(id: String, doctorId: String) = getByIdUseCase(id, doctorId)
}