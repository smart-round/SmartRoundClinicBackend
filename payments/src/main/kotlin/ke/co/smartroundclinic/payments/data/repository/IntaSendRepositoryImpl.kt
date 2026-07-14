package ke.co.smartroundclinic.payments.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendErrorRes
import ke.co.smartroundclinic.payments.data.remote.instasend.request.ApproveSendMoneyRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CheckSendMoneyStatusReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateChargebackRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateSendMoneyRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateWalletReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.GetPaymentStatusReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.IntraTransferReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.STKPushReq
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.ApproveSendMoneyRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CheckSendMoneyStatusRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CreateChargebackRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CreateSendMoneyRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.GetPaymentStatusRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.IntraTransferRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.STKPushRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.Wallet
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import io.ktor.client.request.get
import org.slf4j.LoggerFactory

class IntaSendRepositoryImpl(
    private val http: HttpClient,
    private val config: IntaSendConfig,
) : IntaSendRepository {

    private val log = LoggerFactory.getLogger(IntaSendRepositoryImpl::class.java)

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private fun HttpRequestBuilder.auth() {
        header(HttpHeaders.Authorization, "Bearer ${config.secretKey}")
        contentType(ContentType.Application.Json)
    }

    private inline fun <reified T> HttpRequestBuilder.jsonBody(value: T) {
        val encoded = json.encodeToString(value)
        log.debug("IntaSend outgoing body: $encoded")
        setBody(encoded.toByteArray())
        contentType(ContentType.Application.Json)
    }

    override suspend fun createSendMoneyRequest(
        idNumber: String,
        body: CreateSendMoneyRequestReq,
    ): Resource<CreateSendMoneyRequestRes> = try {
        val outgoing = body.copy(transactions = body.transactions.map { it.addIdNumber(idNumber) })
        val response = http.post("${config.baseUrl}/send-money/initiate/") {
            auth()
            jsonBody(outgoing)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<CreateSendMoneyRequestRes>(), "Send money request created successfully")
        } else {
            val error = response.body<IntaSendErrorRes>()
            log.warn("createSendMoneyRequest idNumber=$idNumber — IntaSend error: ${error.message()}")
            Resource.Error(error.message())
        }
    } catch (e: Exception) {
        log.error("createSendMoneyRequest failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to create send money request")
    }

    override suspend fun approveSendMoneyRequest(body: ApproveSendMoneyRequestReq): Resource<ApproveSendMoneyRequestRes> = try {
        val response = http.post("${config.baseUrl}/send-money/approve/") {
            auth()
            jsonBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<ApproveSendMoneyRequestRes>(), "Send money request approved successfully")
        } else {
            val error = response.body<IntaSendErrorRes>()
            log.warn("approveSendMoneyRequest trackingId=${body.trackingId} — IntaSend error: ${error.message()}")
            Resource.Error(error.message())
        }
    } catch (e: Exception) {
        log.error("approveSendMoneyRequest failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to approve send money request")
    }

    override suspend fun checkSendMoneyStatus(
        body: CheckSendMoneyStatusReq,
    ): Resource<CheckSendMoneyStatusRes> = try {
        val response = http.post("${config.baseUrl}/send-money/status") {
            auth()
            jsonBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<CheckSendMoneyStatusRes>(), "Send money status fetched successfully")
        } else {
            val error = response.body<IntaSendErrorRes>()
            log.warn("checkSendMoneyStatus trackingId=${body.trackingId} — IntaSend error: ${error.message()}")
            Resource.Error(error.message())
        }
    } catch (e: Exception) {
        log.error("checkSendMoneyStatus(${body.trackingId}) failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to check send money status")
    }

    override suspend fun stkPush(body: STKPushReq): Resource<STKPushRes> = try {
        val response = http.post("${config.baseUrl}/payment/mpesa-stk-push/") {
            auth()
            jsonBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<STKPushRes>(), "STK push initiated successfully")
        } else {
            val errorMessage = runCatching { response.body<IntaSendErrorRes>().message() }
                .getOrElse { "STK push failed — HTTP ${response.status.value}" }
            log.warn("stkPush apiRef=${body.apiRef} — IntaSend error: $errorMessage")
            Resource.Error(errorMessage)
        }
    } catch (e: Exception) {
        log.error("stkPush(${body.apiRef}) failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to initiate STK push")
    }

    override suspend fun getPaymentStatus(body: GetPaymentStatusReq): Resource<GetPaymentStatusRes>  = try {
        val response = http.post("${config.baseUrl}/payment/status/") {
            auth()
            jsonBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<GetPaymentStatusRes>(), "Send money status fetched successfully")
        } else {
            val error = response.body<IntaSendErrorRes>()
            log.warn("checkSendMoneyStatus trackingId=${body.invoiceId} — IntaSend error: ${error.message()}")
            Resource.Error(error.message())
        }
    } catch (e: Exception) {
        log.error("checkSendMoneyStatus(${body.invoiceId}) failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to check send money status")
    }

    override suspend fun createChargeback(body: CreateChargebackRequestReq): Resource<CreateChargebackRequestRes> = try {
        val response = http.post("${config.baseUrl}/chargebacks/") {
            auth()
            jsonBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<CreateChargebackRequestRes>(), "Chargeback created successfully")
        } else {
            val errorMessage = runCatching { response.body<IntaSendErrorRes>().message() }
                .getOrElse { "Chargeback creation failed — HTTP ${response.status.value}" }
            log.warn("createChargeback invoiceId=${body.invoiceId} — IntaSend error: $errorMessage")
            Resource.Error(errorMessage)
        }
    } catch (e: Exception) {
        log.error("createChargeback(${body.invoiceId}) failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to create chargeback")
    }

    override suspend fun createWallet(body: CreateWalletReq): Resource<Wallet> = try {
        val response = http.post("${config.baseUrl}/wallets/") {
            auth()
            jsonBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<Wallet>(), "Wallet created successfully")
        } else {
            val errorMessage = runCatching { response.body<IntaSendErrorRes>().message() }
                .getOrElse { "Wallet creation failed — HTTP ${response.status.value}" }
            log.warn("createWallet label=${body.label} — IntaSend error: $errorMessage")
            Resource.Error(errorMessage)
        }
    } catch (e: Exception) {
        log.error("createWallet(${body.label}) failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to create wallet")
    }

    override suspend fun getWallet(walletId: String): Resource<Wallet> = try {
        val response = http.get("${config.baseUrl}/wallets/$walletId/") {
            auth()
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<Wallet>(), "Wallet fetched successfully")
        } else {
            val errorMessage = runCatching { response.body<IntaSendErrorRes>().message() }
                .getOrElse { "Failed to fetch wallet — HTTP ${response.status.value}" }
            log.warn("getWallet walletId=$walletId — IntaSend error: $errorMessage")
            Resource.Error(errorMessage)
        }
    } catch (e: Exception) {
        log.error("getWallet($walletId) failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch wallet")
    }

    override suspend fun internalTransfer(originWalletId: String, body: IntraTransferReq): Resource<IntraTransferRes> = try {
        val response = http.post("${config.baseUrl}/wallets/$originWalletId/intra_transfer/") {
            auth()
            jsonBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<IntraTransferRes>(), "Internal transfer completed successfully")
        } else {
            val errorMessage = runCatching { response.body<IntaSendErrorRes>().message() }
                .getOrElse { "Internal transfer failed — HTTP ${response.status.value}" }
            log.warn("internalTransfer origin=$originWalletId destination=${body.destinationWalletId} — IntaSend error: $errorMessage")
            Resource.Error(errorMessage)
        }
    } catch (e: Exception) {
        log.error("internalTransfer origin=$originWalletId failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to perform internal transfer")
    }
}
