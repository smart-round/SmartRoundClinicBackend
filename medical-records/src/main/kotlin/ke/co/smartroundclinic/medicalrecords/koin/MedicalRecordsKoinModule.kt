package ke.co.smartroundclinic.medicalrecords.koin

import ke.co.smartroundclinic.common.NotificationSender
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.repository.ConsultationSessionRepository
import ke.co.smartroundclinic.medicalrecords.data.repository.MedicalRecordRepositoryImpl
import ke.co.smartroundclinic.medicalrecords.domain.repository.MedicalRecordRepository
import ke.co.smartroundclinic.medicalrecords.domain.service.MedicalRecordService
import ke.co.smartroundclinic.medicalrecords.domain.usecase.GetMedicalRecordByAppointmentUseCase
import ke.co.smartroundclinic.medicalrecords.domain.usecase.GetPatientMedicalHistoryUseCase
import ke.co.smartroundclinic.medicalrecords.domain.usecase.SaveMedicalRecordUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val medicalRecordsKoinModule = module {
    single<MedicalRecordRepository> { MedicalRecordRepositoryImpl(get(named("medicalRecordsDb"))) }
    single { SaveMedicalRecordUseCase(get(), get<ConsultationMessageRepository>(), get<ConsultationSessionRepository>(), get<NotificationSender>()) }
    single { GetMedicalRecordByAppointmentUseCase(get()) }
    single { GetPatientMedicalHistoryUseCase(get()) }
    single { MedicalRecordService(get(), get(), get()) }
}
