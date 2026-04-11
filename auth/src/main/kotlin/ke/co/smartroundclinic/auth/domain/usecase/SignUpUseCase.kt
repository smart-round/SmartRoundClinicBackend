package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.model.User
import ke.co.smartroundclinic.auth.domain.repository.CredentialsHasher
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.OtpCodeGenerator
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.notification.config.EmailConfig
import ke.co.smartroundclinic.notification.domain.model.EmailWithTemplate
import ke.co.smartroundclinic.notification.domain.model.Template
import ke.co.smartroundclinic.notification.domain.repository.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class SignUpUseCase(
    private val userRepository: UserRepository,
    private val credentialsHasher: CredentialsHasher,
    private val sendAccountVerificationOtpUseCase: SendAccountVerificationOtpUseCase
) {
    suspend operator fun invoke(user: User): DefaultResponse<Nothing?> = withContext(Dispatchers.IO) {
        supervisorScope {
            val otpCode = OtpCodeGenerator().generateOtpCode()
            val hashedOtpCode = credentialsHasher.hash(otpCode)
            val otpExpiresAt = Clock.System.now().plus(2.minutes).toEpochMilliseconds() // always UTC

            val createUser = userRepository.create(user.toEntity().copy(otpCode = hashedOtpCode, otpExpiresAt = otpExpiresAt))
            if (createUser is Resource.Success) launch {
                sendAccountVerificationOtpUseCase(fullName = user.fullName, toEmail = user.email, otpCode = otpCode)
            }

            return@supervisorScope createUser.toDefaultResponse(
                successStatusCode = HttpStatusCode.Created.value,
                failedStatusCode = HttpStatusCode.BadRequest.value,
                successMessage = "User Created Successfully, Kindly verify you email.",
            ) { null }
        }
    }


}
