package ke.co.smartroundclinic.common

interface DoctorWalletProvisioner {
    suspend fun provisionWallet(doctorId: String): String?
}
