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
)

data class AppointmentPaymentInfo(
    val paymentId: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val commissionRate: Double,
    val platformFee: Double,
    val netAmount: Double,
)

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
        val doc = payments
            .find(Filters.eq(PaymentDoc::appointmentId.name, appointmentId))
            .firstOrNull() ?: return null
        val fee = doc.amount * (doc.commissionRate / 100.0)
        return AppointmentPaymentInfo(
            paymentId = doc.id,
            amount = doc.amount,
            currency = doc.currency,
            status = doc.status,
            commissionRate = doc.commissionRate,
            platformFee = fee,
            netAmount = doc.amount - fee,
        )
    }

    /** Batch-fetches payments for a list of appointmentIds. Returns a map of appointmentId → info. */
    suspend fun getPaymentsByAppointmentIds(appointmentIds: Collection<String>): Map<String, AppointmentPaymentInfo> {
        if (appointmentIds.isEmpty()) return emptyMap()
        return payments
            .find(Filters.`in`(PaymentDoc::appointmentId.name, appointmentIds))
            .toList()
            .associate { doc ->
                val fee = doc.amount * (doc.commissionRate / 100.0)
                doc.appointmentId to AppointmentPaymentInfo(
                    paymentId = doc.id,
                    amount = doc.amount,
                    currency = doc.currency,
                    status = doc.status,
                    commissionRate = doc.commissionRate,
                    platformFee = fee,
                    netAmount = doc.amount - fee,
                )
            }
    }

    suspend fun getPaymentById(paymentId: String): AppointmentPaymentInfo? {
        val doc = payments
            .find(Filters.eq(PaymentDoc::id.name, paymentId))
            .firstOrNull() ?: return null
        val fee = doc.amount * (doc.commissionRate / 100.0)
        return AppointmentPaymentInfo(
            paymentId = doc.id,
            amount = doc.amount,
            currency = doc.currency,
            status = doc.status,
            commissionRate = doc.commissionRate,
            platformFee = fee,
            netAmount = doc.amount - fee,
        )
    }

    /** Batch-fetches payments for a list of payment ids. Returns a map of paymentId → info. */
    suspend fun getPaymentsByIds(paymentIds: Collection<String>): Map<String, AppointmentPaymentInfo> {
        if (paymentIds.isEmpty()) return emptyMap()
        return payments
            .find(Filters.`in`(PaymentDoc::id.name, paymentIds))
            .toList()
            .associate { doc ->
                val fee = doc.amount * (doc.commissionRate / 100.0)
                doc.id to AppointmentPaymentInfo(
                    paymentId = doc.id,
                    amount = doc.amount,
                    currency = doc.currency,
                    status = doc.status,
                    commissionRate = doc.commissionRate,
                    platformFee = fee,
                    netAmount = doc.amount - fee,
                )
            }
    }
}
