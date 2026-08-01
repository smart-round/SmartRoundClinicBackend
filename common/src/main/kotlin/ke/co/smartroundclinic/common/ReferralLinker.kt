package ke.co.smartroundclinic.common

/**
 * Narrow cross-module callback letting `:scheduling`'s BookAppointmentUseCase validate and link an
 * accepted referral at booking time without a hard Gradle dependency on `:referral` — mirrors the
 * existing NotificationSender?/AppointmentEarningsCreditor? nullable-cross-module-callback idiom.
 */
interface ReferralLinker {
    /** Validates [referralId] is ACCEPTED, targets [doctorId], and belongs to [patientId]. Null = valid. */
    suspend fun validateForBooking(referralId: String, doctorId: String, patientId: String): String?

    /** Records that [referralId] resulted in [appointmentId] once the booking succeeds. */
    suspend fun linkResultingAppointment(referralId: String, appointmentId: String)
}
