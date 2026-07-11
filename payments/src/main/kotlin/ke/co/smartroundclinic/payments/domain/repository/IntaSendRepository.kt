package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.remote.instasend.request.ApproveMpesaB2CRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.ApproveSendMoneyRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CheckSendMoneyStatusReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateMpesaB2CRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateSendMoneyRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.GetPaymentStatusReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.STKPushReq
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.ApproveMpesaB2CRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.ApproveSendMoneyRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CheckSendMoneyStatusRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CreateMpesaB2CRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.CreateSendMoneyRequestRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.GetPaymentStatusRes
import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.STKPushRes

interface IntaSendRepository {
    suspend fun createSendMoneyRequest(idNumber:String,body: CreateSendMoneyRequestReq): Resource<CreateSendMoneyRequestRes>
    suspend fun approveSendMoneyRequest(body: ApproveSendMoneyRequestReq): Resource<ApproveSendMoneyRequestRes>
    suspend fun checkSendMoneyStatus(body: CheckSendMoneyStatusReq): Resource<CheckSendMoneyStatusRes>
    suspend fun stkPush(body: STKPushReq): Resource<STKPushRes>
    suspend fun getPaymentStatus(body: GetPaymentStatusReq): Resource<GetPaymentStatusRes>

    /** M-Pesa B2C disbursement — used for refund payouts (account = beneficiary phone number, no bank_code). */
    suspend fun createMpesaB2CRequest(body: CreateMpesaB2CRequestReq): Resource<CreateMpesaB2CRequestRes>
    suspend fun approveMpesaB2CRequest(body: ApproveMpesaB2CRequestReq): Resource<ApproveMpesaB2CRequestRes>
}
