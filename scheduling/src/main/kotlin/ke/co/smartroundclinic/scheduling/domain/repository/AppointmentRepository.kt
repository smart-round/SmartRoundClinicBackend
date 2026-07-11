package ke.co.smartroundclinic.scheduling.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

interface AppointmentRepository {
    suspend fun book(entity: AppointmentEntity): Resource<AppointmentEntity?>
    suspend fun getById(id: String): Resource<AppointmentEntity?>
    suspend fun getAll(): Resource<List<AppointmentEntity>>
    suspend fun getAllForAdmin(status: String? = null, page: Int = 1, size: Int = 20): Resource<Pair<List<AppointmentEntity>, Long>>
    suspend fun getByPatient(patientId: String): Resource<List<AppointmentEntity>>
    suspend fun getByDoctor(doctorId: String): Resource<List<AppointmentEntity>>
    suspend fun getByDoctorFiltered(doctorId: String, filter: String?, today: String): Resource<List<AppointmentEntity>>
    suspend fun getByDoctorAndDate(doctorId: String, date: String): Resource<List<AppointmentEntity>>
    suspend fun getByDoctorAndDateRange(doctorId: String, from: String, to: String): Resource<List<AppointmentEntity>>
    suspend fun updateStatus(
        id: String,
        status: String,
        cancellationReason: String? = null,
        cancelledBy: String? = null,
    ): Resource<AppointmentEntity?>
    fun watchByDoctorId(doctorId: String): Flow<AppointmentEntity>

    /** True once this doctor/patient pair has ever had a CONFIRMED or COMPLETED appointment — gates chat/call access. */
    suspend fun existsConfirmedOrCompletedBetween(doctorId: String, patientId: String): Boolean

    /** True if a CONFIRMED appointment between this pair has entered its joinable window (from 10 min before slotStart onward, no upper bound) — gates video call join. */
    suspend fun hasJoinableConfirmedAppointment(doctorId: String, patientId: String): Boolean
}
