package ke.co.smartroundclinic.doctor.domain.repository

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.ComplianceEntity

interface ComplianceRepository {
    suspend fun submit(doctorId: String): Resource<ComplianceEntity>
    suspend fun approve(id: String, adminId: String): Resource<ComplianceEntity?>
    suspend fun reject(id: String, adminId: String, reason: String): Resource<ComplianceEntity?>
    suspend fun getByDoctorId(doctorId: String): Resource<ComplianceEntity?>
    suspend fun getById(id: String): Resource<ComplianceEntity?>
    suspend fun getAll(page: Int, size: Int, status: String? = null, doctorIds: Set<String>? = null): Resource<Pair<List<ComplianceEntity>, Long>>
}
