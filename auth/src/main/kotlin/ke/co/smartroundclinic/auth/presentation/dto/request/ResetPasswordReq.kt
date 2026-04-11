package ke.co.smartroundclinic.auth.presentation.dto.request

import kotlinx.serialization.Serializable

data class ResetPasswordReq(
    val email: String,
    val otpCode: String,
    val newPassword: String,
)