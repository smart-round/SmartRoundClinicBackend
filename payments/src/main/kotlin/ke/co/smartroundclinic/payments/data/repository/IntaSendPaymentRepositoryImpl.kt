package ke.co.smartroundclinic.payments.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.remote.dto.request.MpesaStkPushReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.IntaSendErrorRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentSessionRes
import ke.co.smartroundclinic.payments.domain.repository.IntaSendPaymentRepository
import org.slf4j.LoggerFactory

class IntaSendPaymentRepositoryImpl(
    private val http: HttpClient,
    private val config: IntaSendConfig,
) : IntaSendPaymentRepository {

    private val log = LoggerFactory.getLogger(IntaSendPaymentRepositoryImpl::class.java)

    override suspend fun initiateMpesaStkPush(body: MpesaStkPushReq): Resource<PaymentSessionRes> = try {
        val response = http.post("${config.baseUrl}/payment/mpesa-stk-push/") {
            header(HttpHeaders.Authorization, "Bearer ${config.secretKey}")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (response.status.isSuccess()) {
            Resource.Success(response.body(), "MPesa STK push initiated successfully")
        } else {
            val error = response.body<IntaSendErrorRes>()
            log.warn("initiateMpesaStkPush phone=${body.phoneNumber} — IntaSend error: ${error.message()}")
            Resource.Error(error.message())
        }
    } catch (e: Exception) {
        log.error("initiateMpesaStkPush failed — ${e.message}", e)
        Resource.Error(e.message ?: "Failed to initiate MPesa payment")
    }
}
