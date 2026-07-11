package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.instasend.request.ApproveSendMoneyRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CheckSendMoneyStatusReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateChargebackRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateSendMoneyRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.GetPaymentStatusReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.STKPushReq
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.ApproveSendMoneyRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CheckSendMoneyStatusRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CreateChargebackRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CreateSendMoneyRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.GetPaymentStatusRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.STKPushRes

interface IntaSendRepository {
    suspend fun createSendMoneyRequest(idNumber:String,body: CreateSendMoneyRequestReq): Resource<CreateSendMoneyRequestRes>
    suspend fun approveSendMoneyRequest(body: ApproveSendMoneyRequestReq): Resource<ApproveSendMoneyRequestRes>
    suspend fun checkSendMoneyStatus(body: CheckSendMoneyStatusReq): Resource<CheckSendMoneyStatusRes>
    suspend fun stkPush(body: STKPushReq): Resource<STKPushRes>
    suspend fun getPaymentStatus(body: GetPaymentStatusReq): Resource<GetPaymentStatusRes>

    /** Native refund — reverses the original collection transaction by invoice_id. Used for refund payouts. */
    suspend fun createChargeback(body: CreateChargebackRequestReq): Resource<CreateChargebackRequestRes>
}
