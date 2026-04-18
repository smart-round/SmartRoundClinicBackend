package ke.co.smartroundclinic.scheduling.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.DoctorScheduleEntity
import ke.co.smartroundclinic.scheduling.domain.model.BreakBlock

interface DoctorScheduleRepository {
    suspend fun upsert(entity: DoctorScheduleEntity): Resource<DoctorScheduleEntity?>
    suspend fun getByDoctorAndDay(doctorId: String, dayOfWeek: Int): Resource<DoctorScheduleEntity?>
    suspend fun getByDoctor(doctorId: String): Resource<List<DoctorScheduleEntity>>
    suspend fun update(
        doctorId: String,
        dayOfWeek: Int,
        windowStart: String?,
        windowEnd: String?,
        slotDuration: Int?,
        breakBlocks: List<BreakBlock>?,
        isActive: Boolean?,
        timezone: String?,
    ): Resource<DoctorScheduleEntity?>
    suspend fun deactivate(doctorId: String, dayOfWeek: Int): Resource<DoctorScheduleEntity?>
}
