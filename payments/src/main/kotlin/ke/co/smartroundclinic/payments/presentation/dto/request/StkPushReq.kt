package ke.co.smartroundclinic.payments.presentation.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Patient-facing: initiate an STK push for an existing confirmed appointment. */
@Serializable
data class StkPushAppointmentBody(
    @SerialName("appointment_id") val appointmentId: String,
    @SerialName("phone_number") val phoneNumber: String,
)

/** Patient-facing: initiate an STK push before an appointment is booked.
 *  For rebooking (?rebooking=true), also supply previousAppointmentId. */
@Serializable
data class StkPushPreBookingBody(
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("previous_appointment_id") val previousAppointmentId: String? = null,
)
