package ke.co.smartroundclinic.consultation.domain.service

import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.consultation.domain.usecase.session.EndConsultationUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.session.GetConsultationUseCase
import ke.co.smartroundclinic.consultation.domain.usecase.session.StartConsultationUseCase

class ConsultationSessionService(
    private val startUseCase: StartConsultationUseCase,
    private val getUseCase: GetConsultationUseCase,
    private val endUseCase: EndConsultationUseCase,
    val repository: ConsultationSessionRepository,
) {
    suspend fun start(appointmentId: String, userId: String) = startUseCase(appointmentId, userId)
    suspend fun getById(id: String) = getUseCase.byId(id)
    suspend fun getByAppointment(appointmentId: String) = getUseCase.byAppointment(appointmentId)
    suspend fun end(id: String, doctorId: String) = endUseCase(id, doctorId)
}
