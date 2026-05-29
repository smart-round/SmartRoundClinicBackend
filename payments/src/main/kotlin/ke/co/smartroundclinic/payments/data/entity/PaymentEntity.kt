package ke.co.smartroundclinic.payments.data.entity

import ke.co.smartroundclinic.payments.domain.model.Payment
import org.bson.types.ObjectId
import kotlin.time.Clock

data class PaymentEntity(
    val id: String = ObjectId().toString(),
    val appointmentId: String,
    val patientId: String,
    val doctorId: String,
    val amount: Double,
    val currency: String = "KES",
    val status: PaymentStatus = PaymentStatus.PENDING,
    val paymentMethod: String? = null,
    val transactionRef: String? = null,
    val notes: String? = null,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    enum class PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }

    fun toModel() = Payment(
        id = id,
        appointmentId = appointmentId,
        patientId = patientId,
        doctorId = doctorId,
        amount = amount,
        currency = currency,
        status = status.name,
        paymentMethod = paymentMethod,
        transactionRef = transactionRef,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
