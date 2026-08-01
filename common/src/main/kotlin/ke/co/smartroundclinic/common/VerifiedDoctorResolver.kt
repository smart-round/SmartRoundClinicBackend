package ke.co.smartroundclinic.common

interface VerifiedDoctorResolver {
    /** True when [doctorId] is compliance-approved, monetized, and not suspended. */
    suspend fun isVerified(doctorId: String): Boolean
}
