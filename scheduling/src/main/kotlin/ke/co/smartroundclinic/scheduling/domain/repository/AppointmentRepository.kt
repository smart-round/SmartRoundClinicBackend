package ke.co.smartroundclinic.scheduling.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

interface AppointmentRepository {
    suspend fun book(entity: AppointmentEntity): Resource<AppointmentEntity?>
    suspend fun getById(id: String): Resource<AppointmentEntity?>
    suspend fun getByPatient(patientId: String): Resource<List<AppointmentEntity>>
    suspend fun getByDoctorAndDate(doctorId: String, date: String): Resource<List<AppointmentEntity>>
    suspend fun getByDoctorAndDateRange(doctorId: String, from: String, to: String): Resource<List<AppointmentEntity>>
    suspend fun updateStatus(
        id: String,
        status: String,
        cancellationReason: String? = null,
        cancelledBy: String? = null,
    ): Resource<AppointmentEntity?>
    fun watchByDoctorId(doctorId: String): Flow<AppointmentEntity>
}
