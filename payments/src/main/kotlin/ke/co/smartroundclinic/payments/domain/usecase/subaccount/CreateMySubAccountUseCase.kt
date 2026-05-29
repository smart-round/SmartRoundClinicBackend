package ke.co.smartroundclinic.payments.domain.usecase.subaccount

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreateSubAccountReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.CreateSubAccountRes
import ke.co.smartroundclinic.payments.domain.repository.PaystackRepository
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory

class CreateMySubAccountUseCase(
    private val paystackRepository: PaystackRepository,
    doctorDb: MongoDatabase,
    adminDb: MongoDatabase,
) {
    private val log = LoggerFactory.getLogger(CreateMySubAccountUseCase::class.java)
    private val paymentDetailsCol = doctorDb.getCollection<Document>(MongoDBConstants.DOCTOR_PAYMENT_DETAILS)
    private val authCol = doctorDb.getCollection<Document>("doctor_profiles")
    private val commissionCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_COMMISSION_RATES)

    suspend operator fun invoke(doctorId: String): DefaultResponse<CreateSubAccountRes?> {
        val detailsDoc = paymentDetailsCol.find(Filters.eq("doctorId", doctorId)).firstOrNull()
            ?: return Resource.Error<CreateSubAccountRes>("Payment details not found. Please add your bank details first.")
                .toDefaultResponse(failedStatusCode = HttpStatusCode.UnprocessableEntity.value)

        if (detailsDoc.getString("paystackSubaccountCode") != null) {
            return Resource.Error<CreateSubAccountRes>("A Paystack subaccount already exists for this account.")
                .toDefaultResponse(failedStatusCode = HttpStatusCode.Conflict.value)
        }

        val commissionDoc = commissionCol.find().firstOrNull()
            ?: return Resource.Error<CreateSubAccountRes>("Commission rate is not configured. Please contact support.")
                .toDefaultResponse(failedStatusCode = HttpStatusCode.UnprocessableEntity.value)

        val rate = commissionDoc.getDouble("commissionRate")
            ?: return Resource.Error<CreateSubAccountRes>("Commission rate is invalid. Please contact support.")
                .toDefaultResponse(failedStatusCode = HttpStatusCode.UnprocessableEntity.value)

        val accountNumber = detailsDoc.getString("accountNumber") ?: ""
        val bankCode = detailsDoc.getString("bankCode") ?: ""
        val accountName = detailsDoc.getString("accountName") ?: ""

        val result = paystackRepository.createSubAccount(
            CreateSubAccountReq(
                accountNumber = accountNumber,
                businessName = accountName,
                percentageCharge = rate,
                settlementBank = bankCode,
            )
        )

        if (result is Resource.Success) {
            val code = result.data!!.data.subaccountCode
            runCatching {
                paymentDetailsCol.updateOne(
                    Filters.eq("doctorId", doctorId),
                    Updates.set("paystackSubaccountCode", code),
                )
            }.onFailure { log.error("Failed to persist subaccountCode=$code for doctorId=$doctorId", it) }
        }

        return result.toDefaultResponse(successStatusCode = HttpStatusCode.Created.value) { it }
    }
}
