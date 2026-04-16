package ke.co.smartroundclinic.doctor.presentation.dto.response

import ke.co.smartroundclinic.doctor.domain.model.LocalBank
import ke.co.smartroundclinic.doctor.domain.model.LocalBankBranch
import kotlinx.serialization.Serializable

@Serializable
data class LocalBankBranchRes(
    val branchCode: String,
    val branchName: String,
)

@Serializable
data class LocalBankRes(
    val id: String,
    val bankName: String,
    val bankCode: String,
    val branches: List<LocalBankBranchRes>,
)

@Serializable
data class LocalBankPageResult(
    val items: List<LocalBankRes>,
    val total: Long,
    val page: Int,
    val size: Int,
    val pages: Long,
)

fun LocalBankBranch.toRes() = LocalBankBranchRes(branchCode = branchCode, branchName = branchName)

fun LocalBank.toRes() = LocalBankRes(
    id = id,
    bankName = bankName,
    bankCode = bankCode,
    branches = branches.map { it.toRes() },
)