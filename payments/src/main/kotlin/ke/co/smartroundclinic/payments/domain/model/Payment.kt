package ke.co.smartroundclinic.payments.domain.model

import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.presentation.dto.request.InitiatePaymentReq
import ke.co.smartroundclinic.payments.presentation.dto.response.PaymentRes
import org.bson.types.ObjectId
import kotlin.time.Clock

data class Payment(
    val id: String,
    val appointmentId: String?,
    val patientId: String,
    val doctorId: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val paymentMethod: String?,
    val transactionRef: String?,
    val invoiceId: String?,
    val mpesaReference: String?,
    val charges: String?,
    val netAmount: String?,
    val account: String?,
    val notes: String?,
    val commissionRate: Double,
    val createdAt: String,
    val updatedAt: String?,
)

fun InitiatePaymentReq.toModel(patientId: String, commissionRate: Double) = Payment(
    id = ObjectId().toString(),
    appointmentId = appointmentId,
    patientId = patientId,
    doctorId = doctorId,
    amount = amount,
    currency = currency,
    status = PaymentEntity.PaymentStatus.PENDING.name,
    paymentMethod = paymentMethod,
    transactionRef = null,
    invoiceId = null,
    mpesaReference = null,
    charges = null,
    netAmount = null,
    account = null,
    notes = notes,
    commissionRate = commissionRate,
    createdAt = Clock.System.now().toString(),
    updatedAt = null,
)

fun Payment.toEntity() = PaymentEntity(
    id = id,
    appointmentId = appointmentId,
    patientId = patientId,
    doctorId = doctorId,
    amount = amount,
    currency = currency,
    status = PaymentEntity.PaymentStatus.valueOf(status),
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

fun Payment.toRes(): PaymentRes {
    val platformFee = amount * (commissionRate / 100.0)
    return PaymentRes(
        id = id,
        appointmentId = appointmentId,
        patientId = patientId,
        doctorId = doctorId,
        amount = amount,
        currency = currency,
        status = status,
        paymentMethod = paymentMethod,
        transactionRef = transactionRef,
        invoiceId = invoiceId,
        mpesaReference = mpesaReference,
        charges = charges,
        netAmount = netAmount,
        account = account,
        notes = notes,
        commissionRate = commissionRate,
        platformFee = platformFee,
        netEarnings = amount - platformFee,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
