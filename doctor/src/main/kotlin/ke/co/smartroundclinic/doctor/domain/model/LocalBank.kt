package ke.co.smartroundclinic.doctor.domain.model

import ke.co.smartroundclinic.doctor.data.entity.LocalBankBranchEntity
import ke.co.smartroundclinic.doctor.data.entity.LocalBankEntity

data class LocalBankBranch(
    val branchCode: String,
    val branchName: String,
)

data class LocalBank(
    val id: String,
    val bankName: String,
    val bankCode: String,
    val branches: List<LocalBankBranch>,
)

fun LocalBankEntity.toModel() = LocalBank(
    id = id,
    bankName = bankName,
    bankCode = bankCode,
    branches = branches.map { LocalBankBranch(it.branchCode, it.branchName) },
)

fun LocalBank.toEntity() = LocalBankEntity(
    id = id,
    bankName = bankName,
    bankCode = bankCode,
    branches = branches.map { LocalBankBranchEntity(it.branchCode, it.branchName) },
)