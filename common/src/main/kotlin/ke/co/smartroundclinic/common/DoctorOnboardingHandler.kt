package ke.co.smartroundclinic.common

interface DoctorOnboardingHandler {
    suspend fun onboard(
        fullName: String,
        email: String,
        password: String,
        gender: String,
        kraPin: String,
        phoneNumber: String?,
        dateOfBirth: String?,
        profilePictureBytes: ByteArray?,
        profilePictureContentType: String?,
        kmpdcRegNumber: String?,
        title: String?,
        bio: String?,
        yearsOfExperience: Int?,
        languages: List<String>,
        facilityName: String?,
        specializationId: String,
        subSpecializationId: String?,
        licenceName: String,
        licenceBytes: ByteArray,
        licenceContentType: String,
        bankName: String,
        branchName: String,
        bankCode: String,
        branchCode: String,
        accountNumber: String,
        accountName: String,
    ): Resource<Nothing?>
}
