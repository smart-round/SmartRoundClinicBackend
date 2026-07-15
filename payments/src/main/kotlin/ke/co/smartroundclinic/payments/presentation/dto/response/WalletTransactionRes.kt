package ke.co.smartroundclinic.payments.presentation.dto.response

import ke.co.smartroundclinic.payments.data.remote.instasend.reseponse.WalletTransactionItem
import kotlinx.serialization.Serializable

@Serializable
data class WalletTransactionItemRes(
    val transactionId: String,
    val invoice: String?,
    val currency: String,
    val value: Double,
    val runningBalance: Double,
    val narrative: String?,
    val transType: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String?,
)

@Serializable
data class WalletTransactionsPageRes(
    val items: List<WalletTransactionItemRes>,
    val total: Int,
    val page: Int,
    val hasMore: Boolean,
)

fun WalletTransactionItem.toRes(): WalletTransactionItemRes = WalletTransactionItemRes(
    transactionId = transactionId,
    invoice = invoice?.invoiceId,
    currency = currency,
    value = value,
    runningBalance = runningBalance,
    narrative = narrative,
    transType = transType,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
