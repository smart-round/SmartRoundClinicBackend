package ke.co.smartroundclinic.common

data class ReferralDisplayInfo(
    val referringDoctorName: String?,
    val referringDoctorPicture: String?,
    val status: String,
)

/**
 * Lets `:scheduling` show referral display info on an appointment (referring doctor's name/photo,
 * status) without a hard Gradle dependency on `:referral` — appointment.referralId already points
 * at the referral document, this just resolves the denormalized display fields on it.
 */
interface ReferralDisplayResolver {
    suspend fun getReferralDisplayInfo(referralId: String): ReferralDisplayInfo?
}
