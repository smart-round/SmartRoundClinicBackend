package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.SpecializationEntity
import ke.co.smartroundclinic.doctor.presentation.dto.response.SpecializationWithDetailsRes

interface SpecializationRepository {
    suspend fun add(entity: SpecializationEntity): Resource<SpecializationEntity>
    suspend fun remove(id: String, doctorId: String): Resource<Boolean>
    suspend fun getByDoctorId(doctorId: String): Resource<List<SpecializationEntity>>
    suspend fun update(id: String, doctorId: String, specializationId: String, subSpecializationId: String?): Resource<SpecializationEntity?>
    suspend fun getByDoctorIdWithDetails(doctorId: String): Resource<List<SpecializationWithDetailsRes>>
}
