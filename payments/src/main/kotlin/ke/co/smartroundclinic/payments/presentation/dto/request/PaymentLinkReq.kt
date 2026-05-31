package ke.co.smartroundclinic.payments.presentation.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentLinkBody(
    @SerialName("is_active") val isActive: Boolean? = null,
    val amount: Double? = null,
    val currency: String? = null,
)

@Serializable
data class UpdatePaymentLinkBody(
    val title: String,
    @SerialName("is_active") val isActive: Boolean? = null,
    val amount: Double? = null,
    @SerialName("usage_limit") val usageLimit: Int? = null,
    val currency: String? = null,
)

/** Patient-facing: create a payment link for their appointment.
 *  Title, ID, amount (followUpFee from service tier), and currency (KES) are all auto-filled. */
@Serializable
data class CreateAppointmentPaymentLinkBody(
    val appointmentId: String,
)

/** Patient-facing: create a payment link before an appointment is booked.
 *  Amount is resolved from the doctor's service tier; no appointmentId required at this stage. */
@Serializable
data class CreatePreBookingPaymentLinkBody(
    val doctorId: String,
)
