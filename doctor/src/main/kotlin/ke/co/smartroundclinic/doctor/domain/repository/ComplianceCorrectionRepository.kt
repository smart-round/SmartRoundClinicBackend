package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.ComplianceCorrectionEntity

interface ComplianceCorrectionRepository {
    suspend fun create(entity: ComplianceCorrectionEntity): Resource<ComplianceCorrectionEntity>
    suspend fun hasPending(doctorId: String): Boolean
    suspend fun resolvePending(doctorId: String, status: String, reviewedBy: String): Resource<Unit>
    suspend fun getLatest(page: Int, size: Int, status: String? = null): Resource<Pair<List<ComplianceCorrectionEntity>, Long>>
    suspend fun getHistoryForDoctor(doctorId: String, page: Int, size: Int): Resource<Pair<List<ComplianceCorrectionEntity>, Long>>
}
