package ke.co.smartroundclinic.patient.domain.repository

import ke.co.smartroundclinic.patient.data.entity.PersonalInformationEntity
import ke.co.smartroundclinic.common.Resource

interface PersonalInformationRepository {
    suspend fun create(entity: PersonalInformationEntity): Resource<PersonalInformationEntity?>
    suspend fun getByPatientId(patientId: String): Resource<PersonalInformationEntity?>
    suspend fun update(
        patientId: String,
        phoneNumber: String?,
        countryCode: String?,
        bloodGroup: String?,
        dateOfBirth: String?,
        weight: Double?,
        weightIn: String?,
        height: Double?,
        heightIn: String?,
        maritalStatus: String?,
    ): Resource<PersonalInformationEntity?>
}
