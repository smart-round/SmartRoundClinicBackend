package ke.co.smartroundclinic.common

object PushNotificationEvents {

    // ── Appointments ────────────────────────────────────────────────────────────
    const val APPOINTMENT_REQUEST   = "New Appointment Request"
    const val APPOINTMENT_BOOKED    = "Appointment Booked"
    const val APPOINTMENT_CONFIRMED = "Appointment Confirmed"
    const val APPOINTMENT_CANCELLED = "Appointment Cancelled"
    const val APPOINTMENT_COMPLETED = "Appointment Completed"
    const val APPOINTMENT_NO_SHOW   = "Missed Appointment"

    // ── Doctor onboarding ───────────────────────────────────────────────────────
    const val DOCTOR_APPLICATION_APPROVED = "Application Approved"
    const val DOCTOR_APPLICATION_REJECTED = "Application Unsuccessful"

    // ── Consultation session ────────────────────────────────────────────────────
    const val CONSULTATION_DOCTOR_READY = "Doctor is Ready"
    const val CONSULTATION_PATIENT_READY = "Patient is Ready"
    const val CONSULTATION_ENDED        = "Consultation Ended"

    // ── Consultation call ───────────────────────────────────────────────────────
    const val CALL_DOCTOR_JOINED = "Doctor Joined the Call"
    const val CALL_PATIENT_JOINED = "Patient Joined the Call"
    const val CALL_ENDED         = "Call Ended"

    // ── Chat (DM) ───────────────────────────────────────────────────────────────
    const val NEW_CHAT_MESSAGE = "New Chat Message"
    fun newChatMessage(senderName: String) = "New message from $senderName"

    // ── Articles ────────────────────────────────────────────────────────────────
    const val ARTICLE_SUSPENDED = "Article Suspended"
    const val ARTICLE_DELETED   = "Article Deleted"

    // ── Medical records ─────────────────────────────────────────────────────────
    const val MEDICAL_RECORD_UPDATED = "Medical Record Updated"

    // ── Support tickets ─────────────────────────────────────────────────────────
    const val TICKET_STATUS_UPDATED = "Support ticket status updated"

    // ── Account ──────────────────────────────────────────────────────────────────
    const val ACCOUNT_SUSPENDED = "Account Suspended"
}
