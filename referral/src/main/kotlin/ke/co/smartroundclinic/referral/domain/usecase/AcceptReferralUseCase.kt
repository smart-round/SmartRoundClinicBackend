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
 * Flips a PENDING referral to ACCEPTED — does NOT create an appointment. It only unlocks
 * navigation to the receiving doctor's profile so the patient can book normally, per
 * CR-SMRC-0001 §5.1.3 ("Accepting takes the patient to the specific doctor's profile to proceed
 * with booking").
 */
class AcceptReferralUseCase(
    private val repository: ReferralRepository,
    private val notificationSender: NotificationSender? = null,
) {
    private val log = LoggerFactory.getLogger(AcceptReferralUseCase::class.java)

    suspend operator fun invoke(id: String, patientId: String): DefaultResponse<ReferralRes?> {
        val result = repository.accept(id, patientId)
        val referral = (result as? Resource.Success)?.data
        if (referral != null) {
            runCatching {
                notificationSender?.send(
                    title = PushNotificationEvents.REFERRAL_ACCEPTED,
                    message = "Your patient has accepted your referral",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.DOCTOR,
                    recipientId = referral.referringDoctorId,
                    metadata = mapOf("event" to PushNotificationEvents.REFERRAL_ACCEPTED, "referralId" to referral.id),
                )
            }.onFailure { log.warn("Failed to notify referring doctor of accepted referral id=${referral.id} — ${it.message}") }
        }
        return result.toDefaultResponse(failedStatusCode = 404) { it?.toModel()?.toRes() }
    }
}
