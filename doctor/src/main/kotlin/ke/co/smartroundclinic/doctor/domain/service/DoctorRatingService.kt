package ke.co.smartroundclinic.doctor.domain.service

import ke.co.smartroundclinic.doctor.domain.model.DoctorRating
import ke.co.smartroundclinic.doctor.domain.usecase.rating.DeleteRatingUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.rating.GetDoctorRatingsUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.rating.GetRatingByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.rating.SubmitRatingUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.rating.UpdateRatingUseCase

class DoctorRatingService(
    private val submitUseCase: SubmitRatingUseCase,
    private val updateUseCase: UpdateRatingUseCase,
    private val deleteUseCase: DeleteRatingUseCase,
    private val getDoctorRatingsUseCase: GetDoctorRatingsUseCase,
    private val getByIdUseCase: GetRatingByIdUseCase,
) {
    suspend fun submit(model: DoctorRating) = submitUseCase(model)
    suspend fun update(id: String, patientId: String, rating: Int?, comment: String?) =
        updateUseCase(id, patientId, rating, comment)
    suspend fun delete(id: String, patientId: String) = deleteUseCase(id, patientId)
    suspend fun getByDoctorId(doctorId: String, page: Int, size: Int) =
        getDoctorRatingsUseCase(doctorId, page, size)
    suspend fun getById(id: String) = getByIdUseCase(id)
}
