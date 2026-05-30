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
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.request.UpdatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendErrorRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaginatedPaymentLinksRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentLinkRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import org.slf4j.LoggerFactory

class IntaSendRepositoryImpl(
    private val http: HttpClient,
    private val config: IntaSendConfig,
) : IntaSendRepository {

    private val log = LoggerFactory.getLogger(IntaSendRepositoryImpl::class.java)

    private fun HttpRequestBuilder.auth() {
        header(HttpHeaders.Authorization, "Bearer ${config.secretKey}")
        contentType(ContentType.Application.Json)
    }

    /** Turns a relative IntaSend path like /pay/{id}/ into a full URL. */
    private fun PaymentLinkRes.withFullUrl() = url?.let { u ->
        if (u.startsWith("http")) this else copy(url = "${config.paymentBaseUrl}$u")
    } ?: this

    override suspend fun listPaymentLinks(page: Int): Resource<PaginatedPaymentLinksRes> = try {
        val response = http.get("${config.baseUrl}/paymentlinks/") {
            auth()
            parameter("page", page)
        }
        if (response.status.isSuccess()) {
            val body = response.body<PaginatedPaymentLinksRes>()
            Resource.Success(body.copy(results = body.results.map { it.withFullUrl() }), "Payment links fetched successfully")
        } else {
            val error = response.body<IntaSendErrorRes>()
            log.warn("listPaymentLinks page=$page — IntaSend error: ${error.message()}")
            Resource.Error(error.message())
        }
    } catch (e: Exception) {
        log.error("listPaymentLinks failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch payment links")
    }

    override suspend fun createPaymentLink(body: CreatePaymentLinkReq): Resource<PaymentLinkRes> = try {
        val response = http.post("${config.baseUrl}/paymentlinks/") {
            auth()
            setBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<PaymentLinkRes>().withFullUrl(), "Payment link created successfully")
        } else {
            val error = response.body<IntaSendErrorRes>()
            log.warn("createPaymentLink title=${body.title} — IntaSend error: ${error.message()}")
            Resource.Error(error.message())
        }
    } catch (e: Exception) {
        log.error("createPaymentLink failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to create payment link")
    }

    override suspend fun getPaymentLink(id: String): Resource<PaymentLinkRes> = try {
        val response = http.get("${config.baseUrl}/paymentlinks/$id/") {
            auth()
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<PaymentLinkRes>().withFullUrl(), "Payment link fetched successfully")
        } else {
            val error = response.body<IntaSendErrorRes>()
            log.warn("getPaymentLink($id) — IntaSend error: ${error.message()}")
            Resource.Error(error.message())
        }
    } catch (e: Exception) {
        log.error("getPaymentLink($id) failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to fetch payment link")
    }

    override suspend fun updatePaymentLink(id: String, body: UpdatePaymentLinkReq): Resource<PaymentLinkRes> = try {
        val response = http.put("${config.baseUrl}/paymentlinks/$id/") {
            auth()
            setBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body<PaymentLinkRes>().withFullUrl(), "Payment link updated successfully")
        } else {
            val error = response.body<IntaSendErrorRes>()
            log.warn("updatePaymentLink($id) — IntaSend error: ${error.message()}")
            Resource.Error(error.message())
        }
    } catch (e: Exception) {
        log.error("updatePaymentLink($id) failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to update payment link")
    }
}
