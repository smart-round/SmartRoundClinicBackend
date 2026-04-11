package ke.co.smartroundclinic.auth.domain.usecase

import ke.co.smartroundclinic.auth.domain.repository.CredentialsHasher
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.Resource
import kotlin.time.Clock

class VerifyAccountOtpUseCase(
    private val userRepository: UserRepository,
    private val credentialsHasher: CredentialsHasher
){
    suspend operator fun invoke(email:String, otpCode:String): Resource<Boolean> {
        val result = userRepository.getUserByEmail(email)
        val user = result.data ?: return Resource.Error(message = "Otp verification failed", data = false)

        if ( user.otpExpiresAt == null || Clock.System.now().toEpochMilliseconds() > user.otpExpiresAt) {
            return Resource.Error(message = "OTP has expired", data = false)
        }

        val otpVerification = credentialsHasher.verify(
            text = otpCode,
            hashed = user.otpCode ?: return Resource.Error(message = "Otp verification failed", data = false)
        )

        if (!otpVerification) return Resource.Error(message = "Otp verification failed", data = false)
        return Resource.Success(message = "Otp verification successful", data = true)
    }
}