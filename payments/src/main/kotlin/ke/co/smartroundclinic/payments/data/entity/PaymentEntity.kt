package ke.co.smartroundclinic.payments.data.entity

import ke.co.smartroundclinic.payments.domain.model.Payment
import org.bson.types.ObjectId
import kotlin.time.Clock

data class PaymentEntity(
    val id: String = ObjectId().toString(),
    val appointmentId: String? = null,
    val patientId: String,
    val doctorId: String,
    val amount: Double,
    val currency: String = "KES",
    val status: PaymentStatus = PaymentStatus.PENDING,
    val paymentMethod: String? = null,
    val transactionRef: String? = null,
    val invoiceId: String? = null,
    val mpesaReference: String? = null,
    val charges: String? = null,
    val netAmount: String? = null,
    val account: String? = null,
    val notes: String? = null,
    val commissionRate: Double = 0.0,
    val doctorCreditedAt: String? = null,
    val commissionCreditedAt: String? = null,
    val creditIneligible: Boolean = false,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String? = null,
) {
    enum class PaymentStatus { PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED }

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
        invoiceId = invoiceId,
        mpesaReference = mpesaReference,
        charges = charges,
        netAmount = netAmount,
        account = account,
        notes = notes,
        commissionRate = commissionRate,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
