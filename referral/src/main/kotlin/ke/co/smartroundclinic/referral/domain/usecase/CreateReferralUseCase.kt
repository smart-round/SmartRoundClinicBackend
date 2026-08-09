package ke.co.smartroundclinic.referral.domain.usecase

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.NotificationChannel
import ke.co.smartroundclinic.common.NotificationDestination
import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.common.PushNotificationEvents
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.VerifiedDoctorResolver
import ke.co.smartroundclinic.referral.data.entity.ReferralEntity
import ke.co.smartroundclinic.referral.data.lookup.DoctorDisplayLookup
import ke.co.smartroundclinic.referral.domain.repository.ReferralRepository
import ke.co.smartroundclinic.referral.presentation.dto.request.CreateReferralReq
import ke.co.smartroundclinic.referral.presentation.dto.response.ReferralRes
import ke.co.smartroundclinic.referral.presentation.dto.response.toRes
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import org.slf4j.LoggerFactory

class CreateReferralUseCase(
    private val referralRepository: ReferralRepository,
    private val appointmentRepository: AppointmentRepository,
    private val eligibilityUseCase: ReferralEligibilityUseCase,
    private val doctorDisplayLookup: DoctorDisplayLookup,
    private val verifiedDoctorResolver: VerifiedDoctorResolver? = null,
    private val notificationSender: NotificationSender? = null,
) {
    private val log = LoggerFactory.getLogger(CreateReferralUseCase::class.java)

    suspend operator fun invoke(req: CreateReferralReq, referringDoctorId: String): DefaultResponse<ReferralRes?> {
        if (req.receivingDoctorId == referringDoctorId) {
            return Resource.Error<Nothing>("You cannot refer a patient to yourself")
                .toDefaultResponse(failedStatusCode = 400) { null }
        }
        // The doctor app no longer collects a free-text reason — referring goes straight to
        // choosing the receiving doctor, who gets the patient's medical summary instead. Referrals
        // created before that change keep the reason they were given.

        val appointment = (appointmentRepository.getById(req.appointmentId) as? Resource.Success)?.data
            ?: return Resource.Error<Nothing>("Appointment not found").toDefaultResponse(failedStatusCode = 404) { null }
        if (appointment.doctorId != referringDoctorId) {
            return Resource.Error<Nothing>("Not authorized to refer this appointment")
                .toDefaultResponse(failedStatusCode = 403) { null }
        }

        val eligibility = eligibilityUseCase(req.appointmentId, referringDoctorId).data
        if (eligibility?.eligible != true) {
            val reasons = eligibility?.reasons?.joinToString("; ") ?: "Appointment is not eligible for referral"
            return Resource.Error<Nothing>(reasons).toDefaultResponse(failedStatusCode = 422) { null }
        }

        if (verifiedDoctorResolver != null) {
            if (!verifiedDoctorResolver.isVerified(referringDoctorId)) {
                return Resource.Error<Nothing>("You must be a verified doctor to refer patients")
                    .toDefaultResponse(failedStatusCode = 403) { null }
            }
            if (!verifiedDoctorResolver.isVerified(req.receivingDoctorId)) {
                return Resource.Error<Nothing>("The selected doctor is not eligible to receive referrals")
                    .toDefaultResponse(failedStatusCode = 400) { null }
            }
        }

        val referringDoctorInfo = doctorDisplayLookup.lookup(referringDoctorId)

        val entity = ReferralEntity(
            sourceAppointmentId = req.appointmentId,
            referringDoctorId = referringDoctorId,
            referringDoctorName = referringDoctorInfo.name,
            referringDoctorPicture = referringDoctorInfo.profilePicture,
            patientId = appointment.patientId,
            receivingDoctorId = req.receivingDoctorId,
            reason = req.reason.orEmpty(),
        )

        val result = referralRepository.create(entity)
        val referral = (result as? Resource.Success)?.data
        if (referral != null) {
            runCatching {
                notificationSender?.send(
                    title = PushNotificationEvents.REFERRAL_CREATED,
                    message = "Dr. ${referringDoctorInfo.name ?: "your doctor"} has referred you to another doctor",
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    destination = NotificationDestination.PATIENT,
                    recipientId = appointment.patientId,
                    metadata = mapOf("event" to PushNotificationEvents.REFERRAL_CREATED, "referralId" to referral.id),
                )
            }.onFailure { log.warn("Failed to notify patient of new referral id=${referral.id} — ${it.message}") }
        }

        return result.toDefaultResponse(successStatusCode = 201) { it?.toModel()?.toRes() }
    }
}
