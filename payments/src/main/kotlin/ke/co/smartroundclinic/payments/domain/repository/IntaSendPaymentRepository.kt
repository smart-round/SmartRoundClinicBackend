package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.dto.request.MpesaStkPushReq
import ke.co.smartroundclinic.payments.data.remote.dto.response.PaymentSessionRes

interface IntaSendPaymentRepository {
    suspend fun initiateMpesaStkPush(body: MpesaStkPushReq): Resource<PaymentSessionRes>
}
