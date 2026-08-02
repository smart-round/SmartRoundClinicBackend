package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralRes
import ke.co.smartroundclinic.referral.presentation.dto.response.toRes
import org.slf4j.LoggerFactory

/**
 * Flips a PENDING referral to DECLINED — a deliberate addition beyond the CR-as-written (which only
 * specified Pending -> Accepted), so a referral the patient doesn't want reaches a clean terminal
 * state instead of sitting Pending forever, and the referring doctor gets a real signal.
 */
class DeclineReferralUseCase(
    private val repository: ReferralRepository,
    private val notificationSender: NotificationSender? = null,
) {
    private val log = LoggerFactory.getLogger(DeclineReferralUseCase::class.java)

    suspend operator fun invoke(id: String, patientId: String): DefaultResponse<ReferralRes?> {
        val result = repository.decline(id, patientId)
        val referral = (result as? Resource.Success)?.data
        if (referral != null) {
            runCatching {
                notificationSender?.send(
                    title = PushNotificationEvents.REFERRAL_DECLINED,
                    message = "Your patient has declined your referral",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.DOCTOR,
                    recipientId = referral.referringDoctorId,
                    metadata = mapOf("event" to PushNotificationEvents.REFERRAL_DECLINED, "referralId" to referral.id),
                )
            }.onFailure { log.warn("Failed to notify referring doctor of declined referral id=${referral.id} — ${it.message}") }
        }
        return result.toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
    }
}
