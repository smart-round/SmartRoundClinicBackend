package ke.co.smartroundclinic.medicalrecords.koin

import ke.co.smartroundclinic.consultation.domain.repository.ConsultationMessageRepository
import ke.co.smartroundclinic.consultation.domain.usecase.chat.NotifyOfflineConsultationParticipantUseCase
import ke.co.smartroundclinic.medicalrecords.data.lookup.DoctorNameLookup
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
    single { DoctorNameLookup(get(named("authDb"))) }
    single { SaveMedicalRecordUseCase(get(), get<ConsultationMessageRepository>(), getOrNull<NotifyOfflineConsultationParticipantUseCase>()) }
    single { GetMedicalRecordByAppointmentUseCase(get()) }
    single { GetPatientMedicalHistoryUseCase(get()) }
    single { MedicalRecordService(get(), get(), get(), get()) }
}
