package ke.co.smartroundclinic.scheduling.data.lookup

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

data class PaymentVerifyResult(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val status: String,
    val appointmentId: String?,
)

class PaymentVerificationLookup(paymentsDb: MongoDatabase) {

    private val log = LoggerFactory.getLogger(PaymentVerificationLookup::class.java)

    @Serializable
    private data class PaymentDoc(
        val id: String,
        val doctorId: String,
        val patientId: String,
        val status: String,
        val appointmentId: String? = null,
    )

    private val payments = paymentsDb.getCollection<PaymentDoc>(MongoDBConstants.PAYMENTS)

    suspend fun getByTransactionRef(transactionRef: String): PaymentVerifyResult? {
        val doc = try {
            payments.find(Filters.eq("transactionRef", transactionRef)).firstOrNull()
        } catch (e: Exception) {
            log.error("Failed to fetch payment by transactionRef=$transactionRef — ${e.message}", e)
            return null
        } ?: return null
        return PaymentVerifyResult(
            id = doc.id,
            doctorId = doc.doctorId,
            patientId = doc.patientId,
            status = doc.status,
            appointmentId = doc.appointmentId,
        )
    }

    suspend fun linkToAppointment(paymentId: String, appointmentId: String) {
        try {
            payments.findOneAndUpdate(
                Filters.eq("id", paymentId),
                Updates.set("appointmentId", appointmentId),
            )
            log.info("Payment id=$paymentId linked to appointmentId=$appointmentId")
        } catch (e: Exception) {
            log.error("Failed to link payment id=$paymentId to appointmentId=$appointmentId — ${e.message}", e)
        }
    }
}
