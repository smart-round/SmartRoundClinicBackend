package ke.co.smartroundclinic.common

interface DoctorSpecialitiesResolver {
    suspend fun getDoctorSpecialityNames(doctorId: String): List<String>
}
