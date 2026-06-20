package ke.co.smartroundclinic.medicalrecords

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.medicalrecords.domain.service.MedicalRecordService
import ke.co.smartroundclinic.medicalrecords.presentation.controller.medicalRecordController
import org.koin.ktor.ext.inject

fun Application.medicalRecordsModule() {
    val service: MedicalRecordService by inject()
    val messageRepository: ConsultationMessageRepository by inject()

    routing {
        medicalRecordController(service) { userId ->
            messageRepository.getUserName(userId)
        }
    }
}
