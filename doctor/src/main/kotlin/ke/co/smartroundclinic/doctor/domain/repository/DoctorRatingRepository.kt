package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.DoctorRatingEntity

interface DoctorRatingRepository {
    suspend fun add(entity: DoctorRatingEntity): Resource<DoctorRatingEntity>
    suspend fun update(id: String, patientId: String, rating: Int?, comment: String?): Resource<DoctorRatingEntity?>
    suspend fun delete(id: String, patientId: String): Resource<Boolean>
    suspend fun getById(id: String): Resource<DoctorRatingEntity?>
    suspend fun getByDoctorId(doctorId: String, page: Int, size: Int): Resource<Pair<List<DoctorRatingEntity>, Long>>
    suspend fun getAverageRating(doctorId: String): Resource<Pair<Double, Int>>
}
