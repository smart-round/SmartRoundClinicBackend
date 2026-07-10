package ke.co.smartroundclinic.consultation.domain.usecase.chat

import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.consultation.data.entity.ConsultationThreadReadStateEntity
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationThreadReadStateRepository
import kotlin.time.Clock

class MarkThreadReadUseCase(
    private val repository: ConsultationThreadReadStateRepository,
) {
    suspend operator fun invoke(doctorId: String, patientId: String, readerId: String): Resource<ConsultationThreadReadStateEntity?> =
        repository.markRead(doctorId, patientId, readerId, Clock.System.now().toString())
}
