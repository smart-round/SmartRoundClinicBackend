package ke.co.smartroundclinic.payments.data.lookup

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable

@Serializable
data class DoctorPaymentDetails(
    val doctorId: String,
    val bankCode: String,
    val accountNumber: String,
    val accountName: String,
)

class DoctorPaymentDetailsLookup(doctorDb: MongoDatabase) {
    private val col = doctorDb.getCollection<DoctorPaymentDetails>(MongoDBConstants.DOCTOR_PAYMENT_DETAILS)

    suspend fun getByDoctorId(doctorId: String): DoctorPaymentDetails? =
        col.find(Filters.eq(DoctorPaymentDetails::doctorId.name, doctorId)).firstOrNull()
}
