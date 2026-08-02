package ke.co.smartroundclinic.common

object PushNotificationEvents {

    // ── Appointments ────────────────────────────────────────────────────────────
    const val APPOINTMENT_REQUEST   = "New Appointment Request"
    const val APPOINTMENT_BOOKED    = "Appointment Booked"
    const val APPOINTMENT_CONFIRMED = "Appointment Confirmed"
    const val APPOINTMENT_CANCELLED = "Appointment Cancelled"
    const val APPOINTMENT_COMPLETED = "Appointment Completed"

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

    // ── Incoming call ringing (invite/answer/decline/cancel signaling) ────────────
    const val CALL_INVITE    = "Incoming Video Call"
    const val CALL_ANSWERED  = "Call Answered"
    const val CALL_DECLINED  = "Call Declined"
    const val CALL_CANCELLED = "Call Cancelled"

    // ── Doctor-to-doctor call ringing — distinct from CALL_* above because the payload shape
    // differs (threadId, not doctorId/patientId); reusing CALL_INVITE etc. here would silently
    // fail to deep-link since the app's shared push handler expects the patient-call shape.
    const val DOCTOR_CALL_INVITE    = "Incoming Doctor Call"
    const val DOCTOR_CALL_ANSWERED  = "Doctor Call Answered"
    const val DOCTOR_CALL_DECLINED  = "Doctor Call Declined"
    const val DOCTOR_CALL_CANCELLED = "Doctor Call Cancelled"

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
    const val ACCOUNT_SUSPENDED   = "Account Suspended"
    const val ACCOUNT_REINSTATED  = "Account Reinstated"

    // ── Referral ─────────────────────────────────────────────────────────────────
    const val REFERRAL_CREATED  = "You've Been Referred"
    const val REFERRAL_ACCEPTED = "Referral Accepted"
    const val REFERRAL_DECLINED = "Referral Declined"
}
