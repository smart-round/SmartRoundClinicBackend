package ke.co.smartroundclinic.payments.presentation.dto.response

import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.SendMoneyTransactionItem
import kotlinx.serialization.Serializable

/**
 * Doctor-facing withdrawal record, sourced live from IntaSend's send-money/transactions —
 * distinct from the admin-facing WithdrawalItemRes (in AdminPaymentsRes.kt), which still reads
 * our own local WithdrawalEntity audit log.
 */
@Serializable
data class DoctorWithdrawalItemRes(
    val transactionId: String,
    val status: String,
    val statusCode: String?,
    val statusDescription: String?,
    val provider: String?,
    val bankCode: String?,
    val name: String?,
    val account: String?,
    val amount: String,
    val charge: String?,
    val narrative: String?,
    val currency: String,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class DoctorWithdrawalsPageRes(
    val items: List<DoctorWithdrawalItemRes>,
    val total: Int,
    val page: Int,
    val hasMore: Boolean,
)

fun SendMoneyTransactionItem.toRes(): DoctorWithdrawalItemRes = DoctorWithdrawalItemRes(
    transactionId = transactionId,
    status = status,
    statusCode = statusCode,
    statusDescription = statusDescription,
    provider = provider,
    bankCode = bankCode,
    name = name,
    account = account,
    amount = amount,
    charge = charge,
    narrative = narrative,
    currency = currency,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
