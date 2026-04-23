package ke.co.smartroundclinic.common

interface PatientProfileResolver {
    suspend fun getProfilesByPatientIds(patientIds: List<String>): Map<String, PatientProfile>
}
