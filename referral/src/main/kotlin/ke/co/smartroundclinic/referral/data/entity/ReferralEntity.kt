package ke.co.smartroundclinic.referral.data.entity

import ke.co.smartroundclinic.referral.domain.model.Referral
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.time.Clock

object ReferralStatus {
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val DECLINED = "DECLINED"
}

@Serializable
data class ReferralEntity(
    val id: String = ObjectId().toString(),
    val sourceAppointmentId: String,
    val referringDoctorId: String,
    // Denormalized snapshot of the referring doctor's display info at creation time — cheap to
    // read back on the receiving doctor's own bookings list without a live join. See CR-SMRC-0001
    // round-1 plan, "Referral card data source".
    val referringDoctorName: String?,
    val referringDoctorPicture: String?,
    val patientId: String,
    val receivingDoctorId: String,
    val reason: String,
    val status: String = ReferralStatus.PENDING,
    val resultingAppointmentId: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val respondedAt: String? = null,
) {
    fun toModel() = Referral(
        id = id,
        sourceAppointmentId = sourceAppointmentId,
        referringDoctorId = referringDoctorId,
        referringDoctorName = referringDoctorName,
        referringDoctorPicture = referringDoctorPicture,
        patientId = patientId,
        receivingDoctorId = receivingDoctorId,
        reason = reason,
        status = status,
        resultingAppointmentId = resultingAppointmentId,
        createdAt = createdAt,
        respondedAt = respondedAt,
    )
}

fun Referral.toEntity() = ReferralEntity(
    id = id,
    sourceAppointmentId = sourceAppointmentId,
    referringDoctorId = referringDoctorId,
    referringDoctorName = referringDoctorName,
    referringDoctorPicture = referringDoctorPicture,
    patientId = patientId,
    receivingDoctorId = receivingDoctorId,
    reason = reason,
    status = status,
    resultingAppointmentId = resultingAppointmentId,
    createdAt = createdAt,
    respondedAt = respondedAt,
)
