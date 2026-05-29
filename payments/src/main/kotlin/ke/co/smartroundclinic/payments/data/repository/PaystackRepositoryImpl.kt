package ke.co.smartroundclinic.payments.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.PaystackConfig
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreateSubAccountReq
import ke.co.smartroundclinic.payments.data.remote.dto.request.UpdateSubAccountReq
import ke.co.smartroundclinic.payments.data.remote.dto.request.ValidateAccountReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.CreateSubAccountRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.GetSubAccountRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.ListSubAccountsRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaystackErrorRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.ResolveAccountRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.ResolveCardBinRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.UpdateSubAccountRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.ValidateAccountRes
import ke.co.smartroundclinic.payments.domain.repository.PaystackRepository
import org.slf4j.LoggerFactory

class PaystackRepositoryImpl(
    private val http: HttpClient,
    private val config: PaystackConfig,
) : PaystackRepository {

    private val log = LoggerFactory.getLogger(PaystackRepositoryImpl::class.java)

    private fun HttpRequestBuilder.auth() {
        header(HttpHeaders.Authorization, "Bearer ${config.secretKey}")
        contentType(ContentType.Application.Json)
    }

    override suspend fun createSubAccount(body: CreateSubAccountReq): Resource<CreateSubAccountRes> = try {
        val response = http.post("${config.baseUrl}/subaccount") {
            auth()
            setBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body(), "Sub-account created successfully")
        } else {
            val error = response.body<PaystackErrorRes>()
            log.warn("createSubAccount — Paystack error: ${error.message}")
            Resource.Error(error.message)
        }
    } catch (e: Exception) {
        log.error("createSubAccount failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to create sub-account")
    }

    override suspend fun updateSubAccount(idOrCode:String, body: UpdateSubAccountReq): Resource<UpdateSubAccountRes> = try {
        val response = http.put("${config.baseUrl}/subaccount/$idOrCode") {
            auth()
            setBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body(), "Sub-account updated successfully")
        } else {
            val error = response.body<PaystackErrorRes>()
            log.warn("updateSubAccount($idOrCode) — Paystack error: ${error.message}")
            Resource.Error(error.message)
        }
    } catch (e: Exception) {
        log.error("updateSubAccount failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update sub-account")
    }

    override suspend fun listSubAccounts(perPage: Int, page: Int, from: String?, to: String?): Resource<ListSubAccountsRes> = try {
        val response = http.get("${config.baseUrl}/subaccount") {
            auth()
            parameter("perPage", perPage)
            parameter("page", page)
            if (from != null) parameter("from", from)
            if (to != null) parameter("to", to)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body())
        } else {
            val error = response.body<PaystackErrorRes>()
            log.warn("listSubAccounts — Paystack error: ${error.message}")
            Resource.Error(error.message)
        }
    } catch (e: Exception) {
        log.error("listSubAccounts failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to list sub-accounts")
    }

    override suspend fun getSubAccount(idOrCode: String): Resource<GetSubAccountRes> = try {
        val response = http.get("${config.baseUrl}/subaccount/$idOrCode") {
            auth()
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body())
        } else {
            val error = response.body<PaystackErrorRes>()
            log.warn("getSubAccount($idOrCode) — Paystack error: ${error.message}")
            Resource.Error(error.message)
        }
    } catch (e: Exception) {
        log.error("getSubAccount($idOrCode) failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch sub-account")
    }

    override suspend fun resolveAccount(accountNumber: String, bankCode: String): Resource<ResolveAccountRes> = try {
        val response = http.get("${config.baseUrl}/bank/resolve") {
            auth()
            parameter("account_number", accountNumber)
            parameter("bank_code", bankCode)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body(), "Account number resolved")
        } else {
            val error = response.body<PaystackErrorRes>()
            log.warn("resolveAccount($accountNumber, $bankCode) — Paystack error: ${error.message}")
            Resource.Error(error.message)
        }
    } catch (e: Exception) {
        log.error("resolveAccount failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to resolve account number")
    }

    override suspend fun validateAccount(body: ValidateAccountReq): Resource<ValidateAccountRes> = try {
        val response = http.post("${config.baseUrl}/bank/validate") {
            auth()
            setBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body(), "Account validation attempted")
        } else {
            val error = response.body<PaystackErrorRes>()
            log.warn("validateAccount(${body.accountNumber}) — Paystack error: ${error.message}")
            Resource.Error(error.message)
        }
    } catch (e: Exception) {
        log.error("validateAccount failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to validate account")
    }

    override suspend fun resolveCardBin(bin: String): Resource<ResolveCardBinRes> = try {
        val response = http.get("${config.baseUrl}/decision/bin/$bin") {
            auth()
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body(), "Bin resolved")
        } else {
            val error = response.body<PaystackErrorRes>()
            log.warn("resolveCardBin($bin) — Paystack error: ${error.message}")
            Resource.Error(error.message)
        }
    } catch (e: Exception) {
        log.error("resolveCardBin($bin) failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to resolve card BIN")
    }
}
