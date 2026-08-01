package ke.co.smartroundclinic.infra.redis

object RedisKeys {
    /** Active/inactive presence flag for a connected user. TTL = PRESENCE_TTL_SECONDS. */
    fun presence(userId: String) = "presence:$userId"

    const val PRESENCE_TTL_SECONDS = 35L

    /** Serialized CallInviteState for a single ringing call, keyed by callId. TTL = CALL_INVITE_TTL_SECONDS. */
    fun callInvite(callId: String) = "call_invite:$callId"

    /** callId of the currently-ringing invite for a thread, used to reject a second simultaneous invite. */
    fun activeCallForThread(doctorId: String, patientId: String) = "call_invite:thread:$doctorId:$patientId"

    /** Same purpose as [activeCallForThread] but for a doctor-to-doctor thread, keyed by its own threadId rather than a (doctorId, patientId) pair. */
    fun activeCallForDoctorChatThread(threadId: String) = "call_invite:doctor_thread:$threadId"

    /** How long an invite rings before it's considered a missed call and expires on its own. */
    const val CALL_INVITE_TTL_SECONDS = 45L
}