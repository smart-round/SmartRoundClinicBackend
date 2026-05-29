package ke.co.smartroundclinic.payments.data.repository

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import ke.co.smartroundclinic.common.MongoDBConstants
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.common.SubAccountCreator
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreateSubAccountReq
import ke.co.smartroundclinic.payments.data.remote.dto.request.UpdateSubAccountReq
import ke.co.smartroundclinic.payments.domain.repository.PaystackRepository
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory

class PaystackSubAccountCreatorImpl(
    private val paystackRepository: PaystackRepository,
    adminDb: MongoDatabase,
    doctorDb: MongoDatabase,
) : SubAccountCreator {

    private val log = LoggerFactory.getLogger(PaystackSubAccountCreatorImpl::class.java)
    private val commissionCol = adminDb.getCollection<Document>(MongoDBConstants.ADMIN_COMMISSION_RATES)
    private val paymentDetailsCol = doctorDb.getCollection<Document>(MongoDBConstants.DOCTOR_PAYMENT_DETAILS)

    override suspend fun createForDoctor(
        businessName: String,
        settlementBank: String,
        accountNumber: String,
    ): Result<String> {
        val commissionDoc = commissionCol.find().firstOrNull()
            ?: return Result.failure(Exception("No commission rate configured"))

        val rate = commissionDoc.getDouble("commissionRate")
            ?: return Result.failure(Exception("Commission rate is missing or invalid"))

        return when (val result = paystackRepository.createSubAccount(
            CreateSubAccountReq(
                accountNumber = accountNumber,
                businessName = businessName,
                percentageCharge = rate,
                settlementBank = settlementBank,
            )
        )) {
            is Resource.Success -> {
                val code = result.data!!.data.subaccountCode
                log.info("Paystack subaccount created: code=$code businessName=$businessName")
                Result.success(code)
            }
            is Resource.Error -> {
                log.warn("Paystack subaccount creation failed: ${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }

    override suspend fun syncForDoctor(
        doctorId: String,
        accountNumber: String?,
        settlementBank: String?,
        businessName: String?,
    ): Result<Unit> {
        if (accountNumber == null && settlementBank == null && businessName == null) return Result.success(Unit)

        val doc = paymentDetailsCol.find(Filters.eq("doctorId", doctorId)).firstOrNull()
        val code = doc?.getString("paystackSubaccountCode") ?: return Result.success(Unit)

        return when (val result = paystackRepository.updateSubAccount(
            code,
            UpdateSubAccountReq(
                accountNumber = accountNumber,
                settlementBank = settlementBank,
                businessName = businessName,
            )
        )) {
            is Resource.Success -> {
                log.info("Paystack subaccount synced: code=$code doctorId=$doctorId")
                Result.success(Unit)
            }
            is Resource.Error -> {
                log.warn("Paystack subaccount sync failed for doctorId=$doctorId: ${result.message}")
                Result.failure(Exception(result.message))
            }
        }
    }
}
