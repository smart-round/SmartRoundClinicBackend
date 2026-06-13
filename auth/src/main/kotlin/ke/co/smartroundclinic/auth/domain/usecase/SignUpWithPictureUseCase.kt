package ke.co.smartroundclinic.auth.domain.usecase

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.domain.model.User
import ke.co.smartroundclinic.auth.domain.repository.CredentialsHasher
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.common.DefaultResponse
import ke.co.smartroundclinic.common.OtpCodeGenerator
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class SignUpWithPictureUseCase(
    private val userRepository: UserRepository,
    private val credentialsHasher: CredentialsHasher,
    private val sendAccountVerificationOtpUseCase: SendAccountVerificationOtpUseCase,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(
        user: User,
        imageBytes: ByteArray?,
        contentType: String?,
    ): DefaultResponse<Nothing?> = withContext(Dispatchers.IO) {
        supervisorScope {
            val otpCode = OtpCodeGenerator().generateOtpCode()
            val hashedOtpCode = credentialsHasher.hash(otpCode)
            val otpExpiresAt = Clock.System.now().plus(15.minutes).toEpochMilliseconds()

            val createUser = userRepository.create(
                user.toEntity().copy(otpCode = hashedOtpCode, otpExpiresAt = otpExpiresAt)
            )

            if (createUser is Resource.Success) {
                launch {
                    sendAccountVerificationOtpUseCase(
                        fullName = user.fullName,
                        toEmail = user.email,
                        otpCode = otpCode,
                    )
                }

                // Upload profile picture best-effort — never fail the sign-up if this fails
                val createdUserId = createUser.data?.id
                if (imageBytes != null && !contentType.isNullOrBlank() && createdUserId != null) {
                    launch {
                        val extension = imageExtensionOrNull(contentType) ?: "jpeg"
                        val key = "profile-pictures/$createdUserId.$extension"
                        val uploadResult = storageRepository.upload(
                            bucket = AppConfig.r2.bucket,
                            key = key,
                            content = imageBytes,
                            contentType = contentType,
                        )
                        if (uploadResult is Resource.Success) {
                            val uploadedKey = uploadResult.data
                            if (uploadedKey != null) {
                                userRepository.updateProfilePicture(userId = createdUserId, url = uploadedKey)
                            }
                        }
                    }
                }
            }

            return@supervisorScope createUser.toDefaultResponse(
                successStatusCode = HttpStatusCode.Created.value,
                failedStatusCode = HttpStatusCode.BadRequest.value,
                successMessage = "User Created Successfully, Kindly verify your email.",
            ) { null }
        }
    }
}
