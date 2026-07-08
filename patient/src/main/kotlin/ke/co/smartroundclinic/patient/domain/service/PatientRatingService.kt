package ke.co.smartroundclinic.patient.domain.service

import ke.co.smartroundclinic.patient.domain.model.PatientRating
import ke.co.smartroundclinic.patient.domain.usecase.rating.DeletePatientRatingUseCase
import ke.co.smartroundclinic.patient.domain.usecase.rating.GetPatientRatingByIdUseCase
import ke.co.smartroundclinic.patient.domain.usecase.rating.GetPatientRatingsUseCase
import ke.co.smartroundclinic.patient.domain.usecase.rating.SubmitPatientRatingUseCase
import ke.co.smartroundclinic.patient.domain.usecase.rating.UpdatePatientRatingUseCase

class PatientRatingService(
    private val submitUseCase: SubmitPatientRatingUseCase,
    private val updateUseCase: UpdatePatientRatingUseCase,
    private val deleteUseCase: DeletePatientRatingUseCase,
    private val getPatientRatingsUseCase: GetPatientRatingsUseCase,
    private val getByIdUseCase: GetPatientRatingByIdUseCase,
) {
    suspend fun submit(model: PatientRating) = submitUseCase(model)
    suspend fun update(id: String, doctorId: String, rating: Int?, comment: String?) =
        updateUseCase(id, doctorId, rating, comment)
    suspend fun delete(id: String, doctorId: String) = deleteUseCase(id, doctorId)
    suspend fun getByPatientId(patientId: String, page: Int, size: Int) =
        getPatientRatingsUseCase(patientId, page, size)
    suspend fun getById(id: String) = getByIdUseCase(id)
}
