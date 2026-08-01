package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.domain.repository.DoctorRatingRepository
import ke.co.smartroundclinic.medicalrecords.domain.repository.MedicalRecordRepository
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralEligibilityRes
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository

/**
 * A doctor can only refer a patient once the source appointment is COMPLETED and they've submitted
 * the client rating, medical summary, and prescription for it. See CR-SMRC-0001 §5.1.1.
 */
class ReferralEligibilityUseCase(
    private val appointmentRepository: AppointmentRepository,
    private val doctorRatingRepository: DoctorRatingRepository,
    private val medicalRecordRepository: MedicalRecordRepository,
) {
    suspend operator fun invoke(appointmentId: String, doctorId: String): DefaultResponse<ReferralEligibilityRes?> {
        val appointment = (appointmentRepository.getById(appointmentId) as? Resource.Success)?.data
            ?: return Resource.Error<Nothing>("Appointment not found").toDefaultResponse(failedStatusCode = 404) { null }

        if (appointment.doctorId != doctorId) {
            return Resource.Error<Nothing>("Not authorized to refer this appointment")
                .toDefaultResponse(failedStatusCode = 403) { null }
        }

        val reasons = mutableListOf<String>()
        if (appointment.status != "COMPLETED") {
            reasons.add("Appointment must be marked complete")
        }

        val rating = (doctorRatingRepository.getByAppointmentId(appointmentId) as? Resource.Success)?.data
        if (rating == null) reasons.add("Client rating has not been submitted")

        val record = (medicalRecordRepository.getByAppointmentId(appointmentId) as? Resource.Success)?.data
        if (record == null || record.prescription.isEmpty()) reasons.add("Prescription has not been submitted")
        if (record == null || record.summary.isNullOrBlank()) reasons.add("Medical summary has not been submitted")

        return Resource.Success(ReferralEligibilityRes(eligible = reasons.isEmpty(), reasons = reasons))
            .toDefaultResponse { it }
    }
}
