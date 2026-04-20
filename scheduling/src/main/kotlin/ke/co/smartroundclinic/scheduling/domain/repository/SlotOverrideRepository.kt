package ke.co.smartroundclinic.scheduling.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.SlotOverrideEntity

interface SlotOverrideRepository {
    suspend fun create(entity: SlotOverrideEntity): Resource<SlotOverrideEntity?>
    suspend fun getByDoctorAndDate(doctorId: String, date: String): Resource<List<SlotOverrideEntity>>
    suspend fun getByDoctorAndDateRange(doctorId: String, from: String, to: String): Resource<List<SlotOverrideEntity>>
    suspend fun delete(id: String): Resource<Nothing?>
}
