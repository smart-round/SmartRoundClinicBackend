package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.DoctorRatingEntity
import ke.co.smartroundclinic.doctor.domain.repository.DoctorRatingRepository
import ke.co.smartroundclinic.medicalrecords.data.entity.MedicalRecordEntity
import ke.co.smartroundclinic.medicalrecords.data.entity.PrescriptionItemEntity
import ke.co.smartroundclinic.medicalrecords.domain.repository.MedicalRecordRepository
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

    private class FakeDoctorRatingRepository(private val rating: DoctorRatingEntity?) : DoctorRatingRepository {
        override suspend fun add(entity: DoctorRatingEntity) = Resource.Success(entity)
        override suspend fun update(id: String, patientId: String, rating: Int?, comment: String?) = Resource.Success<DoctorRatingEntity?>(null)
        override suspend fun delete(id: String, patientId: String) = Resource.Success(true)
        override suspend fun getById(id: String) = Resource.Success<DoctorRatingEntity?>(null)
        override suspend fun getByAppointmentId(appointmentId: String) = Resource.Success<DoctorRatingEntity?>(rating)
        override suspend fun getByDoctorId(doctorId: String, page: Int, size: Int) = Resource.Success(emptyList<DoctorRatingEntity>() to 0L)
        override suspend fun getAverageRating(doctorId: String) = Resource.Success(0.0 to 0)
    }

    private class FakeMedicalRecordRepository(private val record: MedicalRecordEntity?) : MedicalRecordRepository {
        override suspend fun upsert(entity: MedicalRecordEntity) = Resource.Success<MedicalRecordEntity?>(entity)
        override suspend fun getByAppointmentId(appointmentId: String): Resource<MedicalRecordEntity?> =
            if (record == null) Resource.Error("Medical record not found") else Resource.Success(record)
        override suspend fun getByPatientId(patientId: String) = Resource.Success(emptyList<MedicalRecordEntity>())
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private fun appointment(status: String, doctorId: String = "doc-1") = AppointmentEntity(
        id = "appt-1", doctorId = doctorId, patientId = "pat-1", date = "2026-01-01",
        slotStart = "09:00", slotEnd = "09:30", status = status, bookedAt = "2026-01-01T00:00:00.000Z",
    )

    private fun rating() = DoctorRatingEntity(appointmentId = "appt-1", doctorId = "doc-1", patientId = "pat-1", rating = 5)

    private fun record(hasPrescription: Boolean, hasSummary: Boolean) = MedicalRecordEntity(
        appointmentId = "appt-1", doctorId = "doc-1", patientId = "pat-1",
        prescription = if (hasPrescription) listOf(PrescriptionItemEntity("Amoxicillin", "500mg", "3x daily", "5 days")) else emptyList(),
        summary = if (hasSummary) "Patient recovering well" else null,
    )

    private fun useCase(status: String, hasRating: Boolean, hasPrescription: Boolean, hasSummary: Boolean, appointmentDoctorId: String = "doc-1") =
        ReferralEligibilityUseCase(
            appointmentRepository = FakeAppointmentRepository(appointment(status, appointmentDoctorId)),
            doctorRatingRepository = FakeDoctorRatingRepository(if (hasRating) rating() else null),
            medicalRecordRepository = FakeMedicalRecordRepository(
                if (hasPrescription || hasSummary) record(hasPrescription, hasSummary) else null,
            ),
        )

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `not eligible when nothing has been submitted`(): Unit = runBlocking {
        val result = useCase(status = "CONFIRMED", hasRating = false, hasPrescription = false, hasSummary = false)("appt-1", "doc-1")

        assertFalse(result.data?.eligible ?: true)
        // not completed + no rating + no prescription + no summary
        assertEquals(4, result.data?.reasons?.size)
    }

    @Test
    fun `not eligible when completed but rating missing`(): Unit = runBlocking {
        val result = useCase(status = "COMPLETED", hasRating = false, hasPrescription = true, hasSummary = true)("appt-1", "doc-1")

        assertFalse(result.data?.eligible ?: true)
        assertEquals(listOf("Client rating has not been submitted"), result.data?.reasons)
    }

    @Test
    fun `not eligible when completed and rated but prescription missing`(): Unit = runBlocking {
        val result = useCase(status = "COMPLETED", hasRating = true, hasPrescription = false, hasSummary = true)("appt-1", "doc-1")

        assertFalse(result.data?.eligible ?: true)
        assertEquals(listOf("Prescription has not been submitted"), result.data?.reasons)
    }

    @Test
    fun `not eligible when completed and rated but summary missing`(): Unit = runBlocking {
        val result = useCase(status = "COMPLETED", hasRating = true, hasPrescription = true, hasSummary = false)("appt-1", "doc-1")

        assertFalse(result.data?.eligible ?: true)
        assertEquals(listOf("Medical summary has not been submitted"), result.data?.reasons)
    }

    @Test
    fun `eligible once completed, rated, and both prescription and summary submitted`(): Unit = runBlocking {
        val result = useCase(status = "COMPLETED", hasRating = true, hasPrescription = true, hasSummary = true)("appt-1", "doc-1")

        assertTrue(result.data?.eligible ?: false)
        assertTrue(result.data?.reasons.isNullOrEmpty())
    }

    @Test
    fun `rejects when caller is not the appointment's doctor`(): Unit = runBlocking {
        val result = useCase(status = "COMPLETED", hasRating = true, hasPrescription = true, hasSummary = true, appointmentDoctorId = "doc-1")("appt-1", "doc-2")

        assertEquals(403, result.httpStatusCode)
    }
}
