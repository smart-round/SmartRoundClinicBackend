package ke.co.smartroundclinic.common

interface PatientNameResolver {
    suspend fun getPatientNames(patientIds: List<String>): Map<String, String>
}
