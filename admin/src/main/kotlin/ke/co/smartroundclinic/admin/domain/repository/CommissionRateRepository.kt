package ke.co.smartroundclinic.admin.domain.repository

import ke.co.smartroundclinic.admin.data.entity.CommissionRateEntity
import ke.co.smartroundclinic.common.Resource

interface CommissionRateRepository {
    suspend fun create(entity: CommissionRateEntity): Resource<CommissionRateEntity?>
    suspend fun getById(): Resource<CommissionRateEntity?>
    suspend fun getAll(page: Int, size: Int): Resource<Pair<List<CommissionRateEntity>, Long>>
    suspend fun update(id: String, commissionRate: Double?): Resource<CommissionRateEntity?>
    suspend fun delete(id: String): Resource<CommissionRateEntity?>
}
