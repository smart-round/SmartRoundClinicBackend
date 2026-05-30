package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.dto.request.CreatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.request.UpdatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaginatedPaymentLinksRes
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentLinkRes

interface IntaSendRepository {
    suspend fun listPaymentLinks(page: Int = 1): Resource<PaginatedPaymentLinksRes>
    suspend fun createPaymentLink(body: CreatePaymentLinkReq): Resource<PaymentLinkRes>
    suspend fun getPaymentLink(id: String): Resource<PaymentLinkRes>
    suspend fun updatePaymentLink(id: String, body: UpdatePaymentLinkReq): Resource<PaymentLinkRes>
}
