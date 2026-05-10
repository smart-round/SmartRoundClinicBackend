package ke.co.smartroundclinic.auth.domain.service

import ke.co.smartroundclinic.auth.data.entity.UserEntity
import ke.co.smartroundclinic.auth.domain.usecase.AccountVerificationUseCase
import ke.co.smartroundclinic.auth.domain.usecase.AdminUpdateUserUseCase
import ke.co.smartroundclinic.auth.domain.usecase.FilterUsersByRoleUseCase
import ke.co.smartroundclinic.auth.domain.usecase.GetUsersByRoleUseCase
import ke.co.smartroundclinic.auth.domain.usecase.SignInUseCase
import ke.co.smartroundclinic.auth.domain.usecase.CreateAdminUseCase
import ke.co.smartroundclinic.auth.domain.usecase.GetUserUserUseCase
import ke.co.smartroundclinic.auth.domain.usecase.RefreshTokenUseCase
import ke.co.smartroundclinic.auth.domain.usecase.RemoveProfilePictureUseCase
import ke.co.smartroundclinic.auth.domain.usecase.UploadProfilePictureUseCase
import ke.co.smartroundclinic.auth.domain.usecase.RequestPasswordResetUseCase
import ke.co.smartroundclinic.auth.domain.usecase.ResendAccountVerificationOtpUseCase
import ke.co.smartroundclinic.auth.domain.usecase.ResendPasswordResetOtpUseCase
import ke.co.smartroundclinic.auth.domain.usecase.ResetPasswordUseCase
import ke.co.smartroundclinic.auth.domain.usecase.RevokeTokenUseCase
import ke.co.smartroundclinic.auth.domain.usecase.SignUpUseCase
import ke.co.smartroundclinic.auth.domain.usecase.UpdateUserUseCase
import ke.co.smartroundclinic.auth.presentation.dto.request.AdminUpdateUserReq
import ke.co.smartroundclinic.auth.presentation.dto.request.CreateAdminReq
import ke.co.smartroundclinic.auth.presentation.dto.request.SignUpReq
import ke.co.smartroundclinic.auth.presentation.dto.request.UpdateUserReq

class UserService(
    private val signInUseCase: SignInUseCase,
    private val createAdminUseCase: CreateAdminUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val getUserUserUseCase: GetUserUserUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val accountVerificationUseCase: AccountVerificationUseCase,
    private val resendAccountVerificationOtpUseCase: ResendAccountVerificationOtpUseCase,
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val resendPasswordResetOtpUseCase: ResendPasswordResetOtpUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val revokeTokenUseCase: RevokeTokenUseCase,
    private val uploadProfilePictureUseCase: UploadProfilePictureUseCase,
    private val removeProfilePictureUseCase: RemoveProfilePictureUseCase,
    private val getUsersByRoleUseCase: GetUsersByRoleUseCase,
    private val adminUpdateUserUseCase: AdminUpdateUserUseCase,
    private val filterUsersByRoleUseCase: FilterUsersByRoleUseCase,
) {
    suspend fun signIn(email: String, password: String) = signInUseCase(email, password)
    suspend fun createAdmin(createAdminReq: CreateAdminReq) = createAdminUseCase(user = createAdminReq.toModel())
    suspend fun createSuperAdmin(createAdminReq: CreateAdminReq) = createAdminUseCase(user = createAdminReq.toSuperAdminModel())
    suspend fun signUp(role: String, body: SignUpReq) = signUpUseCase(body.toModel(UserEntity.Role.valueOf(role)))
    suspend fun accountVerification(email: String, otpCode: String) = accountVerificationUseCase(email, otpCode)
    suspend fun resendAccountVerificationOtp(email: String) = resendAccountVerificationOtpUseCase(email)
    suspend fun updateUser(userId:String, body: UpdateUserReq) = updateUserUseCase(
        userId = userId,
        fullName = body.fullName,
        email = body.email,
        phoneNumber = body.phoneNumber,
        gender = body.gender,
        dateOfBirth = body.dateOfBirth
    )
    suspend fun getUser(userId:String) = getUserUserUseCase(userId)
    suspend fun requestPasswordReset(email: String) = requestPasswordResetUseCase(email)
    suspend fun resendPasswordResetOtp(email: String) = resendPasswordResetOtpUseCase(email)
    suspend fun resetPassword(email: String, otpCode: String, newPassword: String) =
        resetPasswordUseCase(email, otpCode, newPassword)
    suspend fun refreshToken(refreshToken: String) = refreshTokenUseCase(refreshToken)
    suspend fun revokeToken(refreshToken: String) = revokeTokenUseCase(refreshToken)
    suspend fun uploadProfilePicture(userId: String, imageBytes: ByteArray, contentType: String) =
        uploadProfilePictureUseCase(userId, imageBytes, contentType)
    suspend fun removeProfilePicture(userId: String) = removeProfilePictureUseCase(userId)
    suspend fun getUsersByRole(role: UserEntity.Role, page: Int, size: Int, search: String?) =
        getUsersByRoleUseCase(role, page, size, search)
    suspend fun adminUpdateUser(userId: String, body: AdminUpdateUserReq) =
        adminUpdateUserUseCase(userId, body.accountStatus, body.verificationStatus)
    suspend fun filterUsers(
        role: UserEntity.Role,
        page: Int,
        size: Int,
        accountStatus: UserEntity.AccountStatus?,
        createdFrom: String?,
        createdTo: String?,
    ) = filterUsersByRoleUseCase(role, page, size, accountStatus, createdFrom, createdTo)
}