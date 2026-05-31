package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.request.UpdatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaginatedPaymentLinksRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentLinkRes
import ke.co.smartroundclinic.payments.data.remote.instasend.request.ApproveSendMoneyRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CheckSendMoneyStatusReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateSendMoneyRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.ApproveSendMoneyRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CheckSendMoneyStatusRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CreateSendMoneyRequestRes

interface IntaSendRepository {
    suspend fun listPaymentLinks(page: Int = 1): Resource<PaginatedPaymentLinksRes>
    suspend fun createPaymentLink(body: CreatePaymentLinkReq): Resource<PaymentLinkRes>
    suspend fun getPaymentLink(id: String): Resource<PaymentLinkRes>
    suspend fun updatePaymentLink(id: String, body: UpdatePaymentLinkReq): Resource<PaymentLinkRes>
    suspend fun createSendMoneyRequest(idNumber:String,body: CreateSendMoneyRequestReq): Resource<CreateSendMoneyRequestRes>
    suspend fun approveSendMoneyRequest(body: ApproveSendMoneyRequestReq): Resource<ApproveSendMoneyRequestRes>
    suspend fun checkSendMoneyStatus(body: CheckSendMoneyStatusReq): Resource<CheckSendMoneyStatusRes>
}
