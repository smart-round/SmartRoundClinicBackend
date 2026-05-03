package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.LocalBankEntity

interface LocalBankRepository {
    suspend fun getAll(): Resource<List<LocalBankEntity>>
    suspend fun searchByName(query: String, page: Int, size: Int): Resource<Pair<List<LocalBankEntity>, Long>>
    suspend fun findByBankCode(bankCode: String): Resource<LocalBankEntity?>
    suspend fun findByBranchCode(branchCode: String): Resource<List<LocalBankEntity>>
}