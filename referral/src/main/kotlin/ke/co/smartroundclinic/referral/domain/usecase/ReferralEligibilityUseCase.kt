package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralEligibilityRes
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository

/**
 * A doctor can refer a patient once the source appointment is COMPLETED. See CR-SMRC-0001 §5.1.1
 * — the rating/prescription/summary prerequisites originally specified there were dropped per
 * user direction; completion of the appointment is now the only requirement.
 */
class ReferralEligibilityUseCase(
    private val appointmentRepository: AppointmentRepository,
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

        return Resource.Success(ReferralEligibilityRes(eligible = reasons.isEmpty(), reasons = reasons))
            .toDefaultResponse { it }
    }
}
