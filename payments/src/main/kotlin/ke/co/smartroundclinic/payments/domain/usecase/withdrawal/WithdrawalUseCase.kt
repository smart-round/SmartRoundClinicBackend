package ke.co.smartroundclinic.payments.domain.usecase.withdrawal

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.IntaSendConfig
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity
import ke.co.smartroundclinic.payments.data.entity.WithdrawalTransactionRecord
import ke.co.smartroundclinic.payments.data.lookup.DoctorPaymentDetailsLookup
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateSendMoneyRequestReq
import ke.co.smartroundclinic.payments.data.remote.instasend.request.CreateSendMoneyTransaction
import ke.co.smartroundclinic.payments.domain.repository.IntaSendRepository
import ke.co.smartroundclinic.payments.domain.repository.WithdrawalRepository
import ke.co.smartroundclinic.payments.domain.service.DoctorWalletResolver
import ke.co.smartroundclinic.payments.presentation.dto.request.WithdrawInitiateReq
import ke.co.smartroundclinic.payments.presentation.dto.response.InsufficientBalanceRes
import ke.co.smartroundclinic.payments.presentation.dto.response.WithdrawInitiateRes
import org.slf4j.LoggerFactory

private const val MIN_WITHDRAWAL_KES = 100.0
private const val WITHDRAWAL_CURRENCY = "KES"
private const val WITHDRAWAL_PROVIDER = "PESALINK"

/** Matches the fee IntaSend embeds in its own insufficient-balance rejection text, e.g.
 * "...Transaction charge estimate is KES 100.00. Your current balance is KES 124.40" */
private val FEE_ESTIMATE_REGEX = Regex("""charge estimate is KES\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)

private fun parseFeeEstimate(message: String): Double? =
    FEE_ESTIMATE_REGEX.find(message)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

/**
 * Balance authority lives entirely in IntaSend now: a doctor's wallet only ever holds money
 * that's already been credited to them post-commission (see CreditDoctorEarningsUseCase), so
 * "available balance" is read live from the wallet rather than recomputed from our own DB — this
 * is what closes the check-then-act race that let concurrent/retried requests over-withdraw.
 * An application-level per-doctor lock (WithdrawalRepository.acquireLock) additionally prevents
 * two disbursement calls from ever being fired concurrently in the first place.
 */
class WithdrawalUseCase(
    private val intaSendRepository: IntaSendRepository,
    private val withdrawalRepository: WithdrawalRepository,
    private val walletResolver: DoctorWalletResolver,
    private val paymentDetailsLookup: DoctorPaymentDetailsLookup,
    private val config: IntaSendConfig,
) {
    private val log = LoggerFactory.getLogger(WithdrawalUseCase::class.java)

    suspend operator fun invoke(doctorId: String, req: WithdrawInitiateReq): DefaultResponse<WithdrawInitiateRes?> {
        // 1. Fetch doctor's registered bank account details
        val paymentDetails = paymentDetailsLookup.getByDoctorId(doctorId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.UnprocessableEntity.value,
                status = false,
                message = "No bank account details found. Please add your payment details before withdrawing.",
                data = null,
            )

        // 2. Resolve the doctor's IntaSend wallet (lazily provisioned if it predates wallet support)
        val walletId = walletResolver.resolve(doctorId)
            ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = "Unable to reach your wallet right now. Please try again shortly.",
                data = null,
            )

        // 3. Minimum withdrawal limit
        if (req.amount < MIN_WITHDRAWAL_KES) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadRequest.value,
                status = false,
                message = "Minimum withdrawal amount is KES $MIN_WITHDRAWAL_KES",
                data = null,
            )
        }

        // 4. Reserve the per-doctor in-flight lock before touching IntaSend at all
        val lockAcquired = (withdrawalRepository.acquireLock(doctorId) as? Resource.Success)?.data ?: false
        if (!lockAcquired) {
            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Conflict.value,
                status = false,
                message = "A withdrawal is already in progress. Please wait for it to complete.",
                data = null,
            )
        }

        try {
            // 5. Live balance check — sourced from IntaSend's own ledger, not a local computation
            val walletResult = intaSendRepository.getWallet(walletId)
            val wallet = (walletResult as? Resource.Success)?.data
                ?: return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadGateway.value,
                    status = false,
                    message = (walletResult as? Resource.Error)?.message ?: "Failed to fetch wallet balance",
                    data = null,
                )

            if (req.amount > wallet.availableBalance) {
                return DefaultResponse(
                    httpStatusCode = HttpStatusCode.BadRequest.value,
                    status = false,
                    message = "Insufficient balance. Available: KES ${"%.2f".format(wallet.availableBalance)}, requested: KES ${"%.2f".format(req.amount)}",
                    data = null,
                )
            }

            // 6. Initiate disbursement with IntaSend, scoped to the doctor's own wallet
            val amountStr = "%.2f".format(req.amount)
            val initiateResult = intaSendRepository.createSendMoneyRequest(
                idNumber = req.idNumber,
                body = CreateSendMoneyRequestReq(
                    callbackUrl = "${config.callBackBaseUrl}/payments/instasend/withdrawal/callback",
                    currency = WITHDRAWAL_CURRENCY,
                    provider = WITHDRAWAL_PROVIDER,
                    walletId = walletId,
                    transactions = listOf(
                        CreateSendMoneyTransaction(
                            account = paymentDetails.accountNumber,
                            amount = amountStr,
                            bankCode = paymentDetails.bankCode,
                            idNumber = req.idNumber,
                            name = paymentDetails.accountName,
                            narrative = "Doctor withdrawal - ${paymentDetails.accountName}",
                        )
                    ),
                )
            )

            if (initiateResult !is Resource.Success) {
                val providerMessage = (initiateResult as? Resource.Error)?.message
                    ?: "Failed to initiate withdrawal with payment provider"
                // IntaSend's own balance check (amount + its transfer fee vs. wallet balance) is a
                // doctor-correctable input problem, not an upstream/system failure — surface it as
                // a normal validation error (400) instead of a 502 that reads as "try again later".
                val isInsufficientBalance = providerMessage.contains("insufficient balance", ignoreCase = true)
                val feeEstimate = if (isInsufficientBalance) parseFeeEstimate(providerMessage) else null
                return DefaultResponse(
                    httpStatusCode = if (isInsufficientBalance) HttpStatusCode.BadRequest.value else HttpStatusCode.BadGateway.value,
                    status = false,
                    message = providerMessage,
                    data = feeEstimate?.let {
                        WithdrawInitiateRes(
                            insufficientBalance = InsufficientBalanceRes(
                                requestedAmount = req.amount,
                                feeEstimate = it,
                                totalRequired = req.amount + it,
                                availableBalance = wallet.availableBalance,
                            )
                        )
                    },
                )
            }

            val initiated = initiateResult.data ?: return DefaultResponse(
                httpStatusCode = HttpStatusCode.BadGateway.value,
                status = false,
                message = "Payment provider returned an empty response",
                data = null,
            )

            // 7. IntaSend auto-approves send-money requests on this account — no separate
            // /send-money/approve/ call needed; the initiate response is the final word.
            val withdrawalEntity = WithdrawalEntity(
                doctorId = doctorId,
                amount = req.amount,
                currency = WITHDRAWAL_CURRENCY,
                trackingId = initiated.trackingId,
                status = WithdrawalEntity.WithdrawalStatus.PENDING.name,
                provider = WITHDRAWAL_PROVIDER,
                platformCommission = 0.0,
                transactions = listOf(
                    WithdrawalTransactionRecord(
                        account = paymentDetails.accountNumber,
                        amount = amountStr,
                        bankCode = paymentDetails.bankCode,
                        name = paymentDetails.accountName,
                    )
                ),
            )
            runCatching { withdrawalRepository.save(withdrawalEntity) }
                .onFailure { log.error("Failed to persist withdrawal record doctorId=$doctorId — ${it.message}", it) }

            return DefaultResponse(
                httpStatusCode = HttpStatusCode.Created.value,
                status = true,
                message = "Withdrawal initiated successfully",
                data = WithdrawInitiateRes(trackingId = initiated.trackingId, status = initiated.status),
            )
        } finally {
            withdrawalRepository.releaseLock(doctorId)
        }
    }
}
