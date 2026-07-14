package ke.co.smartroundclinic.payments.data.lookup

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class DoctorPaymentDetails(
    val doctorId: String,
    val bankCode: String,
    val accountNumber: String,
    val accountName: String,
    val walletId: String? = null,
)

class DoctorPaymentDetailsLookup(doctorDb: MongoDatabase) {
    private val col = doctorDb.getCollection<DoctorPaymentDetails>(MongoDBConstants.DOCTOR_PAYMENT_DETAILS)

    suspend fun getByDoctorId(doctorId: String): DoctorPaymentDetails? =
        col.find(Filters.eq(DoctorPaymentDetails::doctorId.name, doctorId)).firstOrNull()

    /** Backfills walletId for doctors whose payment details predate wallet provisioning. */
    suspend fun setWalletId(doctorId: String, walletId: String) {
        col.updateOne(
            Filters.eq(DoctorPaymentDetails::doctorId.name, doctorId),
            Updates.combine(
                Updates.set(DoctorPaymentDetails::walletId.name, walletId),
                Updates.set("updatedAt", Clock.System.now().toString()),
            ),
        )
    }
}
