package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.PlatformCommissionLogEntity

interface PlatformCommissionLogRepository {
    suspend fun save(entity: PlatformCommissionLogEntity): Resource<PlatformCommissionLogEntity>
    suspend fun getByDoctorId(doctorId: String): Resource<List<PlatformCommissionLogEntity>>
    suspend fun getByWithdrawalId(withdrawalId: String): Resource<PlatformCommissionLogEntity?>
    suspend fun getAllForAdmin(): Resource<List<PlatformCommissionLogEntity>>
    suspend fun getAll(page: Int, size: Int): Resource<Pair<List<PlatformCommissionLogEntity>, Long>>
}
