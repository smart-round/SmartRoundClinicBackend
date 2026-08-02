package ke.co.smartroundclinic.referral.domain.service

import ke.co.smartroundclinic.common.ReferralLinker
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.referral.data.entity.ReferralStatus
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository

class ReferralLinkerImpl(private val repository: ReferralRepository) : ReferralLinker {

    override suspend fun validateForBooking(referralId: String, doctorId: String, patientId: String): String? {
        val referral = (repository.getById(referralId) as? Resource.Success)?.data
            ?: return "Referral not found"
        if (referral.status != ReferralStatus.ACCEPTED) return "This referral has not been accepted yet"
        if (referral.receivingDoctorId != doctorId) return "This referral is not for this doctor"
        if (referral.patientId != patientId) return "This referral does not belong to you"
        if (referral.resultingAppointmentId != null) return "This referral has already been used to book an appointment"
        return null
    }

    override suspend fun linkResultingAppointment(referralId: String, appointmentId: String) {
        repository.linkResultingAppointment(referralId, appointmentId)
    }
}
