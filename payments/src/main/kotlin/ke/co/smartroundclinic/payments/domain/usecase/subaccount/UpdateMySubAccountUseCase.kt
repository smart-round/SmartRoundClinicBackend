package ke.co.smartroundclinic.payments.domain.usecase.subaccount

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.dto.request.UpdateSubAccountReq

import ke.co.smartroundclinic.payments.data.remote.dto.response.UpdateSubAccountRes
import ke.co.smartroundclinic.payments.domain.repository.PaystackRepository
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document

class UpdateMySubAccountUseCase(
    private val paystackRepository: PaystackRepository,
    doctorDb: MongoDatabase,
) {
    private val paymentDetailsCol = doctorDb.getCollection<Document>(MongoDBConstants.DOCTOR_PAYMENT_DETAILS)

    suspend operator fun invoke(doctorId: String, req: UpdateSubAccountReq): DefaultResponse<UpdateSubAccountRes?> {
        val doc = paymentDetailsCol.find(Filters.eq("doctorId", doctorId)).firstOrNull()
            ?: return Resource.Error<UpdateSubAccountRes>("Payment details not found for this doctor")
                .toDefaultResponse(failedStatusCode = HttpStatusCode.NotFound.value) { it }

        val code = doc.getString("paystackSubaccountCode")
            ?: return Resource.Error<UpdateSubAccountRes>("No Paystack subaccount linked to this account")
                .toDefaultResponse(failedStatusCode = HttpStatusCode.UnprocessableEntity.value) { it }

        return paystackRepository.updateSubAccount(code, req).toDefaultResponse { it }
    }
}
