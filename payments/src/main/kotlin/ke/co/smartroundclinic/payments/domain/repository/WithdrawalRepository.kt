package ke.co.smartroundclinic.payments.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.payments.data.entity.WithdrawalEntity

interface WithdrawalRepository {
    suspend fun save(entity: WithdrawalEntity): Resource<WithdrawalEntity>
    suspend fun getByDoctorId(doctorId: String): Resource<List<WithdrawalEntity>>
    suspend fun getByDoctorIdPaginated(doctorId: String, page: Int, size: Int): Resource<Pair<List<WithdrawalEntity>, Long>>
    suspend fun updateStatus(trackingId: String, status: String): Resource<Unit>
    suspend fun getAllForAdmin(status: String? = null): Resource<List<WithdrawalEntity>>
    suspend fun getAll(page: Int, size: Int, status: String? = null): Resource<Pair<List<WithdrawalEntity>, Long>>
}
