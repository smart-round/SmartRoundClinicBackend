package ke.co.smartroundclinic.doctor.domain.usecase.compliance

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.ComplianceCorrectionEntity
import ke.co.smartroundclinic.doctor.domain.model.toModel
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceCorrectionRepository
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.presentation.dto.response.ComplianceCorrectionRes
import ke.co.smartroundclinic.doctor.presentation.dto.response.toRes

/**
 * A rejected doctor confirms they've made the requested changes. Creates a new PENDING
 * correction record (not an update to any previous one) so a doctor rejected multiple times
 * over their onboarding lifetime accumulates a full history — resolved later when an admin
 * approves/rejects the doctor again via the existing compliance endpoints.
 */
class ConfirmComplianceCorrectionUseCase(
    private val complianceRepository: ComplianceRepository,
    private val correctionRepository: ComplianceCorrectionRepository,
) {
    suspend operator fun invoke(doctorId: String): DefaultResponse<ComplianceCorrectionRes?> {
        val complianceResult = complianceRepository.getByDoctorId(doctorId)
        if (complianceResult is Resource.Error) return complianceResult.toDefaultResponse(failedStatusCode = 500) { null }

        val compliance = complianceResult.data
            ?: return Resource.Error<Nothing>("No compliance record found").toDefaultResponse(failedStatusCode = 404) { null }

        val isRejected = !compliance.isApproved && compliance.approvedBy != null
        if (!isRejected) {
            return Resource.Error<Nothing>("Only a rejected application can be confirmed as corrected")
                .toDefaultResponse(failedStatusCode = 422) { null }
        }

        if (correctionRepository.hasPending(doctorId)) {
            return Resource.Error<Nothing>("You already have a correction submission awaiting review")
                .toDefaultResponse(failedStatusCode = 409) { null }
        }

        val entity = ComplianceCorrectionEntity(
            doctorId = doctorId,
            complianceId = compliance.id,
            rejectionReason = compliance.failedApprovalReason ?: "",
        )
        return correctionRepository.create(entity).toDefaultResponse(
            successStatusCode = 201,
            successMessage = "Corrections submitted for review",
        ) { it?.toModel()?.toRes() }
    }
}
