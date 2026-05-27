package ke.co.smartroundclinic.payments.domain.model

import ke.co.smartroundclinic.payments.data.entity.PaymentEntity
import ke.co.smartroundclinic.payments.presentation.dto.request.InitiatePaymentReq
import ke.co.smartroundclinic.payments.presentation.dto.response.PaymentRes
import org.bson.types.ObjectId
import kotlin.time.Clock

data class Payment(
    val id: String,
    val appointmentId: String,
    val patientId: String,
    val doctorId: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val paymentMethod: String?,
    val transactionRef: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String?,
)

fun InitiatePaymentReq.toModel(patientId: String) = Payment(
    id = ObjectId().toString(),
    appointmentId = appointmentId,
    patientId = patientId,
    doctorId = doctorId,
    amount = amount,
    currency = currency,
    status = PaymentEntity.PaymentStatus.PENDING.name,
    paymentMethod = paymentMethod,
    transactionRef = null,
    notes = notes,
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
    status = status,
    paymentMethod = paymentMethod,
    transactionRef = transactionRef,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Payment.toRes() = PaymentRes(
    id = id,
    appointmentId = appointmentId,
    patientId = patientId,
    doctorId = doctorId,
    amount = amount,
    currency = currency,
    status = status,
    paymentMethod = paymentMethod,
    transactionRef = transactionRef,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
