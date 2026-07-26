package ke.co.smartroundclinic.doctor.domain.usecase.signup

import io.ktor.http.HttpStatusCode
import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.repository.CredentialsHasher
import ke.co.smartroundclinic.auth.domain.repository.UserRepository
import ke.co.smartroundclinic.auth.domain.usecase.NotifyNewDoctorSignUpUseCase
import ke.co.smartroundclinic.auth.domain.usecase.SendAccountVerificationOtpUseCase
import ke.co.smartroundclinic.common.DoctorOnboardingHandler
import ke.co.smartroundclinic.common.OtpCodeGenerator
import ke.co.smartroundclinic.common.Resource
import ke.co.smartroundclinic.doctor.data.entity.PractitionerLicenceEntity
import ke.co.smartroundclinic.doctor.data.entity.PractitionerPaymentDetailsEntity
import ke.co.smartroundclinic.doctor.data.entity.PractitionerProfileEntity
import ke.co.smartroundclinic.doctor.data.entity.SpecializationEntity
import ke.co.smartroundclinic.doctor.domain.repository.ComplianceRepository
import ke.co.smartroundclinic.doctor.domain.repository.PaymentDetailsRepository
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerLicenceRepository
import ke.co.smartroundclinic.doctor.domain.repository.PractitionerProfileRepository
import ke.co.smartroundclinic.doctor.domain.repository.SpecializationRepository
import ke.co.smartroundclinic.infra.AppConfig
import ke.co.smartroundclinic.infra.plugins.UnsupportedFileFormatException
import ke.co.smartroundclinic.infra.storage.StorageRepository
import ke.co.smartroundclinic.infra.storage.imageExtensionOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class DoctorSignUpUseCase(
    private val userRepository: UserRepository,
    private val credentialsHasher: CredentialsHasher,
    private val sendAccountVerificationOtpUseCase: SendAccountVerificationOtpUseCase,
    private val profileRepository: PractitionerProfileRepository,
    private val licenceRepository: PractitionerLicenceRepository,
    private val specializationRepository: SpecializationRepository,
    private val complianceRepository: ComplianceRepository,
    private val paymentDetailsRepository: PaymentDetailsRepository,
    private val storageRepository: StorageRepository,
    private val notifyNewDoctorSignUpUseCase: NotifyNewDoctorSignUpUseCase? = null,
) : DoctorOnboardingHandler {

    override suspend fun onboard(
        fullName: String,
        email: String,
        password: String,
        gender: String,
        kraPin: String?,
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
    ): Resource<Nothing?> = withContext(Dispatchers.IO) {
        supervisorScope {
            val licenceExtension = imageExtensionOrNull(licenceContentType)
                ?: throw UnsupportedFileFormatException(licenceContentType)

            val parsedGender = runCatching { UserEntity.Gender.valueOf(gender.uppercase()) }
                .getOrElse { UserEntity.Gender.NON_BINARY }

            val otpCode = OtpCodeGenerator().generateOtpCode()
            val hashedOtpCode = credentialsHasher.hash(otpCode)
            val otpExpiresAt = Clock.System.now().plus(15.minutes).toEpochMilliseconds()
            val userId = ObjectId().toString()
            val now = Clock.System.now().toString()

            val userEntity = UserEntity(
                id = userId,
                fullName = fullName,
                email = email,
                password = password,
                gender = parsedGender,
                role = UserEntity.Role.DOCTOR,
                kraPin = kraPin,
                accountStatus = UserEntity.AccountStatus.INACTIVE,
                verificationStatus = UserEntity.VerificationStatus.UNVERIFIED,
                phoneNumber = phoneNumber,
                dateOfBirth = dateOfBirth,
                otpCode = hashedOtpCode,
                otpExpiresAt = otpExpiresAt,
            )

            val createUser = userRepository.create(userEntity)
            if (createUser is Resource.Error) return@supervisorScope Resource.Error(createUser.message ?: "Failed to create account")

            // Profile picture (optional — failure is non-fatal)
            if (profilePictureBytes != null && !profilePictureContentType.isNullOrBlank()) {
                val ext = imageExtensionOrNull(profilePictureContentType) ?: "jpeg"
                val key = "profile-pictures/$userId.$ext"
                val upload = storageRepository.upload(AppConfig.r2.bucket, key, profilePictureBytes, profilePictureContentType)
                if (upload is Resource.Success) userRepository.updateProfilePicture(userId, key)
            }

            // Practitioner profile
            profileRepository.create(
                PractitionerProfileEntity(
                    doctorId = userId,
                    kmpdcRegNumber = kmpdcRegNumber,
                    title = title,
                    bio = bio,
                    yearsOfExperience = yearsOfExperience,
                    languages = languages,
                    facilityName = facilityName,
                    createdAt = now,
                )
            )

            // Specialization
            specializationRepository.add(
                SpecializationEntity(
                    doctorId = userId,
                    specializationId = specializationId,
                    subSpecializationId = subSpecializationId,
                    createdAt = now,
                )
            )

            // Licence — create record then upload file
            val licenceId = ObjectId().toString()
            val licenceKey = "practitioner-licence/$licenceId.$licenceExtension"
            licenceRepository.add(
                PractitionerLicenceEntity(
                    id = licenceId,
                    doctorId = userId,
                    licenceName = licenceName,
                    licenceUrl = licenceKey,
                    createdAt = now,
                )
            )
            storageRepository.upload(AppConfig.r2.bucket, licenceKey, licenceBytes, licenceContentType)

            // Compliance
            complianceRepository.submit(userId)

            // Bank / payment details
            paymentDetailsRepository.addPaymentDetails(
                PractitionerPaymentDetailsEntity(
                    doctorId = userId,
                    bankName = bankName,
                    branchName = branchName,
                    bankCode = bankCode,
                    branchCode = branchCode,
                    accountNumber = accountNumber,
                    accountName = accountName,
                    createdAt = now,
                )
            )

            // OTP email (async)
            launch {
                sendAccountVerificationOtpUseCase(
                    fullName = fullName,
                    toEmail = email,
                    otpCode = otpCode,
                )
            }

            // Internal doctor signup notification (async)
            launch {
                notifyNewDoctorSignUpUseCase?.invoke(
                    fullName = fullName,
                    email = email,
                    doctorId = userId,
                    createdAt = now,
                )
            }

            Resource.Success(data = null, message = "Account created successfully. Kindly verify your email.")
        }
    }
}
