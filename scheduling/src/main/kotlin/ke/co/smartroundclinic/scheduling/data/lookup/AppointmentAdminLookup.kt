package ke.co.smartroundclinic.scheduling.data.lookup

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable

@Serializable
private data class UserNameDoc(val id: String, val fullName: String)

@Serializable
private data class PaymentDoc(
    val id: String,
    val appointmentId: String,
    val amount: Double = 0.0,
    val currency: String = "KES",
    val status: String = "PENDING",
    val commissionRate: Double = 0.0,
    val paymentMethod: String? = null,
    val account: String? = null,
    val transactionRef: String? = null,
    val invoiceId: String? = null,
    val mpesaReference: String? = null,
)

data class AppointmentPaymentInfo(
    val paymentId: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val commissionRate: Double,
    val platformFee: Double,
    val netAmount: Double,
    val paymentMethod: String?,
    val phoneNumber: String?,
    val transactionRef: String?,
    val invoiceId: String?,
    val mpesaReference: String?,
)

private fun PaymentDoc.toInfo(): AppointmentPaymentInfo {
    val fee = amount * (commissionRate / 100.0)
    return AppointmentPaymentInfo(
        paymentId = id,
        amount = amount,
        currency = currency,
        status = status,
        commissionRate = commissionRate,
        platformFee = fee,
        netAmount = amount - fee,
        paymentMethod = paymentMethod,
        phoneNumber = account,
        transactionRef = transactionRef,
        invoiceId = invoiceId,
        mpesaReference = mpesaReference,
    )
}

class AppointmentAdminLookup(
    authDb: MongoDatabase,
    paymentsDb: MongoDatabase,
) {
    private val users = authDb.getCollection<UserNameDoc>(MongoDBConstants.AUTH_USER)
    private val payments = paymentsDb.getCollection<PaymentDoc>(MongoDBConstants.PAYMENTS)

    /** Returns a map of userId → fullName for all provided IDs in a single query. */
    suspend fun getUserNames(ids: Collection<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        return users
            .find(Filters.`in`(UserNameDoc::id.name, ids))
            .toList()
            .associate { it.id to it.fullName }
    }

    suspend fun getPaymentByAppointmentId(appointmentId: String): AppointmentPaymentInfo? {
        return payments
            .find(Filters.eq(PaymentDoc::appointmentId.name, appointmentId))
            .firstOrNull()
            ?.toInfo()
    }

    /** Batch-fetches payments for a list of appointmentIds. Returns a map of appointmentId → info. */
    suspend fun getPaymentsByAppointmentIds(appointmentIds: Collection<String>): Map<String, AppointmentPaymentInfo> {
        if (appointmentIds.isEmpty()) return emptyMap()
        return payments
            .find(Filters.`in`(PaymentDoc::appointmentId.name, appointmentIds))
            .toList()
            .associate { it.appointmentId to it.toInfo() }
    }

    suspend fun getPaymentById(paymentId: String): AppointmentPaymentInfo? {
        return payments
            .find(Filters.eq(PaymentDoc::id.name, paymentId))
            .firstOrNull()
            ?.toInfo()
    }

    /** Batch-fetches payments for a list of payment ids. Returns a map of paymentId → info. */
    suspend fun getPaymentsByIds(paymentIds: Collection<String>): Map<String, AppointmentPaymentInfo> {
        if (paymentIds.isEmpty()) return emptyMap()
        return payments
            .find(Filters.`in`(PaymentDoc::id.name, paymentIds))
            .toList()
            .associate { it.id to it.toInfo() }
    }
}
