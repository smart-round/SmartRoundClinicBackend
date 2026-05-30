package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreatePaymentLinkReq
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CreatePaymentLinkRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.GetPaymentLinkRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.GetPaymentLinksRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.UpdatePaymentLinkRes

interface InstaSendRepository {
    suspend fun createPaymentLink(body: CreatePaymentLinkReq): Resource<CreatePaymentLinkRes>
    suspend fun updatePaymentLink(id: String, body: CreatePaymentLinkReq): Resource<UpdatePaymentLinkRes>
    suspend fun getPaymentLink(id: String): Resource<GetPaymentLinkRes>
    suspend fun getPaymentLinks(): Resource<GetPaymentLinksRes>
}
