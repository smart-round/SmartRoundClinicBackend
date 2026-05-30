package ke.co.smartroundclinic.payments.data.entity

import org.bson.types.ObjectId
import kotlin.time.Clock

data class PaymentLogEntity(
    val id: String = ObjectId().toString(),
    val invoiceId: String?,
    val state: String?,
    val provider: String?,
    val value: String?,
    val charges: String?,
    val netAmount: String?,
    val currency: String?,
    val account: String?,
    val mpesaReference: String?,
    val providerRef: String?,
    val apiRef: String?,
    val paymentEntityId: String? = null,
    val appointmentId: String? = null,
    val createdAt: String = Clock.System.now().toString(),
)
