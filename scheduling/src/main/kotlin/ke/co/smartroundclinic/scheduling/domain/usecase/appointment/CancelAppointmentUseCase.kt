package ke.co.smartroundclinic.scheduling.domain.usecase.appointment

import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.scheduling.domain.repository.AppointmentRepository
import ke.co.smartroundclinic.scheduling.presentation.dto.response.AppointmentRes
import ke.co.smartroundclinic.scheduling.presentation.dto.response.toRes

class CancelAppointmentUseCase(private val repository: AppointmentRepository) {
    suspend operator fun invoke(
        id: String,
        userId: String,
        role: String,
        reason: String? = null,
    ): DefaultResponse<AppointmentRes?> {
        val existing = repository.getById(id)
        if (existing is Resource.Error) return existing.toDefaultResponse(failedStatusCode = 404) { null }
        val entity = existing.data ?: return Resource.Error<Nothing>("Appointment not found")
            .toDefaultResponse(failedStatusCode = 404) { null }

        val authorized = (role == "PATIENT" && entity.patientId == userId) ||
                (role == "DOCTOR" && entity.doctorId == userId)
        if (!authorized) return Resource.Error<Nothing>("Not authorized to cancel this appointment")
            .toDefaultResponse(failedStatusCode = 403) { null }

        return repository.updateStatus(id, "CANCELLED", cancellationReason = reason, cancelledBy = userId)
            .toDefaultResponse { it?.toModel()?.toRes() }
    }
}
