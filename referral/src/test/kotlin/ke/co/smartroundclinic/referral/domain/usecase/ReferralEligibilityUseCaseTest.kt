package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.data.entity.AppointmentEntity
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReferralEligibilityUseCaseTest {

    // ── Test doubles ─────────────────────────────────────────────────────────

    private class FakeAppointmentRepository(private val appointment: AppointmentEntity?) : AppointmentRepository {
        override suspend fun book(entity: AppointmentEntity) = Resource.Success<AppointmentEntity?>(null)
        override suspend fun getById(id: String) = Resource.Success<AppointmentEntity?>(appointment)
        override suspend fun getAll() = Resource.Success(emptyList<AppointmentEntity>())
        override suspend fun getAllForAdmin(status: String?, page: Int, size: Int) = Resource.Success(emptyList<AppointmentEntity>() to 0L)
        override suspend fun getByPatient(patientId: String) = Resource.Success(emptyList<AppointmentEntity>())
        override suspend fun getByDoctor(doctorId: String) = Resource.Success(emptyList<AppointmentEntity>())
        override suspend fun getByDoctorFiltered(doctorId: String, filter: String?, today: String) = Resource.Success(emptyList<AppointmentEntity>())
        override suspend fun getByDoctorAndDate(doctorId: String, date: String) = Resource.Success(emptyList<AppointmentEntity>())
        override suspend fun getByDoctorAndDateRange(doctorId: String, from: String, to: String) = Resource.Success(emptyList<AppointmentEntity>())
        override suspend fun updateStatus(id: String, status: String, cancellationReason: String?, cancelledBy: String?) = Resource.Success<AppointmentEntity?>(null)
        override fun watchByDoctorId(doctorId: String): Flow<AppointmentEntity> = emptyFlow()
        override suspend fun existsConfirmedOrCompletedBetween(doctorId: String, patientId: String) = false
        override suspend fun hasJoinableConfirmedAppointment(doctorId: String, patientId: String) = false
        override suspend fun getNextConfirmedAppointment(doctorId: String, patientId: String) = Resource.Success<AppointmentEntity?>(null)
        override suspend fun setReferralId(appointmentId: String, referralId: String) = Resource.Success(true)
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private fun appointment(status: String, doctorId: String = "doc-1") = AppointmentEntity(
        id = "appt-1", doctorId = doctorId, patientId = "pat-1", date = "2026-01-01",
        slotStart = "09:00", slotEnd = "09:30", status = status, bookedAt = "2026-01-01T00:00:00.000Z",
    )

    private fun useCase(status: String, appointmentDoctorId: String = "doc-1") =
        ReferralEligibilityUseCase(
            appointmentRepository = FakeAppointmentRepository(appointment(status, appointmentDoctorId)),
        )

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `not eligible when appointment is not completed`(): Unit = runBlocking {
        val result = useCase(status = "CONFIRMED")("appt-1", "doc-1")

        assertFalse(result.data?.eligible ?: true)
        assertEquals(listOf("Appointment must be marked complete"), result.data?.reasons)
    }

    @Test
    fun `eligible once completed`(): Unit = runBlocking {
        val result = useCase(status = "COMPLETED")("appt-1", "doc-1")

        assertTrue(result.data?.eligible ?: false)
        assertTrue(result.data?.reasons.isNullOrEmpty())
    }

    @Test
    fun `rejects when caller is not the appointment's doctor`(): Unit = runBlocking {
        val result = useCase(status = "COMPLETED", appointmentDoctorId = "doc-1")("appt-1", "doc-2")

        assertEquals(403, result.httpStatusCode)
    }
}
