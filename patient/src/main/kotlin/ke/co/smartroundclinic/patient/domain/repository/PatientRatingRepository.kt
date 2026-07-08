package ke.co.smartroundclinic.patient.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.patient.data.entity.PatientRatingEntity

interface PatientRatingRepository {
    suspend fun add(entity: PatientRatingEntity): Resource<PatientRatingEntity>
    suspend fun update(id: String, doctorId: String, rating: Int?, comment: String?): Resource<PatientRatingEntity?>
    suspend fun delete(id: String, doctorId: String): Resource<Boolean>
    suspend fun getById(id: String): Resource<PatientRatingEntity?>
    suspend fun getByPatientId(patientId: String, page: Int, size: Int): Resource<Pair<List<PatientRatingEntity>, Long>>
    suspend fun getAverageRating(patientId: String): Resource<Pair<Double, Int>>
}
