package ke.co.smartroundclinic.doctor.domain.service

import ke.co.smartroundclinic.doctor.domain.usecase.compliance.ApproveComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetAllComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetComplianceByIdUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.GetMyComplianceStatusUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.RejectComplianceUseCase
import ke.co.smartroundclinic.doctor.domain.usecase.compliance.SubmitComplianceUseCase

class ComplianceService(
    private val submitUseCase: SubmitComplianceUseCase,
    private val approveUseCase: ApproveComplianceUseCase,
    private val rejectUseCase: RejectComplianceUseCase,
    private val getMyStatusUseCase: GetMyComplianceStatusUseCase,
    private val getAllUseCase: GetAllComplianceUseCase,
    private val getByIdUseCase: GetComplianceByIdUseCase,
) {
    suspend fun submit(doctorId: String) = submitUseCase(doctorId)
    suspend fun approve(id: String, adminId: String) = approveUseCase(id, adminId)
    suspend fun reject(id: String, adminId: String, reason: String) = rejectUseCase(id, adminId, reason)
    suspend fun getMyStatus(doctorId: String) = getMyStatusUseCase(doctorId)
    suspend fun getAll(page: Int, size: Int) = getAllUseCase(page, size)
    suspend fun getById(id: String) = getByIdUseCase(id)
}