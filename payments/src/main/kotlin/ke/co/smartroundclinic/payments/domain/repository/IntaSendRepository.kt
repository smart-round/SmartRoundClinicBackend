package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
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

interface IntaSendRepository {
    suspend fun createSendMoneyRequest(idNumber:String,body: CreateSendMoneyRequestReq): Resource<CreateSendMoneyRequestRes>
    suspend fun approveSendMoneyRequest(body: ApproveSendMoneyRequestReq): Resource<ApproveSendMoneyRequestRes>
    suspend fun checkSendMoneyStatus(body: CheckSendMoneyStatusReq): Resource<CheckSendMoneyStatusRes>
    suspend fun stkPush(body: STKPushReq): Resource<STKPushRes>
    suspend fun getPaymentStatus(body: GetPaymentStatusReq): Resource<GetPaymentStatusRes>

    /** Native refund — reverses the original collection transaction by invoice_id. Used for refund payouts. */
    suspend fun createChargeback(body: CreateChargebackRequestReq): Resource<CreateChargebackRequestRes>

    /** Creates a WORKING IntaSend wallet (e.g. one per doctor, or the platform collections wallet). */
    suspend fun createWallet(body: CreateWalletReq): Resource<Wallet>

    /** Live wallet balance/state — the authoritative source for "available balance", not a local computation. */
    suspend fun getWallet(walletId: String): Resource<Wallet>

    /** Wallet-to-wallet transfer, e.g. collections wallet -> doctor wallet / commission wallet. */
    suspend fun internalTransfer(originWalletId: String, body: IntraTransferReq): Resource<IntraTransferRes>
}
