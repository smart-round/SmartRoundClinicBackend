package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.SpecializationEntity

interface SpecializationRepository {
    suspend fun add(entity: SpecializationEntity): Resource<SpecializationEntity>
    suspend fun remove(id: String, doctorId: String): Resource<Boolean>
    suspend fun getByDoctorId(doctorId: String): Resource<List<SpecializationEntity>>
}
